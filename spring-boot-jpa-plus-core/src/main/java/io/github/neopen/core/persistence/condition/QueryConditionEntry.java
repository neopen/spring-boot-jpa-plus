/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
package io.github.neopen.core.persistence.condition;

/**
 * 参数注解解析出的单个查询条件。
 *
 * <p>由 {@link QueryConditionMapper#resolve(Object)} 生成，供查询构造器
 * 按分组组装条件。</p>
 *
 * @param group    条件分组名，空字符串表示不分组
 * @param field    字段名或列名
 * @param operator 条件运算符
 * @param value    条件值；无参运算符为 {@code null}
 * @param second   区间结束值；仅 {@link ConditionOperator#BETWEEN} 使用
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record QueryConditionEntry(
        String group,
        String field,
        ConditionOperator operator,
        Object value,
        Object second
) {

    /**
     * 规范化分组名。
     */
    public QueryConditionEntry {
        group = group == null ? "" : group.trim();
    }

    /**
     * 判断是否属于条件分组。
     *
     * <p>同一分组的条件在查询构造器中按 OR 组合。</p>
     *
     * @return 属于分组时返回 {@code true}
     */
    public boolean grouped() {
        return !group.isEmpty();
    }
}
