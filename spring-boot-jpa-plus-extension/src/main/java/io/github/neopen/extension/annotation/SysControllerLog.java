package io.github.neopen.extension.annotation;

import java.lang.annotation.*;

/**
 * 声明控制器方法的日志描述。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysControllerLog {
    /**
     * 获取控制器操作描述。
     *
     * @return 写入日志的操作描述
     */
	String value() default "";
}
