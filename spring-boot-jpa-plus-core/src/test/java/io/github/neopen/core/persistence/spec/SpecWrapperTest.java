package io.github.neopen.core.persistence.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SpecWrapper} 单元测试。
 */
class SpecWrapperTest {

    /** 测试实体。 */
    private static class Sample {

        /** 主键。 */
        private Long id;

        /** 状态。 */
        private String status;

        /** 金额。 */
        private Integer amount;
    }

    /** 测试关联实体。 */
    private static class Item {

        /** 名称。 */
        private String name;
    }

    /**
     * 验证普通条件与嵌套 OR 分组的组合。
     */
    @Test
    void shouldCombineConditionsAndNestedOr() {
        Root<Sample> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Path<Object> statusPath = mock(Path.class);
        Path<Object> idPath = mock(Path.class);

        Predicate paid = mock(Predicate.class);
        Predicate idOne = mock(Predicate.class);
        Predicate created = mock(Predicate.class);
        Predicate orGroup = mock(Predicate.class);
        Predicate result = mock(Predicate.class);

        when(root.get("status")).thenReturn(statusPath);
        when(root.get("id")).thenReturn(idPath);
        when(cb.equal(statusPath, "PAID")).thenReturn(paid);
        when(cb.equal(idPath, 1L)).thenReturn(idOne);
        when(cb.equal(statusPath, "NEW")).thenReturn(created);
        when(cb.and(idOne, created)).thenReturn(orGroup);
        when(cb.or(paid, orGroup)).thenReturn(result);

        Predicate predicate = SpecWrapper.create(Sample.class)
                .eq("status", "PAID")
                .or(wrapper -> wrapper
                        .eq("id", 1L)
                        .eq("status", "NEW"))
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(result);
        verify(cb).and(idOne, created);
        verify(cb).or(paid, orGroup);
    }

    /**
     * 验证连表别名路径解析与自动去重。
     */
    @Test
    void shouldJoinAndResolveAliasPath() {
        Root<Sample> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        Join<Object, Object> join = mock(Join.class);
        Path<Object> namePath = mock(Path.class);
        Predicate result = mock(Predicate.class);

        doReturn(join).when(root).join("items", JoinType.INNER);
        doReturn(namePath).when(join).get("name");
        when(cb.like(any(Expression.class), eq("%机械%"))).thenReturn(result);

        Predicate predicate = SpecWrapper.create(Sample.class)
                .join("items")
                .like("items.name", "机械")
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(result);
        verify(join).alias("items");
        verify(join).get("name");
        verify(query).distinct(true);
    }

    /**
     * 验证 count 查询跳过 fetch join。
     */
    @Test
    void shouldSkipFetchOnCountQuery() {
        Root<Sample> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate conjunction = mock(Predicate.class);

        when(query.getResultType()).thenReturn((Class) Long.class);
        when(cb.conjunction()).thenReturn(conjunction);

        Predicate predicate = SpecWrapper.create(Sample.class)
                .fetch("items")
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(conjunction);
        verify(root, never()).fetch(anyString(), any(JoinType.class));
    }

    /**
     * 验证空集合 IN 条件生成恒假谓词。
     */
    @Test
    void shouldRenderEmptyInAsDisjunction() {
        Root<Sample> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate disjunction = mock(Predicate.class);

        when(cb.disjunction()).thenReturn(disjunction);

        Predicate predicate = SpecWrapper.create(Sample.class)
                .in("id", List.of())
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(disjunction);
        verify(cb).disjunction();
    }

    /**
     * 验证自定义 Specification 可以追加到条件树。
     */
    @Test
    void shouldAppendRawSpecification() {
        Root<Sample> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate raw = mock(Predicate.class);

        Specification<Sample> specification = (r, q, c) -> raw;

        Predicate predicate = SpecWrapper.create(Sample.class)
                .raw(specification)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(raw);
    }

    /**
     * 验证运算符参数数量校验。
     */
    @Test
    void shouldRejectInvalidParameterCount() {
        assertThatThrownBy(() -> SpecWrapper.create(Sample.class)
                .add("amount", StandardSpecOperator.BETWEEN, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expects 2");
    }

    /**
     * 验证重复连表别名校验。
     */
    @Test
    void shouldRejectDuplicateJoinAlias() {
        assertThatThrownBy(() -> SpecWrapper.create(Sample.class)
                .join("items", "i")
                .join("items", "i"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate join alias");
    }
}
