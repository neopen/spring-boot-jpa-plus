package io.github.neopen.core.persistence.query;

import java.util.Objects;

/**
 * 动态查询条件
 */
public final class QueryCondition {

    private final String field;

    private final QueryOperator operator;

    private final Object value;

    private final Object secondValue;

    public QueryCondition(
            String field,
            QueryOperator operator,
            Object value
    ) {
        this(field, operator, value, null);
    }

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

    public String getField() {
        return field;
    }

    public QueryOperator getOperator() {
        return operator;
    }

    public Object getValue() {
        return value;
    }

    public Object getSecondValue() {
        return secondValue;
    }
}
