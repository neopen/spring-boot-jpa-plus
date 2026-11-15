package io.github.neopen.extension.aspect;

import io.github.neopen.extension.annotation.RetryOnOptimisticLock;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OptimisticLockRetryAspect} 单元测试。
 */
class OptimisticLockRetryAspectTest {

    /**
     * 验证冲突后重试直到成功。
     */
    @Test
    void shouldRetryUntilSuccess() {
        FlakyService service = new FlakyService(2);
        FlakyService proxy = proxy(service);

        String result = proxy.load();

        assertThat(result).isEqualTo("ok");
        assertThat(service.getAttempts()).isEqualTo(3);
    }

    /**
     * 验证达到最大次数后抛出原始异常。
     */
    @Test
    void shouldStopAfterMaxAttempts() {
        FlakyService service = new FlakyService(Integer.MAX_VALUE);
        FlakyService proxy = proxy(service);

        assertThatThrownBy(proxy::load)
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(service.getAttempts()).isEqualTo(3);
    }

    /**
     * 验证非乐观锁异常不重试。
     */
    @Test
    void shouldNotRetryOtherExceptions() {
        FailingService service = new FailingService();
        FailingService proxy = proxy(service);

        assertThatThrownBy(proxy::load)
                .isInstanceOf(IllegalStateException.class);

        assertThat(service.getAttempts()).isEqualTo(1);
    }

    /**
     * 验证包装在其他异常中的冲突同样触发重试。
     */
    @Test
    void shouldRetryWrappedOptimisticLockCause() {
        WrappedService service = new WrappedService(1);
        WrappedService proxy = proxy(service);

        assertThat(proxy.load()).isEqualTo("ok");
        assertThat(service.getAttempts()).isEqualTo(2);
    }

    /**
     * 创建代理。
     *
     * @param target 目标对象
     * @param <T>    目标类型
     * @return 代理对象
     */
    private static <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new OptimisticLockRetryAspect());
        return factory.getProxy();
    }

    /**
     * 前若干次冲突、之后成功的服务。
     */
    public static class FlakyService {

        /** 剩余失败次数。 */
        private final int failures;

        /** 调用次数。 */
        private int attempts;

        /**
         * 创建服务。
         *
         * @param failures 失败次数
         */
        public FlakyService(int failures) {
            this.failures = failures;
        }

        /**
         * 获取调用次数。
         *
         * @return 调用次数
         */
        public int getAttempts() {
            return attempts;
        }

        /**
         * 模拟更新操作。
         *
         * @return 成功标记
         */
        @RetryOnOptimisticLock(maxAttempts = 3, delay = 0)
        public String load() {
            attempts++;

            if (attempts <= failures) {
                throw new OptimisticLockingFailureException("conflict");
            }

            return "ok";
        }
    }

    /**
     * 总是抛出非乐观锁异常的服务。
     */
    public static class FailingService {

        /** 调用次数。 */
        private int attempts;

        /**
         * 获取调用次数。
         *
         * @return 调用次数
         */
        public int getAttempts() {
            return attempts;
        }

        /**
         * 模拟更新操作。
         *
         * @return 成功标记
         */
        @RetryOnOptimisticLock(maxAttempts = 3, delay = 0)
        public String load() {
            attempts++;
            throw new IllegalStateException("boom");
        }
    }

    /**
     * 包装乐观锁异常的服务。
     */
    public static class WrappedService {

        /** 剩余失败次数。 */
        private final int failures;

        /** 调用次数。 */
        private int attempts;

        /**
         * 创建服务。
         *
         * @param failures 失败次数
         */
        public WrappedService(int failures) {
            this.failures = failures;
        }

        /**
         * 获取调用次数。
         *
         * @return 调用次数
         */
        public int getAttempts() {
            return attempts;
        }

        /**
         * 模拟更新操作。
         *
         * @return 成功标记
         */
        @RetryOnOptimisticLock(maxAttempts = 3, delay = 0)
        public String load() {
            attempts++;

            if (attempts <= failures) {
                throw new IllegalStateException(
                        "wrapped",
                        new OptimisticLockingFailureException("conflict")
                );
            }

            return "ok";
        }
    }
}
