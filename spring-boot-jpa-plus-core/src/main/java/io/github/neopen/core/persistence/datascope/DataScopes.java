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
package io.github.neopen.core.persistence.datascope;

import io.github.neopen.core.scope.JpaPlusScope;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 行级数据权限工具。
 *
 * <p>把 {@link DataPermissionProvider} 返回的规则转换成 JPA
 * 查询条件，与实体已有的查询条件按 {@code AND} 合并。</p>
 *
 * <p>默认启用，但只有在注册了 {@link DataPermissionProvider}
 * 且返回非空规则时才生效。</p>
 *
 * @author neopen
 * @see DataRule
 * @see DataPermissionProvider
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class DataScopes {

    /** 是否启用数据权限，默认启用。 */
    private static volatile boolean enabled = true;

    /** 当前数据权限提供者。 */
    private static volatile DataPermissionProvider provider;

    /** 按数据源作用域配置的数据权限开关。 */
    private static final Map<String, Boolean> scopeEnabled =
            new ConcurrentHashMap<>();

    /** 按数据源作用域配置的数据权限提供者。 */
    private static final Map<String, DataPermissionProvider> scopeProviders =
            new ConcurrentHashMap<>();

    /** 工具类不允许实例化。 */
    private DataScopes() {
    }

    /**
     * 全局启用或关闭数据权限。
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        DataScopes.enabled = enabled;
    }

    /**
     * 为指定作用域设置数据权限开关。
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
     * 判断全局数据权限是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断指定作用域的数据权限是否启用。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled(String scope) {
        return activeEnabled(scope);
    }

    /**
     * 设置数据权限提供者。
     *
     * @param provider 提供者；传 {@code null} 时不限制数据范围
     */
    public static void setProvider(DataPermissionProvider provider) {
        DataScopes.provider = provider;
    }

    /**
     * 为指定作用域设置数据权限提供者。
     *
     * @param scope    作用域名称，等于默认作用域时等同于全局设置
     * @param provider 提供者；传 {@code null} 时该作用域不限制数据范围
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setProvider(
            String scope,
            DataPermissionProvider provider
    ) {
        if (JpaPlusScope.isDefault(scope)) {
            setProvider(provider);
            return;
        }

        scopeProviders.put(JpaPlusScope.requireName(scope), provider);
    }

    /**
     * 清除指定作用域的数据权限配置。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearScope(String scope) {
        String name = JpaPlusScope.requireName(scope);

        scopeEnabled.remove(name);
        scopeProviders.remove(name);
    }

    /**
     * 判断当前是否有生效的数据权限规则。
     *
     * @return 有提供者时返回 {@code true}
     */
    public static boolean isActive() {
        return activeEnabled(JpaPlusScope.currentScopeName())
                && activeProvider(JpaPlusScope.currentScopeName()) != null;
    }

    /**
     * 为实体追加行级数据权限条件。
     *
     * @param <T>        实体类型
     * @param entityType 实体类型
     * @return 数据权限条件
     */
    public static <T> Specification<T> filter(Class<?> entityType) {
        return filter(entityType, null);
    }

    /**
     * 为查询条件追加行级数据权限。
     *
     * @param <T>           实体类型
     * @param entityType    实体类型
     * @param specification 原查询条件，允许为 {@code null}
     * @return 追加数据权限后的查询条件
     */
    public static <T> Specification<T> filter(
            Class<?> entityType,
            Specification<T> specification
    ) {
        String scope = JpaPlusScope.currentScopeName();

        if (!activeEnabled(scope)) {
            return specification;
        }

        DataPermissionProvider active = activeProvider(scope);

        if (active == null) {
            return specification;
        }

        List<DataRule> rules = active.rulesFor(entityType);

        if (rules == null || rules.isEmpty()) {
            return specification;
        }

        Specification<T> permission = (root, query, cb) -> {
            var predicates = rules.stream()
                    .map(rule -> root.get(rule.field()).in(rule.values()))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new);

            return cb.and(predicates);
        };

        return specification == null
                ? permission
                : specification.and(permission);
    }

    /**
     * 解析指定作用域生效的数据权限开关。
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
     * 解析指定作用域生效的数据权限提供者。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 数据权限提供者
     */
    private static DataPermissionProvider activeProvider(String scope) {
        if (scope == null) {
            return provider;
        }

        DataPermissionProvider scoped = scopeProviders.get(scope);

        return scoped == null ? provider : scoped;
    }
}
