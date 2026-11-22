package io.github.neopen.core.persistence.manager;

import io.github.neopen.core.persistence.sql.SqlWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 原生 SQL 执行器单元测试。
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class JpaPlusSqlExecutorTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query dataQuery;

    @Mock
    private Query countQuery;

    private JpaPlusSqlExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new JpaPlusSqlExecutor(entityManager);
    }

    @Test
    void shouldPageWithDerivedCountAndSort() {
        String sql = "select * from t_order where status = :status";
        String dataSql = sql + " order by id desc";
        String countSql = "select count(*) from ("
                + sql + ") jpa_plus_count";

        when(entityManager.createNativeQuery(dataSql)).thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(1)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of("row"));
        when(countQuery.getSingleResult()).thenReturn(3L);

        Page<Object> page = executor.queryForPage(
                sql,
                Map.of("status", "PAID"),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).containsExactly("row");
        verify(dataQuery).setParameter("status", "PAID");
        verify(countQuery).setParameter("status", "PAID");
    }

    @Test
    void shouldStreamNativeQueryResults() {
        String sql = "select * from t_order where status = :status";

        when(entityManager.createNativeQuery(sql)).thenReturn(dataQuery);
        when(dataQuery.setParameter("status", "PAID"))
                .thenReturn(dataQuery);
        when(dataQuery.getResultStream())
                .thenReturn(Stream.of("row-1", "row-2"));

        List<?> result = executor
                .stream(sql, Map.of("status", "PAID"))
                .toList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo("row-1");
        assertThat(result.get(1)).isEqualTo("row-2");
        verify(dataQuery).setParameter("status", "PAID");
    }

    @Test
    void shouldUseExplicitCountSql() {
        String sql = "select * from t_order";
        String countSql = "select count(*) from t_order where status = 'PAID'";

        when(entityManager.createNativeQuery(sql)).thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(7L);

        Page<Object> page = executor.queryForPage(
                sql,
                countSql,
                null,
                PageRequest.of(0, 5)
        );

        assertThat(page.getTotalElements()).isEqualTo(7);
    }

    @Test
    void shouldStripOrderByWhenDerivingCount() {
        String sql = "select * from t_order order by id desc";
        String countSql = "select count(*) from ("
                + "select * from t_order) jpa_plus_count";

        when(entityManager.createNativeQuery(sql)).thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<Object> page = executor.queryForPage(
                sql,
                null,
                PageRequest.of(0, 5)
        );

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldPageEntitiesWithDerivedCount() {
        String sql = "select * from t_order where status = :status";
        String dataSql = sql + " order by id desc";
        String countSql = "select count(*) from ("
                + sql + ") jpa_plus_count";

        when(entityManager.createNativeQuery(dataSql, SampleEntity.class))
                .thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(new SampleEntity(1L)));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<SampleEntity> page = executor.queryForPage(
                sql,
                Map.of("status", "PAID"),
                SampleEntity.class,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldPageWithWrapperAndPageableSort() {
        SqlWrapper wrapper = SqlWrapper.create("t_order")
                .eq("status", "PAID");
        String baseSql = "select o.* from t_order o where o.status = :p1";
        String dataSql = baseSql + " order by o.id desc";
        String countSql = "select count(*) from ("
                + baseSql + ") jpa_plus_count";

        when(entityManager.createNativeQuery(dataSql, SampleEntity.class))
                .thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(new SampleEntity(1L)));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<SampleEntity> page = executor.queryForPage(
                wrapper,
                SampleEntity.class,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(dataQuery).setParameter("p1", "PAID");
        verify(countQuery).setParameter("p1", "PAID");
    }

    @Test
    void shouldUseWrapperOrderWhenPageableIsUnsorted() {
        SqlWrapper wrapper = SqlWrapper.create("t_order")
                .eq("status", "PAID")
                .orderByAsc("id");
        String baseSql = "select o.* from t_order o where o.status = :p1";
        String dataSql = baseSql + " order by o.id asc";
        String countSql = "select count(*) from ("
                + baseSql + ") jpa_plus_count";

        when(entityManager.createNativeQuery(dataSql)).thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<Object> page = executor.queryForPage(
                wrapper,
                PageRequest.of(0, 5)
        );

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void shouldQueryWrapperListAndCount() {
        SqlWrapper wrapper = SqlWrapper.create("t_order")
                .eq("status", "PAID")
                .orderByDesc("id");
        String baseSql = "select o.* from t_order o where o.status = :p1";
        String dataSql = baseSql + " order by o.id desc";
        String countSql = "select count(*) from ("
                + baseSql + ") jpa_plus_count";

        when(entityManager.createNativeQuery(dataSql, SampleEntity.class))
                .thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(new SampleEntity(1L)));
        when(countQuery.getSingleResult()).thenReturn(1L);

        List<SampleEntity> entities = executor.queryForList(
                wrapper,
                SampleEntity.class
        );
        long total = executor.queryForCount(wrapper);

        assertThat(entities).hasSize(1);
        assertThat(total).isEqualTo(1);
    }

    @Test
    void shouldRejectUnpagedRequestAndExistingOrderBy() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select * from t_order",
                null,
                org.springframework.data.domain.Pageable.unpaged()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unpaged");

        assertThatThrownBy(() -> executor.queryForPage(
                "select * from t_order order by id desc",
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"))
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already contains order by");
    }

    /**
     * 验证 union 分页排序按输出列名追加，不带表别名。
     */
    @Test
    void shouldPageUnionWithoutAliasInSort() {
        SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
                .select("o.id")
                .eq("status", "PAID")
                .union(
                        SqlWrapper.create("t_order_archive", "a")
                                .select("a.id")
                                .eq("status", "CLOSED")
                );
        String baseSql = "select o.id from t_order o where o.status = :p1"
                + " union select a.id from t_order_archive a"
                + " where a.status = :p2";
        String dataSql = baseSql + " order by id desc";
        String countSql = "select count(*) from ("
                + baseSql + ") jpa_plus_count";

        when(entityManager.createNativeQuery(dataSql, SampleEntity.class))
                .thenReturn(dataQuery);
        when(entityManager.createNativeQuery(countSql)).thenReturn(countQuery);
        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList())
                .thenReturn(List.of(new SampleEntity(1L)));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<SampleEntity> page = executor.queryForPage(
                wrapper,
                SampleEntity.class,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        verify(dataQuery).setParameter("p1", "PAID");
        verify(dataQuery).setParameter("p2", "CLOSED");
        verify(countQuery).setParameter("p1", "PAID");
        verify(countQuery).setParameter("p2", "CLOSED");
    }

    @Test
    void shouldQueryTypedObject() {
        String sql = "select * from t_order where id = :id";

        when(entityManager.createNativeQuery(sql, SampleEntity.class))
                .thenReturn(dataQuery);
        when(dataQuery.setMaxResults(1)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of(new SampleEntity(1L)));

        SampleEntity entity = executor.queryForObject(
                sql,
                Map.of("id", 1L),
                SampleEntity.class
        );

        assertThat(entity).isNotNull();
        assertThat(entity.id).isEqualTo(1L);
    }

    @Test
    void shouldRejectBlankSql() {
        assertThatThrownBy(() -> executor.queryForPage(
                " ",
                Map.of(),
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SQL");
    }

    @Test
    void shouldConsumeNativeResultsInBatches() {
        String sql = "select order_no from t_order";

        when(entityManager.createNativeQuery(sql)).thenReturn(dataQuery);
        when(dataQuery.getResultStream()).thenReturn(
                Stream.of("o-1", "o-2", "o-3")
        );

        List<List<?>> batches = new ArrayList<>();

        executor.forEachBatch(sql, null, 2, batches::add);

        assertThat(batches)
                .containsExactly(
                        List.of("o-1", "o-2"),
                        List.of("o-3")
                );
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
    }

    @Test
    void shouldConsumeTypedEntitiesInBatches() {
        String sql = "select * from t_order where status = :status";
        SampleEntity first = new SampleEntity(1L);
        SampleEntity second = new SampleEntity(2L);

        when(entityManager.createNativeQuery(sql, SampleEntity.class))
                .thenReturn(dataQuery);
        when(dataQuery.setParameter("status", "PAID"))
                .thenReturn(dataQuery);
        when(dataQuery.getResultStream())
                .thenReturn(Stream.of(first, second));

        List<List<SampleEntity>> batches = new ArrayList<>();

        executor.forEachBatch(
                sql,
                Map.of("status", "PAID"),
                SampleEntity.class,
                5,
                batches::add
        );

        assertThat(batches).containsExactly(List.of(first, second));
        verify(dataQuery).setParameter("status", "PAID");
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    @Test
    void shouldCloseStreamWhenConsumerFails() {
        String sql = "select order_no from t_order";
        AtomicBoolean closed = new AtomicBoolean();

        when(entityManager.createNativeQuery(sql)).thenReturn(dataQuery);
        when(dataQuery.getResultStream()).thenReturn(
                Stream.of("o-1").onClose(() -> closed.set(true))
        );

        assertThatThrownBy(() -> executor.forEachBatch(
                sql,
                null,
                1,
                batch -> {
                    throw new IllegalStateException("boom");
                }
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(closed).isTrue();
        verify(entityManager, never()).clear();
    }

    @Test
    void shouldRejectInvalidBatchArguments() {
        assertThatThrownBy(() -> executor.forEachBatch(
                "select * from t_order",
                null,
                -1,
                batch -> {
                }
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size");

        assertThatThrownBy(() -> executor.forEachBatch(
                "select * from t_order",
                null,
                10,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consumer");
    }

    /**
     * 测试实体。
     */
    private static final class SampleEntity {

        /** 主键。 */
        private final Long id;

        /**
         * 创建测试实体。
         *
         * @param id 主键
         */
        private SampleEntity(Long id) {
            this.id = id;
        }
    }
}
