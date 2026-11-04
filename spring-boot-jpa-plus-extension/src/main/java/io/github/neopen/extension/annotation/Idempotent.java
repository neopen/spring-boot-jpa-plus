package io.github.neopen.extension.annotation;

import java.lang.annotation.*;

/**
 * <b> 幂等性校验
 * <p>
 *
 * @Author neopen
 * @Date 2021/6/30 14:10
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * <b> 是否必须
     * <p>
     *
     * @return boolean
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Date 2021/6/30 14:11
     */
    boolean required() default false;
}
