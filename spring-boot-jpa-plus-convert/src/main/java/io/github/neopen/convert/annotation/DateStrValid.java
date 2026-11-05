package io.github.neopen.convert.annotation;

import java.lang.annotation.*;

/**
 * 启用方法参数的日期字符串校验。
 *
 * <p>标注在方法上时，切面会检查参数对象中带有
 * {@link DateStrFormat} 的字段。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Documented
public @interface DateStrValid {

}
