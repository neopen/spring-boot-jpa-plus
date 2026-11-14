package io.github.neopen.core.persistence.audit;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * 审计字段监听器。
 *
 * <p>在实体上通过 {@code @EntityListeners(AuditListener.class)} 注册，
 * 插入和更新时自动填充审计字段。当前操作人由
 * {@link Audits#setAuditorSupplier(java.util.function.Supplier)} 提供。</p>
 *
 * <pre>{@code
 * @Entity
 * @EntityListeners(AuditListener.class)
 * public class Order {
 *
 *     @CreatedAt
 *     private Instant createdAt;
 *
 *     @UpdatedAt
 *     private Instant updatedAt;
 * }
 * }</pre>
 */
public class AuditListener {

    /**
     * 插入前填充审计字段。
     *
     * @param entity 实体
     */
    @PrePersist
    public void prePersist(Object entity) {
        Audits.applyCreate(entity);
    }

    /**
     * 更新前填充审计字段。
     *
     * @param entity 实体
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        Audits.applyUpdate(entity);
    }
}
