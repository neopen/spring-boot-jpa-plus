package io.github.neopen.extension.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 慢方法日志。
 *
 * <p>方法执行时间达到阈值时输出 WARN 日志，便于定位性能问题：</p>
 *
 * <pre>{@code
 * @SlowLog(threshold = 200)
 * public List<Order> loadOrders(Long userId) {
 *     // ...
 * }
 * }</pre>
 *
 * <p>方法抛出异常时同样记录耗时，不影响异常向外传播。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SlowLog {

    /**
     * 慢方法阈值，单位毫秒。
     *
     * @return 阈值，默认 500 毫秒
     */
    long threshold() default 500L;
}
