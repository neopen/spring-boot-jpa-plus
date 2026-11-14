package io.github.neopen.core.persistence.logicdelete;

import io.github.neopen.core.persistence.spec.SpecWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    /**
     * 验证按主键逻辑删除生成更新语句。
     */
    @Test
    void shouldUpdateDeletedFlagById() {
        EntityManager entityManager = mockEntityManager();
        Query query = mockQuery(entityManager, 1);

        int affected = LogicDeletes.deleteById(
                entityManager,
                Order.class,
                5L
        );

        assertThat(affected).isEqualTo(1);
        verify(entityManager).createQuery(
                "update Order e set e.deleted = true where e.id in :ids"
        );
        verify(query).setParameter("ids", List.of(5L));
        verify(query).executeUpdate();
    }

    /**
     * 验证空主键集合不会执行语句。
     */
    @Test
    void shouldSkipEmptyIdCollection() {
        EntityManager entityManager = mockEntityManager();

        assertThat(LogicDeletes.deleteAllById(
                entityManager,
                Order.class,
                List.of()
        )).isZero();

        verify(entityManager, never()).createQuery(anyString());
    }

    /**
     * 验证实体标记为已删除。
     */
    @Test
    void shouldMarkEntitiesAsDeleted() {
        EntityManager entityManager = mockEntityManager();
        Order first = new Order();
        Order second = new Order();

        int count = LogicDeletes.deleteAll(
                entityManager,
                Order.class,
                List.of(first, second)
        );

        assertThat(count).isEqualTo(2);
        assertThat(first.isDeleted()).isTrue();
        assertThat(second.isDeleted()).isTrue();
    }

    /**
     * 验证未标注注解的实体拒绝逻辑删除。
     */
    @Test
    void shouldRejectLogicDeleteForAnnotatedOnly() {
        EntityManager entityManager = mockEntityManager();

        assertThatThrownBy(
                () -> LogicDeletes.deleteById(entityManager, Plain.class, 1L)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@LogicDelete is required");
    }

    /**
     * 创建带实体元数据的模拟实体管理器。
     *
     * @return 实体管理器
     */
    private EntityManager mockEntityManager() {
        EntityManager entityManager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        EntityType<Order> entityType = mock(EntityType.class);
        SingularAttribute<? super Order, ?> id =
                mock(SingularAttribute.class);

        when(entityManager.getMetamodel()).thenReturn(metamodel);
        doReturn(entityType).when(metamodel).entity(Order.class);
        when(entityType.getName()).thenReturn("Order");
        when(entityType.getSingularAttributes()).thenReturn(Set.of(id));
        when(id.isId()).thenReturn(true);
        when(id.getName()).thenReturn("id");

        return entityManager;
    }

    /**
     * 模拟更新语句。
     *
     * @param entityManager 实体管理器
     * @param affected      受影响行数
     * @return 查询对象
     */
    private Query mockQuery(EntityManager entityManager, int affected) {
        Query query = mock(Query.class);

        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(affected);

        return query;
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

        /**
         * 判断是否已标记删除。
         *
         * @return 是否已删除
         */
        boolean isDeleted() {
            return Boolean.TRUE.equals(deleted);
        }
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
