package io.github.neopen.demo.support;

import io.github.neopen.core.persistence.audit.trail.AuditEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 演示用的变更历史存储。
 *
 * <p>真实项目应写入审计表或消息队列，这里保存在内存中便于接口
 * 与测试查看。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class DemoAuditStore {

    /** 收集到的变更历史。 */
    private static final List<AuditEntry> ENTRIES =
            new CopyOnWriteArrayList<>();

    /** 工具类不允许实例化。 */
    private DemoAuditStore() {
    }

    /**
     * 添加变更历史。
     *
     * @param entry 变更历史
     */
    public static void add(AuditEntry entry) {
        ENTRIES.add(entry);
    }

    /**
     * 获取当前变更历史快照。
     *
     * @return 变更历史列表
     */
    public static List<AuditEntry> snapshot() {
        return new ArrayList<>(ENTRIES);
    }

    /**
     * 清理变更历史。
     */
    public static void clear() {
        ENTRIES.clear();
    }
}
