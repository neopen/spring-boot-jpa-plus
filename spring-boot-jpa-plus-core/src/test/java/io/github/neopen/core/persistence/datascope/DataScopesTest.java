/*
 * Copyright 2026 neopen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.persistence.datascope;

import io.github.neopen.core.scope.JpaPlusScope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DataScopes} 单元测试。
 */
class DataScopesTest {

    /**
     * 恢复默认配置。
     */
    @AfterEach
    void reset() {
        while (DataScopes.isIgnored()) {
            DataScopes.restore();
        }

        DataScopes.setEnabled(true);
        DataScopes.setProvider(null);
        DataScopes.clearScope("report");
        JpaPlusScope.clear();
    }

    /**
     * 验证规则转换为 {@code IN} 条件。
     */
    @Test
    void shouldAppendInPredicate() {
        DataScopes.setProvider(type -> List.of(DataRule.of("deptId", 1L, 2L)));

        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate in = mock(Predicate.class);
        List<Long> values = List.of(1L, 2L);

        when(root.get("deptId")).thenReturn(path);
        when(path.in(values)).thenReturn(in);

        Predicate predicate = DataScopes.<Order>filter(Order.class)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(in);
        verify(path).in(values);
    }

    /**
     * 验证多条规则按 AND 合并。
     */
    @Test
    void shouldCombineMultipleRules() {
        DataScopes.setProvider(type -> List.of(
                DataRule.of("deptId", 1L),
                DataRule.of("status", "ACTIVE")
        ));

        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> dept = mock(Path.class);
        Path<Object> status = mock(Path.class);
        Predicate first = mock(Predicate.class);
        Predicate second = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("deptId")).thenReturn(dept);
        when(root.get("status")).thenReturn(status);
        when(dept.in(List.of(1L))).thenReturn(first);
        when(status.in(List.of("ACTIVE"))).thenReturn(second);
        when(cb.and(first, second)).thenReturn(combined);

        Predicate predicate = DataScopes.<Order>filter(Order.class)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(combined);
        verify(cb).and(first, second);
    }

    /**
     * 验证没有提供者时返回原条件。
     */
    @Test
    void shouldSkipWithoutProvider() {
        Specification<Order> business = (r, q, c) -> mock(Predicate.class);

        assertThat(DataScopes.filter(Order.class, business))
                .isSameAs(business);
        assertThat(DataScopes.isActive()).isFalse();
    }

    /**
     * 验证提供者返回空规则时不限制数据。
     */
    @Test
    void shouldSkipWhenRulesEmpty() {
        DataScopes.setProvider(type -> List.of());

        Specification<Order> business = (r, q, c) -> mock(Predicate.class);

        assertThat(DataScopes.filter(Order.class, business))
                .isSameAs(business);
        assertThat(DataScopes.isActive()).isTrue();
    }

    /**
     * 验证作用域关闭后不追加规则。
     */
    @Test
    void shouldSkipWhenScopedDisabled() {
        DataScopes.setProvider(type -> List.of(DataRule.of("deptId", 1L)));
        DataScopes.setEnabled("report", false);

        Specification<Order> business = (r, q, c) -> mock(Predicate.class);

        Specification<Order> result = JpaPlusScope.call(
                "report",
                () -> DataScopes.filter(Order.class, business)
        );

        assertThat(result).isSameAs(business);
        assertThat(DataScopes.isEnabled("report")).isFalse();
    }

    /**
     * 验证跳过数据权限后返回原条件。
     */
    @Test
    void shouldIgnoreDataScopeFilter() {
        DataScopes.setProvider(type -> List.of(DataRule.of("deptId", 1L)));

        Specification<Order> business = (r, q, c) -> mock(Predicate.class);

        Specification<Order> result = DataScopes.callIgnored(
                () -> DataScopes.filter(Order.class, business)
        );

        assertThat(result).isSameAs(business);
        assertThat(DataScopes.isIgnored()).isFalse();
        assertThat(DataScopes.isActive()).isTrue();
    }

    /**
     * 验证规则按 OR 组合并支持关联路径。
     */
    @Test
    void shouldCombineRulesWithOrAndNestedPath() {
        DataScopes.setProvider(type -> List.of(
                DataRule.of("deptId", 1L),
                DataRule.or("dept.parentId", 2L)
        ));

        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> dept = mock(Path.class);
        Path<Object> deptPath = mock(Path.class);
        Path<Object> parent = mock(Path.class);
        Predicate first = mock(Predicate.class);
        Predicate second = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(root.get("deptId")).thenReturn(dept);
        when(root.get("dept")).thenReturn(deptPath);
        when(deptPath.get("parentId")).thenReturn(parent);
        when(dept.in(List.of(1L))).thenReturn(first);
        when(parent.in(List.of(2L))).thenReturn(second);
        when(cb.or(first, second)).thenReturn(combined);

        Predicate predicate = DataScopes.<Order>filter(Order.class)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(combined);
        verify(cb).or(first, second);
    }

    /**
     * 验证空字段名被拒绝。
     */
    @Test
    void shouldRejectBlankField() {
        assertThatThrownBy(() -> new DataRule(" ", List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    /**
     * 验证空取值集合被拒绝。
     */
    @Test
    void shouldRejectEmptyValues() {
        assertThatThrownBy(() -> new DataRule("deptId", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    /** 测试实体。 */
    private static class Order {

        /** 主键。 */
        private Long id;
    }
}
