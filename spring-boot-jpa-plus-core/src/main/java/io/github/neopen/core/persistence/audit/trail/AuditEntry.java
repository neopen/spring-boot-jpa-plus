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
