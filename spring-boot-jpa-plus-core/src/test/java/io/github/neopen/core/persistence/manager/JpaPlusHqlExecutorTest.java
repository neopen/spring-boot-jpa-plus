package io.github.neopen.core.persistence.manager;

import io.github.neopen.core.persistence.hql.HqlWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JpaPlusHqlExecutor} 分页查询单元测试。
 */
class JpaPlusHqlExecutorTest {

    /** 被测试执行器使用的实体管理器。 */
    private final EntityManager entityManager =
            mock(EntityManager.class);

    /** 被测执行器。 */
    private final JpaPlusHqlExecutor executor =
            new JpaPlusHqlExecutor(entityManager);

    @Test
    void shouldQueryPageWithDerivedCountAndSort() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "select o from Order o where o.status = :status";

        when(entityManager.createQuery(
                hql + " order by o.createTime desc",
                String.class
        )).thenReturn(dataQuery);

        when(entityManager.createQuery(
                "select count(*) from Order o where o.status = :status",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(10)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList())
                .thenReturn(List.of("o-1", "o-2"));
        when(countQuery.getSingleResult()).thenReturn(42L);

        Page<String> page = executor.queryForPage(
                hql,
                Map.of("status", "PAID"),
                String.class,
                PageRequest.of(
                        1,
                        10,
                        Sort.by(Sort.Direction.DESC, "createTime")
                )
        );

        assertThat(page.getContent())
                .containsExactly("o-1", "o-2");
        assertThat(page.getTotalElements()).isEqualTo(42);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);

        verify(dataQuery).setParameter("status", "PAID");
        verify(countQuery).setParameter("status", "PAID");
    }

    @Test
    void shouldUseExplicitCountHql() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "select o.status from Order o group by o.status";
        String countHql =
                "select count(distinct o.status) from Order o";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(entityManager.createQuery(countHql, Long.class))
                .thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of("PAID"));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<String> page = executor.queryForPage(
                hql,
                countHql,
                null,
                String.class,
                PageRequest.of(0, 5)
        );

        assertThat(page.getContent()).containsExactly("PAID");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldStripOrderByFromDerivedCountQuery() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "select o from Order o order by o.id desc";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(entityManager.createQuery(
                "select count(*) from Order o",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<String> page = executor.queryForPage(
                hql,
                null,
                String.class,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldApplySortWithPropertyNameWhenRootHasNoAlias() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "from Order where status = :status";

        when(entityManager.createQuery(
                hql + " order by createTime desc",
                String.class
        )).thenReturn(dataQuery);
        when(entityManager.createQuery(
                "select count(*) from Order where status = :status",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        executor.queryForPage(
                hql,
                Map.of("status", "PAID"),
                String.class,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "createTime")
                )
        );
    }

    @Test
    void shouldRejectGroupByWithoutCountHql() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select o.status from Order o group by o.status",
                null,
                String.class,
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("countHql");
    }

    @Test
    void shouldRejectSortWhenHqlAlreadyContainsOrderBy() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select o from Order o order by o.id desc",
                null,
                String.class,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order by");
    }

    @Test
    void shouldRejectUnpagedRequest() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select o from Order o",
                null,
                String.class,
                Pageable.unpaged()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paged");
    }

    @Test
    void shouldQueryPageUsingWrapperAndPageableSort() {
        TypedQuery<SampleEntity> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();
        SampleEntity entity = new SampleEntity();

        HqlWrapper<SampleEntity> wrapper =
                HqlWrapper.create(SampleEntity.class)
                        .eq("status", "PAID");

        when(entityManager.createQuery(
                "select o from SampleEntity o"
                        + " where o.status = :p1"
                        + " order by o.id desc",
                SampleEntity.class
        )).thenReturn(dataQuery);

        when(entityManager.createQuery(
                "select count(*) from SampleEntity o"
                        + " where o.status = :p1",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList())
                .thenReturn(List.of(entity));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<SampleEntity> page = executor.queryForPage(
                wrapper,
                PageRequest.of(
                        0,
                        5,
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );

        assertThat(page.getContent()).containsExactly(entity);
        assertThat(page.getTotalElements()).isEqualTo(1);

        verify(dataQuery).setParameter("p1", "PAID");
        verify(countQuery).setParameter("p1", "PAID");
    }

    @Test
    void shouldUseWrapperOrderWhenPageableUnsorted() {
        TypedQuery<SampleEntity> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        HqlWrapper<SampleEntity> wrapper =
                HqlWrapper.create(SampleEntity.class)
                        .eq("status", "PAID")
                        .orderByDesc("createTime");

        when(entityManager.createQuery(
                "select o from SampleEntity o"
                        + " where o.status = :p1"
                        + " order by o.createTime desc",
                SampleEntity.class
        )).thenReturn(dataQuery);

        when(entityManager.createQuery(
                "select count(*) from SampleEntity o"
                        + " where o.status = :p1",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<SampleEntity> page = executor.queryForPage(
                wrapper,
                PageRequest.of(0, 5)
        );

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldQueryListUsingWrapper() {
        TypedQuery<SampleEntity> dataQuery = mockQuery();

        HqlWrapper<SampleEntity> wrapper =
                HqlWrapper.create(SampleEntity.class)
                        .in("id", List.of(1L, 2L))
                        .orderByAsc("id");

        when(entityManager.createQuery(
                "select o from SampleEntity o"
                        + " where o.id in (:p1)"
                        + " order by o.id asc",
                SampleEntity.class
        )).thenReturn(dataQuery);

        when(dataQuery.getResultList()).thenReturn(List.of());

        assertThat(executor.queryForList(wrapper)).isEmpty();

        verify(dataQuery).setParameter("p1", List.of(1L, 2L));
    }

    /**
     * 创建类型化查询的 Mock 对象。
     *
     * @param <T> 查询结果类型
     * @return 查询 Mock 对象
     */
    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> mockQuery() {
        return mock(TypedQuery.class);
    }

    /** 测试实体占位。 */
    private static final class SampleEntity {
    }
}
