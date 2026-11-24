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
 * 区域限制可见数据。</p>
 *
 * <pre>{@code
 * List<DataRule> rules = List.of(
 *         DataRule.of("deptId", 1L, 2L),
 *         new DataRule("status", List.of("ACTIVE"))
 * );
 * }</pre>
 *
 * @param field  实体字段名
 * @param values 允许的字段值，不能为空
 * @author neopen
 * @see DataScopes
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record DataRule(String field, Collection<?> values) {

    /**
     * 校验字段与取值。
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

        values = List.copyOf(values);
    }

    /**
     * 创建数据权限规则。
     *
     * @param field  实体字段名
     * @param values 允许的字段值
     * @return 数据权限规则
     */
    public static DataRule of(String field, Object... values) {
        return new DataRule(field, List.of(values));
    }
}
