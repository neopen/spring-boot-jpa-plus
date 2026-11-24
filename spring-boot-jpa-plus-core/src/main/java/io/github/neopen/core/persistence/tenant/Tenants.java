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
package io.github.neopen.core.persistence.tenant;

import io.github.neopen.core.scope.JpaPlusScope;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 多租户工具。
 *
 * <p>维护当前租户提供者，并为查询追加租户条件。默认启用，
 * 只有实体声明了 {@link TenantId} 字段时才会过滤，未使用多租户
 * 的项目无需任何配置。</p>
 *
 * <pre>{@code
 * Tenants.setProvider(() -> TenantHolder.get());
 *
 * Specification<Order> spec = Tenants.filter(
 *         Order.class,
 *         (root, query, cb) -> cb.equal(root.get("status"), "PAID")
 * );
 * }</pre>
 *
 * <p>作用域配置与其它能力保持一致：通过 {@code neopen.jpa.scopes}
 * 可以为指定数据源作用域单独关闭租户过滤或固定租户。</p>
 *
 * @author neopen
 * @see TenantProvider
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class Tenants {

    /** 是否启用租户过滤，默认启用。 */
    private static volatile boolean enabled = true;

    /** 当前租户提供者。 */
    private static volatile TenantProvider provider;

    /** 按数据源作用域配置的租户开关。 */
    private static final Map<String, Boolean> scopeEnabled =
            new ConcurrentHashMap<>();

    /** 按数据源作用域配置的租户提供者。 */
    private static final Map<String, TenantProvider> scopeProviders =
            new ConcurrentHashMap<>();

    /** 按数据源作用域固定的租户。 */
    private static final Map<String, String> scopeTenants =
            new ConcurrentHashMap<>();

    /** 当前线程跳过租户过滤的嵌套层数。 */
    private static final ThreadLocal<Integer> IGNORE_DEPTH =
            new ThreadLocal<>();

    /** 工具类不允许实例化。 */
    private Tenants() {
    }

    /**
     * 全局启用或关闭租户过滤。
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        Tenants.enabled = enabled;
    }

    /**
     * 为指定作用域设置租户开关。
     *
     * @param scope   作用域名称，等于默认作用域时等同于全局设置
     * @param enabled 是否启用
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setEnabled(String scope, boolean enabled) {
        if (JpaPlusScope.isDefault(scope)) {
            setEnabled(enabled);
            return;
        }

        scopeEnabled.put(JpaPlusScope.requireName(scope), enabled);
    }

    /**
     * 判断全局租户过滤是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断指定作用域的租户过滤是否启用。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled(String scope) {
        return activeEnabled(scope);
    }

    /**
     * 设置当前租户提供者。
     *
     * @param provider 租户提供者；传 {@code null} 时不做租户过滤
     */
    public static void setProvider(TenantProvider provider) {
        Tenants.provider = provider;
    }

    /**
     * 为指定作用域设置租户提供者。
     *
     * @param scope    作用域名称，等于默认作用域时等同于全局设置
     * @param provider 租户提供者；传 {@code null} 时该作用域不做租户过滤
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setProvider(String scope, TenantProvider provider) {
        if (JpaPlusScope.isDefault(scope)) {
            setProvider(provider);
            return;
        }

        scopeProviders.put(JpaPlusScope.requireName(scope), provider);
    }

    /**
     * 为指定作用域固定租户。
     *
     * <p>固定租户优先于租户提供者，适用于归档、运维等需要跨租户
     * 访问单个租户数据的场景。</p>
     *
     * @param scope  作用域名称，不能为默认作用域
     * @param tenant 固定租户；传 {@code null} 时清除固定租户
     * @throws IllegalArgumentException 作用域名称为空或为默认作用域
     */
    public static void setTenant(String scope, String tenant) {
        String name = JpaPlusScope.requireName(scope);

        if (tenant == null || tenant.isBlank()) {
            scopeTenants.remove(name);
            return;
        }

        scopeTenants.put(name, tenant.trim());
    }

    /**
     * 清除指定作用域的租户配置。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearScope(String scope) {
        String name = JpaPlusScope.requireName(scope);

        scopeEnabled.remove(name);
        scopeProviders.remove(name);
        scopeTenants.remove(name);
    }

    /**
     * 判断当前线程是否跳过租户过滤。
     *
     * @return 跳过时返回 {@code true}
     */
    public static boolean isIgnored() {
        Integer depth = IGNORE_DEPTH.get();

        return depth != null && depth > 0;
    }

    /**
     * 开始跳过租户过滤。
     *
     * <p>支持嵌套调用，必须与 {@link #restore()} 成对使用；业务代码
     * 优先使用 {@link #runIgnored(Runnable)} 或
     * {@link #callIgnored(Supplier)}，避免异常时漏掉恢复。</p>
     */
    public static void ignore() {
        IGNORE_DEPTH.set(ignoreDepth() + 1);
    }

    /**
     * 结束一层租户过滤跳过。
     *
     * @throws IllegalStateException 当前没有跳过租户过滤时抛出
     */
    public static void restore() {
        int depth = ignoreDepth();

        if (depth <= 0) {
            throw new IllegalStateException(
                    "Tenant filter is not ignored"
            );
        }

        if (depth == 1) {
            IGNORE_DEPTH.remove();
        } else {
            IGNORE_DEPTH.set(depth - 1);
        }
    }

    /**
     * 在跳过租户过滤的上下文中执行逻辑。
     *
     * @param action 执行逻辑，不能为 {@code null}
     * @throws IllegalArgumentException 执行逻辑为空时抛出
     */
    public static void runIgnored(Runnable action) {
        callIgnored(() -> {
            action.run();
            return null;
        });
    }

    /**
     * 在跳过租户过滤的上下文中执行有返回值逻辑。
     *
     * <p>执行结束后恢复之前的过滤状态，异常同样会恢复。</p>
     *
     * @param <T>    返回值类型
     * @param action 执行逻辑，不能为 {@code null}
     * @return 执行结果
     * @throws IllegalArgumentException 执行逻辑为空时抛出
     */
    public static <T> T callIgnored(Supplier<T> action) {
        if (action == null) {
            throw new IllegalArgumentException(
                    "Ignored action must not be null"
            );
        }

        ignore();

        try {
            return action.get();
        } finally {
            restore();
        }
    }

    /**
     * 获取当前租户。
     *
     * @return 当前租户；未启用、被忽略或没有租户上下文时返回
     *         {@code null}
     */
    public static String currentTenant() {
        if (isIgnored()) {
            return null;
        }

        if (!activeEnabled(JpaPlusScope.currentScopeName())) {
            return null;
        }

        String scope = JpaPlusScope.currentScopeName();

        if (scope != null) {
            String fixed = scopeTenants.get(scope);

            if (fixed != null) {
                return fixed;
            }
        }

        TenantProvider active = activeProvider(scope);

        if (active == null) {
            return null;
        }

        String tenant = active.currentTenant();

        return tenant == null || tenant.isBlank()
                ? null
                : tenant.trim();
    }

    /**
     * 判断实体是否声明了租户字段。
     *
     * @param entityType 实体类型
     * @return 是否声明租户字段
     */
    public static boolean isTenantEntity(Class<?> entityType) {
        return entityType != null
                && TenantMetadata.find(entityType).isPresent();
    }

    /**
     * 为实体追加租户过滤条件。
     *
     * @param <T>        实体类型
     * @param entityType 实体类型
     * @return 租户过滤条件
     */
    public static <T> Specification<T> filter(Class<?> entityType) {
        return filter(entityType, null);
    }

    /**
     * 为查询条件追加租户过滤。
     *
     * @param <T>          实体类型
     * @param entityType   实体类型
     * @param specification 原查询条件，允许为 {@code null}
     * @return 追加租户条件后的查询条件
     */
    public static <T> Specification<T> filter(
            Class<?> entityType,
            Specification<T> specification
    ) {
        String tenant = currentTenant();

        if (tenant == null) {
            return specification;
        }

        Optional<TenantMetadata> metadata = TenantMetadata.find(entityType);

        if (metadata.isEmpty()) {
            return specification;
        }

        TenantMetadata tenantMetadata = metadata.get();
        Object value = tenantMetadata.convert(tenant);

        Specification<T> tenantSpecification = (root, query, cb) ->
                cb.equal(root.get(tenantMetadata.fieldName()), value);

        return specification == null
                ? tenantSpecification
                : specification.and(tenantSpecification);
    }

    /**
     * 为实体填充租户字段。
     *
     * <p>仅在字段为空时写入，避免覆盖已有租户。</p>
     *
     * @param entity 实体
     */
    static void applyTenant(Object entity) {
        if (entity == null) {
            return;
        }

        String tenant = currentTenant();

        if (tenant == null) {
            return;
        }

        TenantMetadata.find(entity.getClass()).ifPresent(metadata -> {
            try {
                if (metadata.field().get(entity) != null) {
                    return;
                }

                metadata.field().set(entity, metadata.convert(tenant));
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(
                        "Cannot write tenant field "
                                + entity.getClass().getName()
                                + "#" + metadata.fieldName(),
                        e
                );
            }
        });
    }

    /**
     * 获取当前线程跳过租户过滤的嵌套层数。
     *
     * @return 嵌套层数，未跳过时返回 0
     */
    private static int ignoreDepth() {
        Integer depth = IGNORE_DEPTH.get();

        return depth == null ? 0 : depth;
    }

    /**
     * 解析指定作用域生效的租户开关。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    private static boolean activeEnabled(String scope) {
        if (scope == null) {
            return enabled;
        }

        Boolean scoped = scopeEnabled.get(scope);

        return scoped == null ? enabled : scoped;
    }

    /**
     * 解析指定作用域生效的租户提供者。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 租户提供者
     */
    private static TenantProvider activeProvider(String scope) {
        if (scope == null) {
            return provider;
        }

        TenantProvider scoped = scopeProviders.get(scope);

        return scoped == null ? provider : scoped;
    }
}
