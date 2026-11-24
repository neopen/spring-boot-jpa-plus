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

import io.github.neopen.core.context.JpaPlusContextTaskDecorator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文传播自动配置测试。
 */
class JpaPlusContextPropagationAutoConfigurationTest {

    /** 加载自动配置的容器运行器。 */
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(
                    AutoConfigurations.of(JpaPlusAutoConfiguration.class)
            );

    /**
     * 验证默认注册上下文传播装饰器。
     */
    @Test
    void shouldRegisterDecoratorByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(TaskDecorator.class);
            assertThat(context.getBean(TaskDecorator.class))
                    .isInstanceOf(JpaPlusContextTaskDecorator.class);
        });
    }

    /**
     * 验证关闭开关后不再注册装饰器。
     */
    @Test
    void shouldSkipDecoratorWhenDisabled() {
        runner.withPropertyValues(
                "neopen.jpa.context-propagation.enabled=false"
        ).run(context -> assertThat(context)
                .doesNotHaveBean(TaskDecorator.class));
    }

    /**
     * 验证业务自定义装饰器优先。
     */
    @Test
    void shouldBackOffWhenUserProvidesDecorator() {
        runner.withBean(
                "userTaskDecorator",
                TaskDecorator.class,
                () -> (TaskDecorator) runnable -> runnable
        ).run(context -> {
            assertThat(context).hasSingleBean(TaskDecorator.class);
            assertThat(context.getBean(TaskDecorator.class))
                    .isNotInstanceOf(
                            JpaPlusContextTaskDecorator.class
                    );
        });
    }
}
