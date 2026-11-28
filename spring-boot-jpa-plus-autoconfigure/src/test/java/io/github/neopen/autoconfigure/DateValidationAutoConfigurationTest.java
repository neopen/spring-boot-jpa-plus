/*
 * Copyright 2026 neopen
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
package io.github.neopen.autoconfigure;

import io.github.neopen.convert.aspect.DateStrAspect;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日期校验自动配置测试。
 */
class DateValidationAutoConfigurationTest {

    /** 上下文运行器。 */
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            JpaPlusAutoConfiguration.class
                    ));

    /**
     * 验证默认注册日期校验切面。
     */
    @Test
    void shouldRegisterDateStrAspectByDefault() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(DateStrAspect.class));
    }

    /**
     * 验证可以通过配置关闭日期校验切面。
     */
    @Test
    void shouldDisableDateStrAspect() {
        contextRunner.withPropertyValues(
                        "neopen.jpa.convert.date-validation.enabled=false"
                )
                .run(context -> assertThat(context)
                        .doesNotHaveBean(DateStrAspect.class));
    }
}
