package io.github.neopen.extension.autoconfigure;

import io.github.neopen.extension.aspect.IdempotentAspect;
import io.github.neopen.extension.aspect.RateLimitAspect;
import io.github.neopen.extension.response.JsonPageConverter;
import io.github.neopen.extension.response.JsonPageResponseFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExtensionAutoConfiguration} 单元测试。
 */
class ExtensionAutoConfigurationTest {

    /** 自动配置测试上下文。 */
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            ExtensionAutoConfiguration.class
                    ));

    /**
     * 验证默认组件与切面均已注册。
     */
    @Test
    void shouldRegisterDefaultComponents() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(JsonPageResponseFactory.class);
            assertThat(context).hasSingleBean(JsonPageConverter.class);
            assertThat(context).hasSingleBean(RateLimitAspect.class);
            assertThat(context).hasSingleBean(IdempotentAspect.class);
        });
    }

    /**
     * 验证可以通过配置关闭切面。
     */
    @Test
    void shouldDisableAspectsByProperty() {
        runner.withPropertyValues(
                "neopen.extension.rate-limit.enabled=false",
                "neopen.extension.idempotent.enabled=false"
        ).run(context -> {
            assertThat(context).doesNotHaveBean(RateLimitAspect.class);
            assertThat(context).doesNotHaveBean(IdempotentAspect.class);
        });
    }
}
