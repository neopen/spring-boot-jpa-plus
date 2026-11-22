package io.github.neopen.core.persistence.page;

import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * 游标分页 H2 集成测试。
 *
 * <p>验证单键、复合键、混合方向的翻页无重复、无遗漏，并与 offset
 * 分页结果保持一致。</p>
 */
class KeysetPaginationIntegrationTest {

    /** 测试数据的基准时间。 */
    private static final LocalDateTime BASE =
            LocalDateTime.of(2026, 1, 1, 0, 0);

    /** 实体管理器工厂。 */
    private static EntityManagerFactory entityManagerFactory;

    /**
     * 创建实体管理器工厂。
     */
    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory(
                "jpa-plus-core-test"
        );
    }

    /**
     * 关闭实体管理器工厂。
     */
    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    /**
     * 重建测试数据。
     *
     * <p>金额取主键模 3，创建时间取主键模 2，制造排序值重复，
     * 用于验证主键 tiebreaker 在翻页边界上的作用。</p>
     */
    @BeforeEach
    void seed() {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.createQuery(
                    "delete from KeysetOrder"
            ).executeUpdate();

            for (long id = 1L; id <= 10L; id++) {
                entityManager.persist(new KeysetOrder(
                        id,
                        (int) (id % 3),
                        BASE.plusMinutes(id % 2)
                ));
            }

            entityManager.getTransaction().commit();
        }
    }

    /**
     * 验证单键升序翻页。
     */
    @Test
    void shouldPageBySingleKeyAscending() {
        List<Long> ids = collect(Sort.by("amount"), 3);

        assertThat(ids)
                .doesNotHaveDuplicates()
                .containsExactly(
                        3L, 6L, 9L,
                        1L, 4L, 7L, 10L,
                        2L, 5L, 8L
                );
    }

    /**
     * 验证单键降序翻页。
     */
    @Test
    void shouldPageBySingleKeyDescending() {
        List<Long> ids = collect(
                Sort.by(Sort.Order.desc("amount")),
                4
        );

        assertThat(ids)
                .doesNotHaveDuplicates()
                .containsExactly(
                        2L, 5L, 8L,
                        1L, 4L, 7L, 10L,
                        3L, 6L, 9L
                );
    }

    /**
     * 验证复合键混合方向翻页，排序值重复时按主键稳定推进。
     */
    @Test
    void shouldPageByCompositeKeysWithMixedDirections() {
        Sort sort = Sort.by(
                Sort.Order.asc("amount"),
                Sort.Order.desc("createdAt")
        );

        List<Long> ids = collect(sort, 2);

        assertThat(ids)
                .doesNotHaveDuplicates()
                .containsExactly(
                        3L, 9L,
                        6L, 1L,
                        7L, 4L,
                        10L, 5L,
                        2L, 8L
                );
    }

    /**
     * 验证游标分页与 offset 分页结果一致。
     */
    @Test
    void shouldMatchOffsetPagination() {
        Sort keysetSort = Sort.by(
                Sort.Order.asc("amount"),
                Sort.Order.desc("createdAt")
        );
        Sort offsetSort = Sort.by(
                Sort.Order.asc("amount"),
                Sort.Order.desc("createdAt"),
                Sort.Order.asc("id")
        );

        assertThat(collect(keysetSort, 3))
                .isEqualTo(offsetIds(offsetSort, 3));
    }

    /**
     * 验证排序已包含主键时不再重复追加 tiebreaker。
     */
    @Test
    void shouldKeepExplicitIdSort() {
        List<Long> ids = collect(
                Sort.by(
                        Sort.Order.asc("amount"),
                        Sort.Order.asc("id")
                ),
                4
        );

        assertThat(ids).doesNotHaveDuplicates().hasSize(10);
    }

    /**
     * 验证查询条件与游标条件合并。
     */
    @Test
    void shouldCombineSpecificationWithCursor() {
        Specification<KeysetOrder> specification =
                (root, query, cb) -> cb.greaterThan(
                        root.<Integer>get("amount"),
                        0
                );

        List<Long> ids = collect(
                specification,
                Sort.by("amount"),
                2
        );

        assertThat(ids)
                .doesNotHaveDuplicates()
                .containsExactly(
                        1L, 4L, 7L, 10L,
                        2L, 5L, 8L
                );
    }

    /**
     * 验证游标值个数与排序项不匹配时被拒绝。
     */
    @Test
    void shouldRejectMismatchedCursorValueCount() {
        KeysetPageParams params = KeysetPageParams.builder()
                .size(2)
                .sort(Sort.by(
                        Sort.Order.asc("amount"),
                        Sort.Order.desc("createdAt")
                ))
                .cursor(KeysetCursor.of(1L))
                .build();

        assertThatThrownBy(() -> keyset(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cursor value count");
    }

    /**
     * 按游标分页收集全部主键。
     *
     * @param sort 排序规则
     * @param size 每页数量
     * @return 主键列表
     */
    private List<Long> collect(Sort sort, int size) {
        return collect(null, sort, size);
    }

    /**
     * 按游标分页收集满足条件的全部主键。
     *
     * @param specification 查询条件，允许为 {@code null}
     * @param sort          排序规则
     * @param size          每页数量
     * @return 主键列表
     */
    private List<Long> collect(
            Specification<KeysetOrder> specification,
            Sort sort,
            int size
    ) {
        List<Long> ids = new ArrayList<>();
        KeysetCursor cursor = null;

        for (int page = 0; page < 20; page++) {
            KeysetPageParams params = KeysetPageParams.builder()
                    .size(size)
                    .sort(sort)
                    .cursor(cursor)
                    .build();

            KeysetPage<KeysetOrder> result = keyset(
                    specification,
                    params
            );
            result.getContent().forEach(order -> ids.add(order.getId()));

            if (!result.hasNext()) {
                assertThat(result.getNextCursor()).isNull();
                return ids;
            }

            cursor = KeysetCursor.decode(
                    result.getNextCursor().encode()
            );
        }

        fail("Keyset pagination did not terminate");
        return ids;
    }

    /**
     * 按 offset 分页收集全部主键。
     *
     * @param sort 排序规则
     * @param size 每页数量
     * @return 主键列表
     */
    private List<Long> offsetIds(Sort sort, int size) {
        List<Long> ids = new ArrayList<>();

        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            JpaPlusSpecExecutor<KeysetOrder> executor =
                    JpaPlusSpecExecutor.of(
                            entityManager,
                            KeysetOrder.class
                    );

            for (int page = 0; ; page++) {
                Page<KeysetOrder> result = executor.findPage(
                        null,
                        PageRequest.of(page, size, sort)
                );
                result.getContent().forEach(
                        order -> ids.add(order.getId())
                );

                if (!result.hasNext()) {
                    return ids;
                }
            }
        }
    }

    /**
     * 执行一次游标分页查询。
     *
     * @param params 分页参数
     * @return 游标分页结果
     */
    private KeysetPage<KeysetOrder> keyset(KeysetPageParams params) {
        return keyset(null, params);
    }

    /**
     * 执行一次带条件的游标分页查询。
     *
     * @param specification 查询条件，允许为 {@code null}
     * @param params        分页参数
     * @return 游标分页结果
     */
    private KeysetPage<KeysetOrder> keyset(
            Specification<KeysetOrder> specification,
            KeysetPageParams params
    ) {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            return JpaPlusSpecExecutor
                    .of(entityManager, KeysetOrder.class)
                    .keyset(specification, params);
        }
    }
}
