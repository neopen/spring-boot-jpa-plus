/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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
