package io.github.neopen.core.persistence.tenant;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * 租户字段填充监听器。
 *
 * <p>在实体上声明
 * {@code @EntityListeners(TenantListener.class)} 后，插入和更新时
 * 自动写入当前租户。</p>
 *
 * @author neopen
 * @see TenantId
 * @see Tenants
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class TenantListener {

    /**
     * 创建默认实例。
     */
    public TenantListener() {
    }

    /**
     * 插入前填充租户字段。
     *
     * @param entity 实体
     */
    @PrePersist
    public void prePersist(Object entity) {
        Tenants.applyTenant(entity);
    }

    /**
     * 更新前补齐缺失的租户字段。
     *
     * @param entity 实体
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        Tenants.applyTenant(entity);
    }
}
