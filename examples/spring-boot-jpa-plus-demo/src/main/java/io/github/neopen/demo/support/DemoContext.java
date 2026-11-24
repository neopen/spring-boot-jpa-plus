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
