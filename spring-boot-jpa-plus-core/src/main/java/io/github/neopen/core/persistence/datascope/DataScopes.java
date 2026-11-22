package io.github.neopen.core.persistence.datascope;

import io.github.neopen.core.scope.JpaPlusScope;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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

    /** 当前线程跳过数据权限过滤的嵌套层数。 */
    private static final ThreadLocal<Integer> IGNORE_DEPTH =
            new ThreadLocal<>();

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
     * 判断当前线程是否跳过数据权限过滤。
     *
     * @return 跳过时返回 {@code true}
     */
    public static boolean isIgnored() {
        Integer depth = IGNORE_DEPTH.get();

        return depth != null && depth > 0;
    }

    /**
     * 开始跳过数据权限过滤。
     *
     * <p>支持嵌套调用，必须与 {@link #restore()} 成对使用；业务代码
     * 优先使用 {@link #runIgnored(Runnable)} 或
     * {@link #callIgnored(Supplier)}，避免异常时漏掉恢复。</p>
     */
    public static void ignore() {
        IGNORE_DEPTH.set(ignoreDepth() + 1);
    }

    /**
     * 结束一层数据权限过滤跳过。
     *
     * @throws IllegalStateException 当前没有跳过数据权限过滤时抛出
     */
    public static void restore() {
        int depth = ignoreDepth();

        if (depth <= 0) {
            throw new IllegalStateException(
                    "Data scope filter is not ignored"
            );
        }

        if (depth == 1) {
            IGNORE_DEPTH.remove();
        } else {
            IGNORE_DEPTH.set(depth - 1);
        }
    }

    /**
     * 在跳过数据权限过滤的上下文中执行逻辑。
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
     * 在跳过数据权限过滤的上下文中执行有返回值逻辑。
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
     * 判断当前是否有生效的数据权限规则。
     *
     * @return 有提供者时返回 {@code true}
     */
    public static boolean isActive() {
        return !isIgnored()
                && activeEnabled(JpaPlusScope.currentScopeName())
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

        if (isIgnored() || !activeEnabled(scope)) {
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
            Predicate combined = null;

            for (DataRule rule : rules) {
                Predicate current = path(root, rule.field())
                        .in(rule.values());

                if (combined == null) {
                    combined = current;
                } else if (rule.logic() == DataRule.Logic.OR) {
                    combined = cb.or(combined, current);
                } else {
                    combined = cb.and(combined, current);
                }
            }

            return combined;
        };

        return specification == null
                ? permission
                : specification.and(permission);
    }

    /**
     * 解析实体字段路径，支持 {@code a.b} 形式的关联属性。
     *
     * @param root  查询根
     * @param field 字段路径
     * @return 字段路径
     */
    private static Path<?> path(Root<?> root, String field) {
        Path<?> path = root;

        for (String segment : field.split("\\.")) {
            path = path.get(segment);
        }

        return path;
    }

    /**
     * 获取当前线程跳过数据权限过滤的嵌套层数。
     *
     * <p>同时作为 {@link io.github.neopen.core.context.JpaPlusContext}
     * 捕获与恢复上下文时的读取入口。</p>
     *
     * @return 嵌套层数，未跳过时返回 0
     */
    public static int ignoreDepth() {
        Integer depth = IGNORE_DEPTH.get();

        return depth == null ? 0 : depth;
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
