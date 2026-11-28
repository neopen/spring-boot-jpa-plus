/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.extension.autoconfigure;

import io.github.neopen.extension.aspect.IdempotentAspect;
import io.github.neopen.extension.aspect.OptimisticLockRetryAspect;
import io.github.neopen.extension.aspect.RateLimitAspect;
import io.github.neopen.extension.aspect.SlowLogAspect;
import io.github.neopen.extension.desensitize.DesensitizeModule;
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
            assertThat(context).hasSingleBean(
                    OptimisticLockRetryAspect.class
            );
            assertThat(context).hasSingleBean(SlowLogAspect.class);
            assertThat(context).hasSingleBean(DesensitizeModule.class);
        });
    }

    /**
     * 验证可以通过配置关闭切面。
     */
    @Test
    void shouldDisableAspectsByProperty() {
        runner.withPropertyValues(
                "neopen.extension.rate-limit.enabled=false",
                "neopen.extension.idempotent.enabled=false",
                "neopen.extension.retry.enabled=false",
                "neopen.extension.slow-log.enabled=false"
        ).run(context -> {
            assertThat(context).doesNotHaveBean(RateLimitAspect.class);
            assertThat(context).doesNotHaveBean(IdempotentAspect.class);
            assertThat(context).doesNotHaveBean(
                    OptimisticLockRetryAspect.class
            );
            assertThat(context).doesNotHaveBean(SlowLogAspect.class);
        });
    }
}
