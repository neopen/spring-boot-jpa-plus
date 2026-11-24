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
