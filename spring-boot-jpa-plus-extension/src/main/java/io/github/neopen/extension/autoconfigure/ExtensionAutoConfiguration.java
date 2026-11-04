package io.github.neopen.extension.autoconfigure;

import io.github.neopen.extension.response.DefaultJsonPageResponseFactory;
import io.github.neopen.extension.response.JsonPageConverter;
import io.github.neopen.extension.response.JsonPageResponseFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ExtensionAutoConfiguration {

    /**
     * 默认 JSON 分页响应工厂。
     * 用户可以通过自定义 JsonPageResponseFactory 覆盖默认实现。
     */
    @Bean
    @ConditionalOnMissingBean(JsonPageResponseFactory.class)
    public JsonPageResponseFactory jsonPageResponseFactory() {
        return new DefaultJsonPageResponseFactory();
    }

    /**
     * JSON 分页转换器。
     * Spring 默认以单例方式管理。
     */
    @Bean
    @ConditionalOnMissingBean(JsonPageConverter.class)
    public JsonPageConverter jsonPageConverter(
            JsonPageResponseFactory responseFactory) {
        return new JsonPageConverter(responseFactory);
    }
}