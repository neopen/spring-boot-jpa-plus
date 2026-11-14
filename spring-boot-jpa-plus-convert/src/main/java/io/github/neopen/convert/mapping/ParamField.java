package io.github.neopen.convert.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明请求参数字段与实体字段的映射关系。
 *
 * <p>当参数类字段名与实体字段名不一致时，在参数字段上标注该注解
 * 指定目标实体字段名；未标注时按同名字段自动匹配。</p>
 *
 * <pre>{@code
 * public class OrderParam {
 *
 *     @ParamField("orderNo")
 *     private String number;
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamField {

    /**
     * 目标实体字段名。
     *
     * @return 实体字段名
     */
    String value();
}
