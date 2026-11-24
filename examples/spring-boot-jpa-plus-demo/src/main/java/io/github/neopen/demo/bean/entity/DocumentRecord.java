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
package io.github.neopen.demo.bean.entity;

import io.github.neopen.core.persistence.audit.trail.Audited;
import io.github.neopen.core.persistence.audit.trail.AuditTrailListener;
import io.github.neopen.core.persistence.logicdelete.LogicDelete;
import io.github.neopen.core.persistence.tenant.TenantId;
import io.github.neopen.core.persistence.tenant.TenantListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档记录实体。
 *
 * <p>演示 0.6 的多租户、行级数据权限、变更历史与逻辑删除恢复能力。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@NoArgsConstructor
@Entity
@Audited
@EntityListeners({AuditTrailListener.class, TenantListener.class})
@Table(name = "t_document_record")
public class DocumentRecord {

    /** 主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 租户标识，插入时由监听器自动填充。 */
    @TenantId
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    /** 文档编号。 */
    @Column(name = "doc_no", nullable = false, unique = true, length = 32)
    private String docNo;

    /** 文档标题。 */
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /** 部门标识，用于演示行级数据权限。 */
    @Column(name = "dept_id")
    private Long deptId;

    /** 逻辑删除标记。 */
    @LogicDelete
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;
}
