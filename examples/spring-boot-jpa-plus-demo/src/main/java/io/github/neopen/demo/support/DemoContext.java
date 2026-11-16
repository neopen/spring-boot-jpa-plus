package io.github.neopen.demo.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示用的租户与部门上下文。
 *
 * <p>真实项目通常从登录信息或请求头中解析，这里用 ThreadLocal
 * 简化示例。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class DemoContext {

    /** 默认租户。 */
    public static final String DEFAULT_TENANT = "demo-tenant";

    /** 当前线程的上下文。 */
    private static final ThreadLocal<Map<String, Object>> HOLDER =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    /** 工具类不允许实例化。 */
    private DemoContext() {
    }

    /**
     * 获取当前租户。
     *
     * @return 当前租户
     */
    public static String tenant() {
        return (String) HOLDER.get().getOrDefault(
                "tenant",
                DEFAULT_TENANT
        );
    }

    /**
     * 设置当前租户。
     *
     * @param tenant 租户标识
     */
    public static void setTenant(String tenant) {
        HOLDER.get().put("tenant", tenant);
    }

    /**
     * 获取当前部门。
     *
     * @return 部门标识；未设置时返回 {@code null}
     */
    public static Long deptId() {
        return (Long) HOLDER.get().get("deptId");
    }

    /**
     * 设置当前部门。
     *
     * @param deptId 部门标识
     */
    public static void setDeptId(Long deptId) {
        HOLDER.get().put("deptId", deptId);
    }

    /**
     * 清理当前线程上下文。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
