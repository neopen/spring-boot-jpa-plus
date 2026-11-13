package io.github.neopen.core.persistence.spec;

import io.github.neopen.core.persistence.logicdelete.LogicDelete;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JpaPlusSpecExecutor} 单元测试。
 */
class JpaPlusSpecExecutorTest {

    /**
     * 验证构造参数校验。
     */
    @Test
    void shouldValidateConstructorArguments() {
        EntityManager entityManager = mock(EntityManager.class);

        assertThatThrownBy(
                () -> JpaPlusSpecExecutor.of(null, Sample.class)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entity manager");

        assertThatThrownBy(
                () -> JpaPlusSpecExecutor.of(entityManager, null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entity type");

        assertThatThrownBy(
                () -> JpaPlusSpecExecutor.of(
                        entityManager,
                        Sample.class,
                        " "
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Id attribute");
    }

    /**
     * 验证 count 使用主键去重并追加逻辑删除条件。
     */
    @Test
    void shouldCountDistinctWithLogicDeleteFilter() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> query = mock(CriteriaQuery.class);
        Root<Sample> root = mock(Root.class);
        Path<Object> idPath = mock(Path.class);
        Path<Object> deletedPath = mock(Path.class);
        Expression<Long> countExpression = mock(Expression.class);
        Predicate logicPredicate = mock(Predicate.class);
        TypedQuery<Long> typedQuery = mock(TypedQuery.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Long.class)).thenReturn(query);
        when(query.from(Sample.class)).thenReturn(root);
        when(root.get("id")).thenReturn(idPath);
        when(cb.countDistinct(idPath)).thenReturn(countExpression);
        when(root.get("deleted")).thenReturn(deletedPath);
        when(cb.equal(deletedPath, false)).thenReturn(logicPredicate);
        when(entityManager.createQuery(query)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(7L);

        long total = JpaPlusSpecExecutor.of(entityManager, Sample.class)
                .count(null);

        assertThat(total).isEqualTo(7L);
        verify(cb).countDistinct(idPath);
        verify(query).where(logicPredicate);
    }

    /**
     * 验证关闭逻辑删除后不再追加过滤条件。
     */
    @Test
    void shouldSkipLogicDeleteFilterWhenDisabled() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> query = mock(CriteriaQuery.class);
        Root<Sample> root = mock(Root.class);
        Path<Object> idPath = mock(Path.class);
        Expression<Long> countExpression = mock(Expression.class);
        TypedQuery<Long> typedQuery = mock(TypedQuery.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Long.class)).thenReturn(query);
        when(query.from(Sample.class)).thenReturn(root);
        when(root.get("id")).thenReturn(idPath);
        when(cb.countDistinct(idPath)).thenReturn(countExpression);
        when(entityManager.createQuery(query)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(3L);

        long total = JpaPlusSpecExecutor.of(entityManager, Sample.class)
                .logicDelete(false)
                .count(null);

        assertThat(total).isEqualTo(3L);
        verify(root, never()).get("deleted");
    }

    /**
     * 验证切片查询多取一条判断下一页。
     */
    @Test
    void shouldFetchOneExtraRowForSlice() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Sample> query = mock(CriteriaQuery.class);
        Root<Sample> root = mock(Root.class);
        Path<Object> idPath = mock(Path.class);
        Path<Object> deletedPath = mock(Path.class);
        Predicate logicPredicate = mock(Predicate.class);
        Order idOrder = mock(Order.class);
        TypedQuery<Sample> typedQuery = mock(TypedQuery.class);

        Sample first = new Sample();
        Sample second = new Sample();
        Sample third = new Sample();
        Pageable pageable = PageRequest.of(0, 2);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Sample.class)).thenReturn(query);
        when(query.from(Sample.class)).thenReturn(root);
        when(query.select(root)).thenReturn(query);
        when(root.get("deleted")).thenReturn(deletedPath);
        when(cb.equal(deletedPath, false)).thenReturn(logicPredicate);
        when(root.get("id")).thenReturn(idPath);
        when(cb.asc(idPath)).thenReturn(idOrder);
        when(entityManager.createQuery(query)).thenReturn(typedQuery);
        when(typedQuery.getResultList())
                .thenReturn(List.of(first, second, third));

        Slice<Sample> slice = JpaPlusSpecExecutor
                .of(entityManager, Sample.class)
                .findSlice(null, pageable);

        assertThat(slice.getContent()).containsExactly(first, second);
        assertThat(slice.hasNext()).isTrue();
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(3);
    }

    /**
     * 验证总数为零时直接返回空分页。
     */
    @Test
    void shouldReturnEmptyPageWhenTotalIsZero() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> query = mock(CriteriaQuery.class);
        Root<Sample> root = mock(Root.class);
        Path<Object> idPath = mock(Path.class);
        Path<Object> deletedPath = mock(Path.class);
        Expression<Long> countExpression = mock(Expression.class);
        Predicate logicPredicate = mock(Predicate.class);
        TypedQuery<Long> typedQuery = mock(TypedQuery.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Long.class)).thenReturn(query);
        when(query.from(Sample.class)).thenReturn(root);
        when(root.get("id")).thenReturn(idPath);
        when(cb.countDistinct(idPath)).thenReturn(countExpression);
        when(root.get("deleted")).thenReturn(deletedPath);
        when(cb.equal(deletedPath, false)).thenReturn(logicPredicate);
        when(entityManager.createQuery(query)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(0L);

        Page<Sample> page = JpaPlusSpecExecutor
                .of(entityManager, Sample.class)
                .findPage(null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    /** 测试实体。 */
    private static class Sample {

        /** 主键。 */
        private Long id;

        /** 是否已删除。 */
        @LogicDelete
        private Boolean deleted;
    }
}
