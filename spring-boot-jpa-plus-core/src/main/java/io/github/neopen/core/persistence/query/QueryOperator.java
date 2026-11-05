package io.github.neopen.core.persistence.query;

/**
 * 动态查询操作符。
 */
public enum QueryOperator {

    /** 等于；操作数为 {@code null} 时匹配空值。 */
    EQ,

    /** 不等于；操作数为 {@code null} 时匹配非空值。 */
    NE,

    /** 大于。 */
    GT,

    /** 大于或等于。 */
    GE,

    /** 小于。 */
    LT,

    /** 小于或等于。 */
    LE,

    /** 包含指定文本，对应两侧模糊匹配。 */
    LIKE,

    /** 以指定文本结尾，对应左侧模糊匹配。 */
    LIKE_LEFT,

    /** 以指定文本开头，对应右侧模糊匹配。 */
    LIKE_RIGHT,

    /** 字段值属于指定集合。 */
    IN,

    /** 字段值不属于指定集合。 */
    NOT_IN,

    /** 字段值位于指定的闭区间内。 */
    BETWEEN,

    /** 字段值为 {@code null}。 */
    IS_NULL,

    /** 字段值不为 {@code null}。 */
    IS_NOT_NULL
}
