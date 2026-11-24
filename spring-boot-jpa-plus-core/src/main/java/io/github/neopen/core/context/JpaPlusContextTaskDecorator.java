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

import org.springframework.core.task.TaskDecorator;

/**
 * 传播 JPA Plus 上下文的任务装饰器。
 *
 * <p>在提交任务时捕获当前线程的 {@link JpaPlusContext} 快照，任务执行
 * 前应用该快照，执行结束后恢复工作线程原有上下文：</p>
 *
 * <pre>{@code
 * ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 * executor.setTaskDecorator(new JpaPlusContextTaskDecorator());
 * executor.initialize();
 * }</pre>
 *
 * <p>由于任务结束时恢复工作线程的原状态，线程池复用不会把上一个任务的
 * 作用域、过滤开关或数据源路由遗留给后续任务。租户值本身不包含在快照
 * 中，仍由 {@code TenantProvider} 决定。</p>
 *
 * @author neopen
 * @see JpaPlusContext
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class JpaPlusContextTaskDecorator implements TaskDecorator {

    /**
     * 创建上下文传播装饰器。
     */
    public JpaPlusContextTaskDecorator() {
    }

    /**
     * 捕获提交线程的上下文并包装任务。
     *
     * @param runnable 原始任务，不能为 {@code null}
     * @return 应用上下文快照并自动恢复的任务
     * @throws IllegalArgumentException 任务为空时抛出
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException(
                    "Runnable must not be null"
            );
        }

        JpaPlusContext snapshot = JpaPlusContext.capture();

        return () -> snapshot.run(runnable);
    }
}
