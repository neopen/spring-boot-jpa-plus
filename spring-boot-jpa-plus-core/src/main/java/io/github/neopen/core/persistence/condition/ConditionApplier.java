package io.github.neopen.core.persistence.condition;

/**
 * 条件应用回调。
 *
 * <p>由查询构造器实现，把参数注解解析出的条件写入具体查询。</p>
 */
@FunctionalInterface
public interface ConditionApplier {

    /**
     * 应用一个查询条件。
     *
     * @param field    字段名或列名
     * @param operator 条件运算符
     * @param value    条件值；无参运算符为 {@code null}
     * @param second   区间结束值；仅 {@link ConditionOperator#BETWEEN} 使用
     */
    void apply(
            String field,
            ConditionOperator operator,
            Object value,
            Object second
    );
}
