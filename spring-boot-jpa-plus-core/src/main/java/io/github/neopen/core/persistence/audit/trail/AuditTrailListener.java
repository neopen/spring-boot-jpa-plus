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
package io.github.neopen.core.persistence.audit.trail;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * 变更历史监听器。
 *
 * <p>在实体上声明
 * {@code @EntityListeners(AuditTrailListener.class)} 并标注
 * {@link Audited} 后，自动记录新增、修改和删除。</p>
 *
 * <p>修改记录依赖加载时的快照，因此通过 {@code merge} 合并的游离
 * 实体可能只记录当前值；需要精确的前后对比时，请先查询实体再修改。</p>
 *
 * @author neopen
 * @see AuditTrails
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class AuditTrailListener {

    /**
     * 创建默认实例。
     */
    public AuditTrailListener() {
    }

    /**
     * 加载后记录快照。
     *
     * @param entity 实体
     */
    @PostLoad
    public void postLoad(Object entity) {
        AuditTrails.capture(entity);
    }

    /**
     * 新增后发布记录。
     *
     * @param entity 实体
     */
    @PostPersist
    public void postPersist(Object entity) {
        AuditTrails.publishCreate(entity);
    }

    /**
     * 修改前对比快照并发布记录。
     *
     * @param entity 实体
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        AuditTrails.publishUpdate(entity);
    }

    /**
     * 删除前记录快照。
     *
     * @param entity 实体
     */
    @PreRemove
    public void preRemove(Object entity) {
        AuditTrails.captureForDelete(entity);
    }

    /**
     * 删除后发布记录。
     *
     * @param entity 实体
     */
    @PostRemove
    public void postRemove(Object entity) {
        AuditTrails.publishDelete(entity);
    }
}
