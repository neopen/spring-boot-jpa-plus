package io.github.neopen.extension.annotation;

import java.lang.annotation.*;

/**
 * 声明服务方法的异常日志描述。
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysServiceLog {

    /**
     * 获取服务操作描述。
     *
     * @return 写入异常日志的操作描述
     */
    String value() default "";
}
