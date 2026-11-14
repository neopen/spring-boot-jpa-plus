package io.github.neopen.extension.aspect;

import io.github.neopen.extension.annotation.RateLimit;
import io.github.neopen.extension.exception.RateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RateLimitAspect} 单元测试。
 */
class RateLimitAspectTest {

    /**
     * 验证方法维度限流生效。
     */
    @Test
    void shouldLimitByMethod() {
        RateLimitedService proxy = createProxy();

        assertThat(proxy.single()).isEqualTo("ok");

        assertThatThrownBy(proxy::single)
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("操作过于频繁");
    }

    /**
     * 验证 SpEL 业务键维度限流生效。
     */
    @Test
    void shouldLimitBySpelKey() {
        RateLimitedService proxy = createProxy();

        assertThat(proxy.perUser("user-a")).isEqualTo("user-a");
        assertThat(proxy.perUser("user-b")).isEqualTo("user-b");

        assertThatThrownBy(() -> proxy.perUser("user-a"))
                .isInstanceOf(RateLimitException.class);
    }

    /**
     * 创建带限流切面的代理。
     *
     * @return 代理对象
     */
    private RateLimitedService createProxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(
                new RateLimitedService()
        );

        factory.setProxyTargetClass(true);
        factory.addAspect(new RateLimitAspect());

        return factory.getProxy();
    }

    /** 限流服务。 */
    static class RateLimitedService {

        /**
         * 方法维度限流。
         *
         * @return 固定字符串
         */
        @RateLimit(permits = 1, interval = 60, unit = TimeUnit.SECONDS)
        public String single() {
            return "ok";
        }

        /**
         * 用户维度限流。
         *
         * @param userId 用户标识
         * @return 用户标识
         */
        @RateLimit(key = "#userId", permits = 1, interval = 60)
        public String perUser(String userId) {
            return userId;
        }
    }
}
