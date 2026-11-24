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

import java.util.Collection;
import java.util.List;

/**
 * 行级数据权限规则。
 *
 * <p>表示某个字段的取值必须落在指定集合内，例如按部门、组织或
 * 区域限制可见数据。字段支持关联路径，例如 {@code dept.parentId}；
 * 多条规则之间默认按 {@code AND} 组合，使用 {@link #or(String,
 * Object...)} 创建按 {@code OR} 组合的规则。</p>
 *
 * <pre>{@code
 * List<DataRule> rules = List.of(
 *         DataRule.of("deptId", 1L, 2L),
 *         DataRule.or("ownerId", 9L),
 *         new DataRule("status", List.of("ACTIVE"))
 * );
 * // 生成 deptId in (1,2) or ownerId in (9) and status in ('ACTIVE')
 * }</pre>
 *
 * @param field  实体字段名，支持 {@code a.b} 形式的关联路径
 * @param values 允许的字段值，不能为空
 * @param logic  与上一条规则的组合方式，不能为 {@code null}
 * @author neopen
 * @see DataScopes
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record DataRule(String field, Collection<?> values, Logic logic) {

    /**
     * 规则之间的组合方式。
     */
    public enum Logic {

        /** 与上一条规则按 AND 组合，默认值。 */
        AND,

        /** 与上一条规则按 OR 组合。 */
        OR
    }

    /**
     * 使用默认的 {@code AND} 组合创建规则。
     *
     * @param field  实体字段名
     * @param values 允许的字段值
     */
    public DataRule(String field, Collection<?> values) {
        this(field, values, Logic.AND);
    }

    /**
     * 校验字段、取值与组合方式。
     */
    public DataRule {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(
                    "Data rule field must not be blank"
            );
        }

        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    "Data rule values must not be empty: " + field
            );
        }

        if (logic == null) {
            throw new IllegalArgumentException(
                    "Data rule logic must not be null: " + field
            );
        }

        values = List.copyOf(values);
    }

    /**
     * 创建按 {@code AND} 组合的数据权限规则。
     *
     * @param field  实体字段名
     * @param values 允许的字段值
     * @return 数据权限规则
     */
    public static DataRule of(String field, Object... values) {
        return new DataRule(field, List.of(values));
    }

    /**
     * 创建按 {@code OR} 组合的数据权限规则。
     *
     * @param field  实体字段名
     * @param values 允许的字段值
     * @return 数据权限规则
     */
    public static DataRule or(String field, Object... values) {
        return new DataRule(field, List.of(values), Logic.OR);
    }
}
