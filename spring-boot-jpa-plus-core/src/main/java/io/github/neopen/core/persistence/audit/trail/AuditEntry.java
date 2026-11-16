package io.github.neopen.core.persistence.audit.trail;

import java.time.Instant;
import java.util.List;

/**
 * 一条变更历史记录。
 *
 * @param entityName 实体名称（简单类名）
 * @param entityType 实体类型
 * @param entityId   实体主键
 * @param action     变更类型
 * @param changes    字段变更明细
 * @param actor      操作人
 * @param occurredAt 发生时间
 * @author neopen
 * @see AuditTrails
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record AuditEntry(
        String entityName,
        Class<?> entityType,
        Object entityId,
        AuditAction action,
        List<AuditChange> changes,
        String actor,
        Instant occurredAt
) {

    /**
     * 校验并复制变更明细。
     */
    public AuditEntry {
        changes = changes == null ? List.of() : List.copyOf(changes);
    }
}
