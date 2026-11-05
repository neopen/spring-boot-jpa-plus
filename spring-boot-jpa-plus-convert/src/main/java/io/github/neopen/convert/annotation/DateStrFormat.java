package io.github.neopen.convert.annotation;

import java.lang.annotation.*;

/**
 * 声明日期字符串的格式及校验规则。
 *
 * <p>可标注在方法参数或字段上，由日期校验切面读取。</p>
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DateStrFormat {

    /**
     * 获取日期格式。
     *
     * @return 兼容 {@link java.text.SimpleDateFormat} 的格式，例如
     *         {@code yyyy-MM-dd HH:mm:ss}；空字符串表示不校验格式
     */
	String value() default "";

    /**
     * 获取日期值是否必填。
     *
     * @return 必填时返回 {@code true}
     */
	boolean required() default false;

    /**
     * 获取是否忽略日期格式校验。
     *
     * @return 忽略格式校验时返回 {@code true}
     */
	boolean ignore() default false;
}
