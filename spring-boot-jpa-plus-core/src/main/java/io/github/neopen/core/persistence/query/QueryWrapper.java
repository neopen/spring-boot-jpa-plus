package io.github.neopen.core.persistence.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JPA Plus 动态查询构造器。
 *
 * <p>通过链式调用累积查询条件，所有条件最终以逻辑与组合。</p>
 *
 * @param <T> 实体类型
 */
public class QueryWrapper<T> {

    /**
     * 创建空的动态查询构造器。
     */
    public QueryWrapper() {
    }

    /** 按添加顺序保存的查询条件。 */
    private final List<QueryCondition> conditions =
            new ArrayList<>();

    /**
     * 添加等于条件。
     *
     * @param field 实体字段名
     * @param value 匹配值；为 {@code null} 时匹配空值
     * @return 当前查询构造器
     */
    public QueryWrapper<T> eq(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.EQ, value);
    }

    /**
     * 添加不等于条件。
     *
     * @param field 实体字段名
     * @param value 匹配值；为 {@code null} 时匹配非空值
     * @return 当前查询构造器
     */
    public QueryWrapper<T> ne(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.NE, value);
    }

    /**
     * 添加大于条件。
     *
     * @param field 实体字段名
     * @param value 可比较的下界值
     * @return 当前查询构造器
     */
    public QueryWrapper<T> gt(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.GT, value);
    }

    /**
     * 添加大于或等于条件。
     *
     * @param field 实体字段名
     * @param value 可比较的下界值
     * @return 当前查询构造器
     */
    public QueryWrapper<T> ge(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.GE, value);
    }

    /**
     * 添加小于条件。
     *
     * @param field 实体字段名
     * @param value 可比较的上界值
     * @return 当前查询构造器
     */
    public QueryWrapper<T> lt(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.LT, value);
    }

    /**
     * 添加小于或等于条件。
     *
     * @param field 实体字段名
     * @param value 可比较的上界值
     * @return 当前查询构造器
     */
    public QueryWrapper<T> le(
            String field,
            Object value
    ) {
        return add(field, QueryOperator.LE, value);
    }

    /**
     * 添加两侧模糊匹配条件。
     *
     * @param field 实体字段名
     * @param value 需要包含的文本
     * @return 当前查询构造器
     */
    public QueryWrapper<T> like(
            String field,
            String value
    ) {
        return add(field, QueryOperator.LIKE, value);
    }

    /**
     * 添加以指定文本结尾的匹配条件。
     *
     * @param field 实体字段名
     * @param value 结尾文本
     * @return 当前查询构造器
     */
    public QueryWrapper<T> likeLeft(
            String field,
            String value
    ) {
        return add(field, QueryOperator.LIKE_LEFT, value);
    }

    /**
     * 添加以指定文本开头的匹配条件。
     *
     * @param field 实体字段名
     * @param value 开头文本
     * @return 当前查询构造器
     */
    public QueryWrapper<T> likeRight(
            String field,
            String value
    ) {
        return add(field, QueryOperator.LIKE_RIGHT, value);
    }

    /**
     * 添加字段值属于指定集合的条件。
     *
     * @param field  实体字段名
     * @param values 候选值集合
     * @return 当前查询构造器
     */
    public QueryWrapper<T> in(
            String field,
            Collection<?> values
    ) {
        return add(field, QueryOperator.IN, values);
    }

    /**
     * 添加字段值不属于指定集合的条件。
     *
     * @param field  实体字段名
     * @param values 排除值集合
     * @return 当前查询构造器
     */
    public QueryWrapper<T> notIn(
            String field,
            Collection<?> values
    ) {
        return add(field, QueryOperator.NOT_IN, values);
    }

    /**
     * 添加闭区间范围条件。
     *
     * @param field 实体字段名
     * @param start 可比较的区间起始值
     * @param end   可比较的区间结束值
     * @return 当前查询构造器
     */
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

    /**
     * 添加字段值为空的条件。
     *
     * @param field 实体字段名
     * @return 当前查询构造器
     */
    public QueryWrapper<T> isNull(String field) {
        return add(field, QueryOperator.IS_NULL, null);
    }

    /**
     * 添加字段值非空的条件。
     *
     * @param field 实体字段名
     * @return 当前查询构造器
     */
    public QueryWrapper<T> isNotNull(String field) {
        return add(field, QueryOperator.IS_NOT_NULL, null);
    }

    /**
     * 添加通用单值查询条件。
     *
     * @param field    实体字段名
     * @param operator 查询操作符
     * @param value    条件操作数
     * @return 当前查询构造器
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
     * 判断是否尚未添加查询条件。
     *
     * @return 没有查询条件时返回 {@code true}
     */
    public boolean isEmpty() {
        return conditions.isEmpty();
    }

    /**
     * 获取只读查询条件列表。
     *
     * @return 当前条件的不可修改视图
     */
    public List<QueryCondition> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /**
     * 清空全部查询条件。
     *
     * @return 当前查询构造器
     */
    public QueryWrapper<T> clear() {
        conditions.clear();
        return this;
    }
}
