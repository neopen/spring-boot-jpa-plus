package io.github.neopen.core.persistence.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询参数条件注解。
 *
 * <p>标注在查询参数对象的字段上，配合 {@link QueryConditionMapper#apply}
 * 把非空参数自动映射为查询条件：</p>
 *
 * <pre>{@code
 * public class OrderQueryParam {
 *
 *     @QueryCondition(operator = ConditionOperator.LIKE)
 *     private String orderNo;
 *
 *     @QueryCondition(operator = ConditionOperator.BETWEEN, endField = "maxAmount")
 *     private BigDecimal minAmount;
 *
 *     private BigDecimal maxAmount;
 * }
 *
 * SpecWrapper.create(Order.class).apply(param);
 * }</pre>
 *
 * <p>默认忽略空值、空白字符串和空集合，因此未填写的查询参数不会
 * 参与条件拼接。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryCondition {

    /**
     * 字段名或列名，为空时使用被标注字段的名称。
     *
     * @return 字段名或列名
     */
    String value() default "";

    /**
     * 条件运算符。
     *
     * @return 条件运算符
     */
    ConditionOperator operator() default ConditionOperator.EQ;

    /**
     * 是否忽略空白字符串。
     *
     * @return 忽略空白字符串时返回 {@code true}
     */
    boolean ignoreBlank() default true;

    /**
     * 区间结束值所在字段名，仅 {@link ConditionOperator#BETWEEN} 使用。
     *
     * @return 区间结束值所在字段名
     */
    String endField() default "";

    /**
     * 条件分组名。
     *
     * <p>同一分组内的条件按 OR 组合，不同分组之间以及与未分组条件
     * 之间按 AND 组合。例如关键字搜索的多个候选字段可以配置为
     * 同一个分组。</p>
     *
     * @return 条件分组名；空字符串表示不分组
     */
    String group() default "";
}
