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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
