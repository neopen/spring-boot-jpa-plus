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

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * JPA Plus 线程上下文快照。
 *
 * <p>把当前线程上的作用域、数据权限与租户过滤跳过深度、数据源路由
 * 栈复制为不可变快照，用于跨线程传播或在当前线程恢复：</p>
 *
 * <pre>{@code
 * JpaPlusContext snapshot = JpaPlusContext.capture();
 *
 * executor.submit(() -> snapshot.run(() -> repository.findAll(spec)));
 * }</pre>
 *
 * <p>{@link #run(Runnable)} 与 {@link #call(Supplier)} 会在执行前应用
 * 快照，执行结束后恢复执行线程原有状态，因此线程池复用不会残留上下文。
 * 快照不包含租户值本身，租户值仍由 {@code TenantProvider} 决定，使用方
 * 需要保证其线程安全或自行传播请求上下文。</p>
 *
 * <p>快照创建后不可变，可以安全地在多个线程之间共享。</p>
 *
 * @author neopen
 * @see JpaPlusContextTaskDecorator
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class JpaPlusContext {

    /** 作用域名称；未绑定时为 {@code null}。 */
    private final String scope;

    /** 跳过数据权限过滤的嵌套层数。 */
    private final int dataScopeIgnoreDepth;

    /** 跳过租户过滤的嵌套层数。 */
    private final int tenantIgnoreDepth;

    /** 数据源路由栈，栈顶在前。 */
    private final List<String> dataSourceTargets;

    /**
     * 创建上下文快照。
     *
     * @param scope                 作用域名称
     * @param dataScopeIgnoreDepth  跳过数据权限过滤的嵌套层数
     * @param tenantIgnoreDepth     跳过租户过滤的嵌套层数
     * @param dataSourceTargets     数据源路由栈，栈顶在前
     */
    private JpaPlusContext(
            String scope,
            int dataScopeIgnoreDepth,
            int tenantIgnoreDepth,
            List<String> dataSourceTargets
    ) {
        this.scope = scope;
        this.dataScopeIgnoreDepth = dataScopeIgnoreDepth;
        this.tenantIgnoreDepth = tenantIgnoreDepth;
        this.dataSourceTargets = List.copyOf(dataSourceTargets);
    }

    /**
     * 捕获当前线程的上下文快照。
     *
     * @return 不可变上下文快照
     */
    public static JpaPlusContext capture() {
        return new JpaPlusContext(
                JpaPlusScope.current(),
                DataScopes.ignoreDepth(),
                Tenants.ignoreDepth(),
                DataSourceContext.snapshot()
        );
    }

    /**
     * 清除当前线程的全部 JPA Plus 上下文。
     *
     * <p>作用域、两类过滤跳过深度与数据源路由栈都会被移除，适用于
     * 线程池任务结束后的兜底清理。</p>
     */
    public static void clear() {
        JpaPlusScope.clear();
        clearDepth(DataScopes::ignoreDepth, DataScopes::restore);
        clearDepth(Tenants::ignoreDepth, Tenants::restore);
        DataSourceContext.clear();
    }

    /**
     * 把快照应用到当前线程。
     *
     * <p>应用后当前线程的上下文与快照完全一致：快照中没有作用域时
     * 清除作用域，跳过深度按快照增减，数据源路由栈先清空再按原顺序
     * 重建。</p>
     */
    public void apply() {
        if (scope == null) {
            JpaPlusScope.clear();
        } else {
            JpaPlusScope.set(scope);
        }

        applyDepth(
                dataScopeIgnoreDepth,
                DataScopes.ignoreDepth(),
                DataScopes::ignore,
                DataScopes::restore
        );
        applyDepth(
                tenantIgnoreDepth,
                Tenants.ignoreDepth(),
                Tenants::ignore,
                Tenants::restore
        );

        DataSourceContext.clear();

        for (int index = dataSourceTargets.size() - 1; index >= 0; index--) {
            DataSourceContext.push(dataSourceTargets.get(index));
        }
    }

    /**
     * 在快照上下文中执行无返回值逻辑。
     *
     * <p>执行结束后恢复执行线程原有的上下文，异常同样会恢复。</p>
     *
     * @param action 执行逻辑，不能为 {@code null}
     * @throws IllegalArgumentException 执行逻辑为空时抛出
     */
    public void run(Runnable action) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "Context action must not be null"
            );
        }

        call(() -> {
            action.run();
            return null;
        });
    }

    /**
     * 在快照上下文中执行有返回值逻辑。
     *
     * <p>执行结束后恢复执行线程原有的上下文，异常同样会恢复。</p>
     *
     * @param action 执行逻辑，不能为 {@code null}
     * @param <T>    返回值类型
     * @return 执行结果
     * @throws IllegalArgumentException 执行逻辑为空时抛出
     */
    public <T> T call(Supplier<T> action) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "Context action must not be null"
            );
        }

        JpaPlusContext previous = capture();
        apply();

        try {
            return action.get();
        } finally {
            previous.apply();
        }
    }

    /**
     * 获取作用域名称。
     *
     * @return 作用域名称；快照未绑定作用域时返回 {@code null}
     */
    public String getScope() {
        return scope;
    }

    /**
     * 获取跳过数据权限过滤的嵌套层数。
     *
     * @return 嵌套层数，未跳过时返回 0
     */
    public int getDataScopeIgnoreDepth() {
        return dataScopeIgnoreDepth;
    }

    /**
     * 获取跳过租户过滤的嵌套层数。
     *
     * @return 嵌套层数，未跳过时返回 0
     */
    public int getTenantIgnoreDepth() {
        return tenantIgnoreDepth;
    }

    /**
     * 获取数据源路由栈。
     *
     * @return 栈顶在前的只读列表；未设置时返回空列表
     */
    public List<String> getDataSourceTargets() {
        return dataSourceTargets;
    }

    /**
     * 把当前跳过深度调整到目标值。
     *
     * @param target  目标层数
     * @param current 当前层数
     * @param ignore  增加一层跳过
     * @param restore 减少一层跳过
     */
    private static void applyDepth(
            int target,
            int current,
            Runnable ignore,
            Runnable restore
    ) {
        int depth = current;

        while (depth < target) {
            ignore.run();
            depth++;
        }

        while (depth > target) {
            restore.run();
            depth--;
        }
    }

    /**
     * 清除指定过滤的跳过深度。
     *
     * @param current 读取当前层数
     * @param restore 减少一层跳过
     */
    private static void clearDepth(
            IntSupplier current,
            Runnable restore
    ) {
        while (current.getAsInt() > 0) {
            restore.run();
        }
    }
}
