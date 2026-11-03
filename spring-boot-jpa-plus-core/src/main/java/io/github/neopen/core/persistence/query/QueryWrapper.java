package io.github.neopen.core.persistence.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JPA Plus 动态查询构造器
 *
 * @param <T> 实体类型
 */
public class QueryWrapper<T> {

    private final List<QueryCondition> conditions =
            new ArrayList<>();

    public QueryWrapper<T> eq(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.EQ, value);
    }

    public QueryWrapper<T> ne(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.NE, value);
    }

    public QueryWrapper<T> gt(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.GT, value);
    }

    public QueryWrapper<T> ge(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.GE, value);
    }

    public QueryWrapper<T> lt(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.LT, value);
    }

    public QueryWrapper<T> le(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.LE, value);
    }

    public QueryWrapper<T> like(
            String field,
            String value
    ) {
        return add(field, QueryOperator.LIKE, value);
    }

    public QueryWrapper<T> likeLeft(
            String field,
            String value
    ) {
        return add(field, QueryOperator.LIKE_LEFT, value);
    }

    public QueryWrapper<T> likeRight(
            String field,
            String value
    ) {
        return add(field, QueryOperator.LIKE_RIGHT, value);
    }

    public QueryWrapper<T> in(
            String field,
            Collection<?> values
    ) {
        return add(field, QueryOperator.IN, values);
    }

    public QueryWrapper<T> notIn(
            String field,
            Collection<?> values
    ) {
        return add(field, QueryOperator.NOT_IN, values);
    }

    public QueryWrapper<T> between(
            String field,
            Object start,
            Object end
    ) {
        conditions.add(
                new QueryCondition(
                        field,
                        QueryOperator.BETWEEN,
                        start,
                        end
                )
        );
        return this;
    }

    public QueryWrapper<T> isNull(String field) {
        return add(field, QueryOperator.IS_NULL, null);
    }

    public QueryWrapper<T> isNotNull(String field) {
        return add(field, QueryOperator.IS_NOT_NULL, null);
    }

    /**
     * 添加查询条件
     */
    public QueryWrapper<T> add(
            String field,
            QueryOperator operator,
            Object value
    ) {
        conditions.add(
                new QueryCondition(field, operator, value)
        );
        return this;
    }

    /**
     * 是否包含查询条件
     */
    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    /**
     * 获取只读条件集合
     */
    public List<QueryCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /**
     * 清空查询条件
     */
    public QueryWrapper<T> clear() {
        conditions.clear();
        return this;
    }
}
