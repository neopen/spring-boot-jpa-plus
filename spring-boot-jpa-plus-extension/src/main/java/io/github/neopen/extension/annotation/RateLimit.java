package io.github.neopen.extension.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法级限流注解。
 *
 * <p>基于 Guava {@code RateLimiter} 在本地内存中限流，适用于单实例
 * 服务的接口保护；多实例部署时请改用分布式限流组件。</p>
 *
 * <pre>{@code
 * @RateLimit(permits = 10, interval = 1, unit = TimeUnit.SECONDS)
 * public void submit() {
 * }
 *
 * // 按用户维度限流，key 支持 SpEL
 * @RateLimit(key = "#userId", permits = 1, interval = 1)
 * public void send(String userId) {
 * }
 * }</pre>
 *
 * <p>限流器按解析后的 key 缓存，key 为空时使用
 * {@code 类名#方法名} 作为默认维度。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度表达式，支持 SpEL。
     *
     * <p>可引用方法参数，例如 {@code "#userId"}；为空时按方法维度限流。</p>
     *
     * @return SpEL 表达式
     */
    String key() default "";

    /**
     * 每个时间窗口允许的许可数量。
     *
     * @return 许可数量，必须大于 0
     */
    double permits() default 5;

    /**
     * 时间窗口长度。
     *
     * @return 时间窗口长度，必须大于 0
     */
    long interval() default 1;

    /**
     * 时间窗口单位。
     *
     * @return 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 触发限流时的提示信息。
     *
     * @return 提示信息
     */
    String message() default "操作过于频繁，请稍后重试";
}
