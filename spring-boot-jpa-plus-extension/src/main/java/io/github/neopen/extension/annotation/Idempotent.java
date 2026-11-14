package io.github.neopen.extension.annotation;

import java.lang.annotation.*;

/**
 * 启用方法级幂等性校验。
 *
 * <p>切面支持两种模式：</p>
 *
 * <ul>
 *     <li>一次性令牌模式：{@link #key()} 为空时，从方法参数中查找
 *         {@code checkToken} 字段，并与当前会话缓存的令牌比对。</li>
 *     <li>业务键模式：配置 {@link #key()}（支持 SpEL）后，同一业务键
 *         在 {@link #ttl()} 秒内只允许成功执行一次。</li>
 * </ul>
 *
 * <pre>{@code
 * @Idempotent(required = true, key = "#orderNo", ttl = 30)
 * public void submit(String orderNo) {
 * }
 * }</pre>
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

    /**
     * 幂等业务键表达式，支持 SpEL。
     *
     * <p>配置后按业务键去重，不再依赖 HTTP 会话与一次性令牌；
     * 为空时沿用一次性令牌模式。</p>
     *
     * @return SpEL 表达式
     */
    String key() default "";

    /**
     * 业务键的去重时间窗口，单位秒。
     *
     * <p>仅在配置 {@link #key()} 时生效；小于等于 0 表示长期有效，
     * 直到缓存淘汰。默认值：5 秒。</p>
     *
     * @return 去重时间窗口秒数
     */
    long ttl() default 5;
}
