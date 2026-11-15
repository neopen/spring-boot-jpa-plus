package io.github.neopen.extension.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 乐观锁冲突自动重试。
 *
 * <p>方法因乐观锁冲突失败时按配置重试，常见于高并发更新场景。
 * 切面优先级最高，会包裹事务切面，保证每次重试都运行在独立事务中：</p>
 *
 * <pre>{@code
 * @RetryOnOptimisticLock(maxAttempts = 3, delay = 100)
 * @Transactional
 * public void updateStock(Long id, int quantity) {
 *     // ...
 * }
 * }</pre>
 *
 * <p>只有 {@code OptimisticLockingFailureException} 与
 * {@code OptimisticLockException}（含嵌套原因）会触发重试，其余异常
 * 立即抛出。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetryOnOptimisticLock {

    /**
     * 最大尝试次数，包含首次调用。
     *
     * @return 最大尝试次数，最小值为 1
     */
    int maxAttempts() default 3;

    /**
     * 两次尝试之间的等待时间，单位毫秒。
     *
     * @return 等待时间，小于 0 时按 0 处理
     */
    long delay() default 50L;
}
