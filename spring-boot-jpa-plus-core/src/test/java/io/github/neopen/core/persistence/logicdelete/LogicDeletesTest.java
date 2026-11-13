package io.github.neopen.core.persistence.logicdelete;

import io.github.neopen.core.persistence.spec.SpecWrapper;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LogicDeletes} 单元测试。
 */
class LogicDeletesTest {

    /**
     * 验证标注了逻辑删除字段的实体被识别。
     */
    @Test
    void shouldDetectLogicDeleteField() {
        assertThat(LogicDeletes.isLogicDeleted(Order.class)).isTrue();
        assertThat(LogicDeletes.isLogicDeleted(Plain.class)).isFalse();
    }

    /**
     * 验证父类上的逻辑删除字段可以被识别。
     */
    @Test
    void shouldDetectInheritedLogicDeleteField() {
        assertThat(LogicDeletes.isLogicDeleted(Child.class)).isTrue();
    }

    /**
     * 验证非布尔类型的逻辑删除字段被拒绝。
     */
    @Test
    void shouldRejectNonBooleanLogicDeleteField() {
        assertThatThrownBy(
                () -> LogicDeletes.isLogicDeleted(Invalid.class)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@LogicDelete field must be Boolean");
    }

    /**
     * 验证逻辑删除条件生成 {@code deleted = false}。
     */
    @Test
    void shouldBuildDeletedFalsePredicate() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> deletedPath = mock(Path.class);
        Predicate expected = mock(Predicate.class);

        when(root.get("deleted")).thenReturn(deletedPath);
        when(cb.equal(deletedPath, false)).thenReturn(expected);

        Predicate predicate = LogicDeletes.filter(Order.class)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(expected);
        verify(cb).equal(deletedPath, false);
    }

    /**
     * 验证逻辑删除条件与业务条件按 AND 合并。
     */
    @Test
    void shouldCombineWithBusinessSpecification() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> deletedPath = mock(Path.class);
        Predicate logicPredicate = mock(Predicate.class);
        Predicate businessPredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("deleted")).thenReturn(deletedPath);
        when(cb.equal(deletedPath, false)).thenReturn(logicPredicate);
        when(cb.and(logicPredicate, businessPredicate))
                .thenReturn(combined);

        Specification<Order> business =
                (r, q, c) -> businessPredicate;

        Predicate predicate = LogicDeletes
                .filter(Order.class, business)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(combined);
        verify(cb).and(logicPredicate, businessPredicate);
    }

    /**
     * 验证未标注逻辑删除时原样返回业务条件。
     */
    @Test
    void shouldReturnOriginalSpecificationWhenNotAnnotated() {
        Specification<Plain> business = (r, q, c) -> mock(Predicate.class);

        assertThat(LogicDeletes.filter(Plain.class, business))
                .isSameAs(business);
    }

    /**
     * 验证 SpecWrapper 可以链式追加逻辑删除过滤。
     */
    @Test
    void shouldAppendLogicDeleteFilterThroughWrapper() {
        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> deletedPath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);
        Predicate logicPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("deleted")).thenReturn(deletedPath);
        when(root.get("status")).thenReturn(statusPath);
        when(cb.equal(deletedPath, false)).thenReturn(logicPredicate);
        when(cb.equal(statusPath, "PAID")).thenReturn(statusPredicate);
        when(cb.and(logicPredicate, statusPredicate))
                .thenReturn(combined);

        Predicate predicate = SpecWrapper.create(Order.class)
                .filterDeleted()
                .eq("status", "PAID")
                .toSpecification()
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(combined);
    }

    /** 带逻辑删除字段的实体。 */
    private static class Order {

        /** 主键。 */
        private Long id;

        /** 状态。 */
        private String status;

        /** 是否已删除。 */
        @LogicDelete
        private Boolean deleted;
    }

    /** 不带逻辑删除字段的实体。 */
    private static class Plain {

        /** 主键。 */
        private Long id;
    }

    /** 逻辑删除字段定义在父类的实体。 */
    private static class Child extends Parent {
    }

    /** 逻辑删除字段定义在父类。 */
    private static class Parent {

        /** 是否已删除。 */
        @LogicDelete
        private boolean deleted;
    }

    /** 逻辑删除字段类型非法的实体。 */
    private static class Invalid {

        /** 是否已删除。 */
        @LogicDelete
        private String deleted;
    }
}
