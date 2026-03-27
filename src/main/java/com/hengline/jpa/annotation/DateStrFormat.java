package com.hengline.jpa.annotation;

import java.lang.annotation.*;

/**
 * <b> 日期格式字符串
 * 
 * @author He.hp
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
	 * @author He.hp
	 * @date 2020年9月11日 下午2:11:48
	 * @return
	 */
	String value() default "";

	/**
	 * <b> 是否必填
	 * <P>
	 *
	 * @Author He.hp
	 * @Email hehp@citycloud.com.cn
	 * @Date 2021/6/29 15:57
	 * @return boolean
	 */
	boolean required() default false;

	/**
	 * <b> 是否忽略
	 * <P>
	 *
	 * @author He.hp
	 * @date 2020年9月11日 下午4:32:08
	 * @return
	 */
	boolean ignore() default false;
}
