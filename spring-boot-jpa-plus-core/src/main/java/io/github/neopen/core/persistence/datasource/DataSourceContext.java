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
package io.github.neopen.core.persistence.datasource;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Supplier;

/**
 * 当前线程的数据源目标上下文。
 *
 * <p>使用栈保存数据源名称，支持嵌套切换：内层执行结束后自动
 * 恢复外层的数据源。手动调用时务必在 {@code finally} 中
 * {@link #pop()}，更推荐使用 {@link #run(String, Runnable)} 或
 * {@link #call(String, Supplier)}。</p>
 *
 * <p>上下文只影响当前线程；线程池场景请确保在任务内部设置，
 * 任务结束前清理。</p>
 *
 * @author neopen
 * @see DataSource
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class DataSourceContext {

    /** 按线程保存的数据源名称栈。 */
    private static final ThreadLocal<Deque<String>> TARGETS =
            ThreadLocal.withInitial(ArrayDeque::new);

    /** 工具类不允许实例化。 */
    private DataSourceContext() {
    }

    /**
     * 压入数据源名称。
     *
     * @param target 数据源名称，不能为空
     * @throws IllegalArgumentException 名称为空时抛出
     */
    public static void push(String target) {
        TARGETS.get().push(requireTarget(target));
    }

    /**
     * 弹出当前数据源名称。
     *
     * @return 被弹出的数据源名称；栈为空时返回 {@code null}
     */
    public static String pop() {
        Deque<String> targets = TARGETS.get();

        return targets.isEmpty() ? null : targets.pop();
    }

    /**
     * 获取当前数据源名称。
     *
     * @return 当前数据源名称；未设置时返回 {@code null}
     */
    public static String current() {
        return TARGETS.get().peek();
    }

    /**
     * 获取当前线程的数据源名称栈快照。
     *
     * <p>返回顺序为栈顶在前；快照创建后不受后续压栈、弹栈影响，
     * 用于 {@link io.github.neopen.core.context.JpaPlusContext} 的
     * 跨线程传播。</p>
     *
     * @return 栈顶在前的只读列表；栈为空时返回空列表
     */
    public static List<String> snapshot() {
        return List.copyOf(TARGETS.get());
    }

    /**
     * 清除当前线程的全部数据源配置。
     */
    public static void clear() {
        TARGETS.remove();
    }

    /**
     * 在指定数据源下执行操作。
     *
     * @param target 数据源名称，不能为空
     * @param action 待执行操作
     * @throws IllegalArgumentException 名称为空时抛出
     */
    public static void run(String target, Runnable action) {
        push(target);

        try {
            action.run();
        } finally {
            pop();
        }
    }

    /**
     * 在指定数据源下执行操作并返回结果。
     *
     * @param <T>    返回值类型
     * @param target 数据源名称，不能为空
     * @param action 待执行操作
     * @return 操作结果
     * @throws IllegalArgumentException 名称为空时抛出
     */
    public static <T> T call(String target, Supplier<T> action) {
        push(target);

        try {
            return action.get();
        } finally {
            pop();
        }
    }

    /**
     * 校验数据源名称。
     *
     * @param target 数据源名称
     * @return 去空格后的名称
     * @throws IllegalArgumentException 名称为空时抛出
     */
    private static String requireTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException(
                    "Data source target must not be blank"
            );
        }

        return target.trim();
    }
}
