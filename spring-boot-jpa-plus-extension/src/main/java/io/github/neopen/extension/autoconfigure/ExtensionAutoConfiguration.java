package io.github.neopen.extension.autoconfigure;

import com.google.common.util.concurrent.RateLimiter;
import io.github.neopen.extension.aspect.IdempotentAspect;
import io.github.neopen.extension.aspect.OptimisticLockRetryAspect;
import io.github.neopen.extension.aspect.RateLimitAspect;
import io.github.neopen.extension.aspect.SlowLogAspect;
import io.github.neopen.extension.response.DefaultJsonPageResponseFactory;
import io.github.neopen.extension.response.JsonPageConverter;
import io.github.neopen.extension.response.JsonPageResponseFactory;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * JPA Plus 扩展组件自动配置。
 *
 * <p>在业务应用未提供同类 Bean 时注册默认分页响应组件，
 * 以及幂等和限流切面。</p>
 */
@AutoConfiguration
public class ExtensionAutoConfiguration {

    /**
     * 注册默认 JSON 分页响应工厂。
     *
     * <p>用户可通过自定义 {@link JsonPageResponseFactory} 覆盖默认实现。</p>
     *
     * @return 默认分页响应工厂
     */
    @Bean
    @ConditionalOnMissingBean(JsonPageResponseFactory.class)
    public JsonPageResponseFactory jsonPageResponseFactory() {
        return new DefaultJsonPageResponseFactory();
    }

    /**
     * 注册 JSON 分页转换器。
     *
     * @param responseFactory 分页响应工厂
     * @return 由 Spring 管理的分页转换器
     */
    @Bean
    @ConditionalOnMissingBean(JsonPageConverter.class)
    public JsonPageConverter jsonPageConverter(
            JsonPageResponseFactory responseFactory) {
        return new JsonPageConverter(responseFactory);
    }

    /**
     * 幂等切面配置。
     *
     * <p>一次性令牌模式依赖 Servlet 请求上下文，缺少 Servlet API
     * 时自动跳过。</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "jakarta.servlet.http.HttpServletRequest")
    @ConditionalOnProperty(
            prefix = "neopen.extension.idempotent",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    static class IdempotentAspectConfiguration {

        /**
         * 注册幂等切面。
         *
         * @return 幂等切面
         */
        @Bean
        @ConditionalOnMissingBean(IdempotentAspect.class)
        public IdempotentAspect idempotentAspect() {
            return new IdempotentAspect();
        }
    }

    /**
     * 限流切面配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Aspect.class, RateLimiter.class})
    @ConditionalOnProperty(
            prefix = "neopen.extension.rate-limit",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    static class RateLimitAspectConfiguration {

        /**
         * 注册限流切面。
         *
         * @return 限流切面
         */
        @Bean
        @ConditionalOnMissingBean(RateLimitAspect.class)
        public RateLimitAspect rateLimitAspect() {
            return new RateLimitAspect();
        }
    }

    /**
     * 乐观锁重试切面配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({Aspect.class, OptimisticLockingFailureException.class})
    @ConditionalOnProperty(
            prefix = "neopen.extension.retry",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    static class RetryAspectConfiguration {

        /**
         * 注册乐观锁重试切面。
         *
         * @return 重试切面
         */
        @Bean
        @ConditionalOnMissingBean(OptimisticLockRetryAspect.class)
        public OptimisticLockRetryAspect optimisticLockRetryAspect() {
            return new OptimisticLockRetryAspect();
        }
    }

    /**
     * 慢方法日志切面配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(Aspect.class)
    @ConditionalOnProperty(
            prefix = "neopen.extension.slow-log",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    static class SlowLogAspectConfiguration {

        /**
         * 注册慢方法日志切面。
         *
         * @return 慢日志切面
         */
        @Bean
        @ConditionalOnMissingBean(SlowLogAspect.class)
        public SlowLogAspect slowLogAspect() {
            return new SlowLogAspect();
        }
    }
}