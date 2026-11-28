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
package io.github.neopen.core.persistence.query;

import java.util.Objects;

/**
 * 不可变的动态查询条件。
 *
 * <p>记录实体字段、操作符以及操作数，由查询构建器转换为
 * JPA Criteria 谓词。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class QueryCondition {

    /** 实体字段名。 */
    private final String field;

    /** 查询操作符。 */
    private final QueryOperator operator;

    /** 第一个操作数。 */
    private final Object value;

    /** 第二个操作数，主要用于范围查询。 */
    private final Object secondValue;

    /**
     * 创建只包含一个操作数的查询条件。
     *
     * @param field    实体字段名，不能为空白
     * @param operator 查询操作符，不能为 {@code null}
     * @param value    操作数，是否允许为空取决于操作符
     * @throws IllegalArgumentException 字段为空白
     * @throws NullPointerException     操作符为 {@code null}
     */
    public QueryCondition(
            String field,
            QueryOperator operator,
            Object value
    ) {
        this(field, operator, value, null);
    }

    /**
     * 创建包含两个操作数的查询条件。
     *
     * @param field       实体字段名，不能为空白
     * @param operator    查询操作符，不能为 {@code null}
     * @param value       第一个操作数
     * @param secondValue 第二个操作数，范围查询时表示结束值
     * @throws IllegalArgumentException 字段为空白
     * @throws NullPointerException     操作符为 {@code null}
     */
    public QueryCondition(
            String field,
            QueryOperator operator,
            Object value,
            Object secondValue
    ) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(
                    "Query field must not be blank"
            );
        }

        this.field = field;
        this.operator = Objects.requireNonNull(
                operator,
                "Query operator must not be null"
        );
        this.value = value;
        this.secondValue = secondValue;
    }

    /**
     * 获取实体字段名。
     *
     * @return 实体字段名
     */
    public String getField() {
        return field;
    }

    /**
     * 获取查询操作符。
     *
     * @return 查询操作符
     */
    public QueryOperator getOperator() {
        return operator;
    }

    /**
     * 获取第一个操作数。
     *
     * @return 第一个操作数，可能为 {@code null}
     */
    public Object getValue() {
        return value;
    }

    /**
     * 获取第二个操作数。
     *
     * @return 第二个操作数，未设置时为 {@code null}
     */
    public Object getSecondValue() {
        return secondValue;
    }
}
