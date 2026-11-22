package io.github.neopen.core.persistence.spec;

import io.github.neopen.core.persistence.page.KeysetCursor;
import io.github.neopen.core.persistence.page.KeysetPageParams;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.IdClass;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JpaPlusSpecExecutor#keyset} 参数校验测试。
 */
class KeysetExecutorValidationTest {

    /**
     * 验证分页参数为空时被拒绝。
     */
    @Test
    void shouldRejectNullParams() {
        EntityManager entityManager = mock(EntityManager.class);

        assertThatThrownBy(() -> JpaPlusSpecExecutor
                .of(entityManager, Sample.class)
                .keyset(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    /**
     * 验证复合主键实体被拒绝。
     */
    @Test
    void shouldRejectCompositePrimaryKey() {
        EntityManager entityManager = mock(EntityManager.class);
        KeysetPageParams params = KeysetPageParams.of(10, Sort.by("id"));

        assertThatThrownBy(() -> JpaPlusSpecExecutor
                .of(entityManager, IdClassEntity.class)
                .keyset(null, params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("composite primary keys");

        assertThatThrownBy(() -> JpaPlusSpecExecutor
                .of(entityManager, EmbeddedIdEntity.class)
                .keyset(null, params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("composite primary keys");
    }

    /**
     * 验证不支持的游标值类型被拒绝。
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldRejectUnsupportedCursorType() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Legacy> query = mock(CriteriaQuery.class);
        Root<Legacy> root = mock(Root.class);
        Path atPath = mock(Path.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Legacy.class)).thenReturn(query);
        when(query.from(Legacy.class)).thenReturn(root);
        when(root.get("at")).thenReturn(atPath);
        when(atPath.getJavaType()).thenReturn(Date.class);

        KeysetPageParams params = KeysetPageParams.builder()
                .size(10)
                .sort(Sort.by("at"))
                .cursor(KeysetCursor.of("2026-01-01", "1"))
                .build();

        assertThatThrownBy(() -> JpaPlusSpecExecutor
                .of(entityManager, Legacy.class)
                .keyset(null, params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported cursor value type");
    }

    /** 测试实体。 */
    private static class Sample {

        /** 主键。 */
        private Long id;
    }

    /** 使用 {@code @IdClass} 的复合主键实体。 */
    @IdClass(CompositeId.class)
    private static class IdClassEntity {

        /** 主键一。 */
        private Long id;

        /** 主键二。 */
        private Long tenantId;
    }

    /** 复合主键类。 */
    private static class CompositeId implements Serializable {

        /** 主键一。 */
        private Long id;

        /** 主键二。 */
        private Long tenantId;
    }

    /** 使用 {@code @EmbeddedId} 的复合主键实体。 */
    private static class EmbeddedIdEntity {

        /** 嵌入式主键。 */
        @EmbeddedId
        private Long id;
    }

    /** 使用不支持类型排序的实体。 */
    private static class Legacy {

        /** 主键。 */
        private Long id;

        /** 旧式时间字段。 */
        private Date at;
    }
}
