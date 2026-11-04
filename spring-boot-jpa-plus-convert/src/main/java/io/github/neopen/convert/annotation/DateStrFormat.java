package io.github.neopen.convert.annotation;

import java.lang.annotation.*;

/**
 * <b> 日期格式字符串
 * 
 * @Author neopen
 * @date 2020年9月11日 下午2:10:07
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DateStrFormat {

	/**
	 * <b> 格式：如 yyyy-MM-dd HH:mm:ss
	 * <P>
	 * 
	 * @Author neopen
	 * @date 2020年9月11日 下午2:11:48
	 * @return
	 */
	String value() default "";

	/**
	 * <b> 是否必填
	 * <P>
	 *
	 * @Author neopen
	 * @GitHub https://github.com/neopen/spring-boot-jpa-plus
	 * @Date 2021/6/29 15:57
	 * @return boolean
	 */
	boolean required() default false;

	/**
	 * <b> 是否忽略
	 * <P>
	 *
	 * @Author neopen
	 * @date 2020年9月11日 下午4:32:08
	 * @return
	 */
	boolean ignore() default false;
}
