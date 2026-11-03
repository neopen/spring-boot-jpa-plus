package io.github.neopen.jpa.annotation;

import java.lang.annotation.*;

/**
 * <b> 幂等性校验
 * <p>
 *
 * @Author He.hp
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
     * @Author He.hp
     * @Email hehp@citycloud.com.cn
     * @Date 2021/6/30 14:11
     */
    boolean required() default false;
}
