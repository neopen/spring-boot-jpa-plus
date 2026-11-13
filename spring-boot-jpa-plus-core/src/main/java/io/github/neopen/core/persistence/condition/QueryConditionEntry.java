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
