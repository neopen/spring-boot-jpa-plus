package io.github.neopen.core.persistence.condition;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 查询参数注解支持的条件运算符。
 *
 * <p>用于 {@link QueryCondition} 声明字段的查询方式，是
 * {@code SpecWrapper}、{@code HqlWrapper} 和 {@code SqlWrapper}
 * 运算符的统一抽象。</p>
 */
public enum ConditionOperator {

    /** 等于。 */
    EQ(1),

    /** 不等于。 */
    NE(1),

    /** 大于。 */
    GT(1),

    /** 大于或等于。 */
    GE(1),

    /** 小于。 */
    LT(1),

    /** 小于或等于。 */
    LE(1),

    /** 两侧模糊匹配。 */
    LIKE(1),

    /** 左侧模糊匹配，即右模糊。 */
    LIKE_LEFT(1),

    /** 右侧模糊匹配，即左模糊。 */
    LIKE_RIGHT(1),

    /** 两侧不匹配。 */
    NOT_LIKE(1),

    /** 属于集合。 */
    IN(1),

    /** 不属于集合。 */
    NOT_IN(1),

    /** 闭区间，需要配合 {@link QueryCondition#endField()} 使用。 */
    BETWEEN(2),

    /** 为空，注解字段作为开关使用。 */
    IS_NULL(0),

    /** 不为空，注解字段作为开关使用。 */
    IS_NOT_NULL(0);

    /** 运算符需要的参数数量。 */
    private final int parameterCount;

    /**
     * 创建条件运算符。
     *
     * @param parameterCount 参数数量
     */
    ConditionOperator(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    /**
     * 获取运算符需要的参数数量。
     *
     * @return 参数数量
     */
    public int parameterCount() {
        return parameterCount;
    }

    /**
     * 为模糊匹配值补齐通配符。
     *
     * <p>非模糊匹配运算符原样返回；非字符串值也原样返回。</p>
     *
     * @param value 原始值
     * @return 补齐通配符后的值
     */
    public Object decorate(Object value) {
        if (!(value instanceof String text)) {
            return value;
        }

        return switch (this) {
            case LIKE, NOT_LIKE -> "%" + text + "%";
            case LIKE_LEFT -> "%" + text;
            case LIKE_RIGHT -> text + "%";
            default -> text;
        };
    }

    /**
     * 将条件值转换为集合。
     *
     * @param value 条件值
     * @return 集合
     * @throws IllegalArgumentException 值不是集合或数组时抛出
     */
    public Collection<?> toCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection;
        }

        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);

            for (int i = 0; i < length; i++) {
                values.add(Array.get(value, i));
            }

            return values;
        }

        throw new IllegalArgumentException(
                "Operator " + this + " requires a Collection value, but got "
                        + (value == null ? "null" : value.getClass().getName())
        );
    }
}
