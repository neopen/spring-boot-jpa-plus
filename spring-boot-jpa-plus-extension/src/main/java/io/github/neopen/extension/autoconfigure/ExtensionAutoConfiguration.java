package io.github.neopen.extension.autoconfigure;

import io.github.neopen.extension.response.DefaultJsonPageResponseFactory;
import io.github.neopen.extension.response.JsonPageConverter;
import io.github.neopen.extension.response.JsonPageResponseFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * JPA Plus 扩展组件自动配置。
 *
 * <p>在业务应用未提供同类 Bean 时注册默认分页响应组件。</p>
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
}