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
package io.github.neopen.core.context;

import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.datasource.DataSourceContext;
import io.github.neopen.core.persistence.tenant.Tenants;
import io.github.neopen.core.scope.JpaPlusScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JpaPlusContextTaskDecorator} 单元测试。
 */
class JpaPlusContextTaskDecoratorTest {

    /** 待测试的装饰器。 */
    private final JpaPlusContextTaskDecorator decorator =
            new JpaPlusContextTaskDecorator();

    /** 单线程池，用于验证线程复用不泄漏上下文。 */
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    /**
     * 清理上下文并关闭线程池。
     */
    @AfterEach
    void tearDown() {
        JpaPlusContext.clear();
        executor.shutdownNow();
    }

    /**
     * 验证任务使用提交时刻的上下文，而不是执行时刻的上下文。
     */
    @Test
    void shouldCaptureContextAtSubmissionTime() throws Exception {
        JpaPlusScope.set("async");
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");

        AtomicReference<String> observed = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> observed.set(
                JpaPlusScope.current()
                        + "|" + DataScopes.ignoreDepth()
                        + "|" + Tenants.ignoreDepth()
                        + "|" + DataSourceContext.current()
        ));

        JpaPlusContext.clear();

        executor.submit(() -> {
            JpaPlusScope.set("worker");
            decorated.run();
        }).get();

        assertThat(observed.get()).isEqualTo("async|1|1|read");
    }

    /**
     * 验证任务结束后恢复工作线程原有上下文。
     */
    @Test
    void shouldRestoreWorkerContextAfterTask() throws Exception {
        JpaPlusScope.set("async");
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");

        AtomicReference<String> observed = new AtomicReference<>();

        executor.submit(decorator.decorate(() -> observed.set(
                JpaPlusScope.current()
                        + "|" + DataScopes.ignoreDepth()
                        + "|" + Tenants.ignoreDepth()
                        + "|" + DataSourceContext.current()
        ))).get();

        assertThat(observed.get()).isEqualTo("async|1|1|read");

        JpaPlusContext remaining = executor.submit(
                JpaPlusContext::capture
        ).get();

        assertThat(remaining.getScope()).isNull();
        assertThat(remaining.getDataScopeIgnoreDepth()).isZero();
        assertThat(remaining.getTenantIgnoreDepth()).isZero();
        assertThat(remaining.getDataSourceTargets()).isEmpty();
    }

    /**
     * 验证任务异常时仍然恢复工作线程上下文。
     */
    @Test
    void shouldRestoreWorkerContextWhenTaskFails() throws Exception {
        JpaPlusScope.set("async");
        DataScopes.ignore();

        assertThatThrownBy(() -> executor.submit(
                decorator.decorate(() -> {
                    throw new IllegalStateException("boom");
                })
        ).get())
                .hasCauseInstanceOf(IllegalStateException.class);

        JpaPlusContext remaining = executor.submit(
                JpaPlusContext::capture
        ).get();

        assertThat(remaining.getScope()).isNull();
        assertThat(remaining.getDataScopeIgnoreDepth()).isZero();
    }

    /**
     * 验证空任务被拒绝。
     */
    @Test
    void shouldRejectNullRunnable() {
        assertThatThrownBy(() -> decorator.decorate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Runnable");
    }
}
