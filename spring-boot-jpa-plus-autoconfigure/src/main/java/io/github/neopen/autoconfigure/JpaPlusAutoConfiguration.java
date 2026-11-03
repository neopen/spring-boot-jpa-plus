package io.github.neopen.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import jakarta.persistence.EntityManager;

@AutoConfiguration
@ConditionalOnClass(EntityManager.class)
@ConditionalOnProperty(
        prefix = "neopen.jpa",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(JpaPlusProperties.class)
public class JpaPlusAutoConfiguration {

    // 后续在这里注册 JpaPlusDao、查询构建器、
    // 批量操作器等默认 Bean。

}
