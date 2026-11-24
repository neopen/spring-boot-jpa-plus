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
     * 获取当前租户。
     *
     * @return 当前租户；未启用或没有租户上下文时返回 {@code null}
     */
    public static String currentTenant() {
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
