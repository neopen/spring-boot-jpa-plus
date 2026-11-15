package io.github.neopen.core.scope;

import java.util.function.Supplier;

/**
 * 数据源作用域上下文。
 *
 * <p>逻辑删除过滤、审计、字段加密和批量操作等能力以静态开关提供，
 * 在单数据源应用中直接使用全局默认值即可。多数据源应用可以按
 * 数据源名称注册独立的开关，并在访问对应数据源时绑定作用域：</p>
 *
 * <pre>{@code
 * JpaPlusScope.run("secondary", () -> {
 *     // 这里的逻辑删除过滤、审计与加密配置都来自 secondary
 *     secondaryRepository.findAll(spec);
 * });
 * }</pre>
 *
 * <p>作用域绑定在线程上，不会影响其他线程；嵌套调用结束后自动恢复
 * 外层作用域。未绑定作用域或作用域未配置对应开关时，各能力回退到
 * 全局默认值。</p>
 *
 * <p>Spring Boot 中通常配合 {@code neopen.jpa.scopes} 配置使用，
 * 自动配置会在启动时按名称注册开关。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class JpaPlusScope {

    /** 默认作用域名称，表示使用全局开关。 */
    public static final String DEFAULT = "default";

    /** 当前线程绑定的作用域。 */
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    /** 工具类不允许实例化。 */
    private JpaPlusScope() {
    }

    /**
     * 获取当前线程绑定的作用域。
     *
     * @return 作用域名称；未绑定时返回 {@code null}
     */
    public static String current() {
        return CURRENT.get();
    }

    /**
     * 绑定当前线程的作用域。
     *
     * <p>只应在明确的作用域边界内成对调用 {@link #set(String)} 与
     * {@link #clear()}，或直接使用 {@link #run(String, Runnable)}、
     * {@link #call(String, Supplier)}。</p>
     *
     * @param name 作用域名称，不能为空白
     * @throws IllegalArgumentException 名称为空时抛出
     */
    public static void set(String name) {
        CURRENT.set(requireName(name));
    }

    /**
     * 清除当前线程绑定的作用域。
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * 在指定作用域内执行无返回值逻辑。
     *
     * @param name   作用域名称
     * @param action 执行逻辑，不能为 {@code null}
     * @throws IllegalArgumentException 名称或执行逻辑为空时抛出
     */
    public static void run(String name, Runnable action) {
        call(name, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 在指定作用域内执行有返回值逻辑。
     *
     * <p>执行结束后恢复之前的作用域，异常同样会恢复。</p>
     *
     * @param name   作用域名称
     * @param action 执行逻辑，不能为 {@code null}
     * @param <T>    返回值类型
     * @return 执行结果
     * @throws IllegalArgumentException 名称或执行逻辑为空时抛出
     */
    public static <T> T call(String name, Supplier<T> action) {
        String scope = requireName(name);

        if (action == null) {
            throw new IllegalArgumentException(
                    "Scoped action must not be null"
            );
        }

        String previous = CURRENT.get();
        CURRENT.set(scope);

        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    /**
     * 校验并规范化作用域名称。
     *
     * @param name 作用域名称
     * @return 去除首尾空白后的名称
     * @throws IllegalArgumentException 名称为空时抛出
     */
    public static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Scope name must not be blank"
            );
        }

        return name.trim();
    }

    /**
     * 判断作用域是否表示全局默认配置。
     *
     * @param name 作用域名称，允许为 {@code null}
     * @return 未指定或等同于默认作用域时返回 {@code true}
     */
    public static boolean isDefault(String name) {
        return name == null || DEFAULT.equals(name);
    }

    /**
     * 获取用于查询按作用域存储配置的名称。
     *
     * @return 非默认作用域名称；未绑定或默认作用域时返回 {@code null}
     */
    public static String currentScopeName() {
        String scope = CURRENT.get();

        return isDefault(scope) ? null : scope;
    }
}
