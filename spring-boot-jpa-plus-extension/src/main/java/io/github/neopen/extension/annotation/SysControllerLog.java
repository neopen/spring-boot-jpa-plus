package io.github.neopen.extension.annotation;

import java.lang.annotation.*;

/**
 * <b> controller 日志注解
 * <P>
 * 
 * @Author neopen
 * @date 2020年6月12日 下午2:46:47
 * @version 1.0
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME) // 运行时注解
@Documented
public @interface SysControllerLog {
	/**
	 * <b> 描述说明
	 * <P>
	 * 
	 * @Author neopen
	 * @date 2020年6月12日 下午2:47:20
	 * @return
	 */
	String value() default "";
}
