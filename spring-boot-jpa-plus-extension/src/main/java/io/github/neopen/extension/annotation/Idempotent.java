package io.github.neopen.extension.annotation;

import java.lang.annotation.*;

/**
 * 启用方法级幂等性校验。
 *
 * <p>切面会根据请求会话和一次性令牌拦截重复提交。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 获取是否必须执行幂等性校验。
     *
     * @return 必须校验时返回 {@code true}
     */
    boolean required() default false;
}
