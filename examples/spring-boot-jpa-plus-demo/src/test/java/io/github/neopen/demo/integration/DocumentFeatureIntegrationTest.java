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
package io.github.neopen.demo.integration;

import io.github.neopen.core.persistence.audit.trail.AuditAction;
import io.github.neopen.core.persistence.audit.trail.AuditEntry;
import io.github.neopen.demo.bean.entity.DocumentRecord;
import io.github.neopen.demo.service.DocumentFeatureService;
import io.github.neopen.demo.support.DemoAuditStore;
import io.github.neopen.demo.support.DemoContext;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 0.6 新增能力示例集成测试。
 *
 * <p>覆盖多租户隔离、行级数据权限、变更历史、逻辑删除恢复与
 * 批量 upsert。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class DocumentFeatureIntegrationTest {

    /** 文档能力示例服务。 */
    @Autowired
    private DocumentFeatureService documentFeatureService;

    /** 实体管理器。 */
    @Autowired
    private EntityManager entityManager;

    /**
     * 清理演示上下文与变更历史。
     */
    @BeforeEach
    void setUp() {
        DemoContext.clear();
        DemoAuditStore.clear();
    }

    /**
     * 清理线程上下文。
     */
    @AfterEach
    void tearDown() {
        DemoContext.clear();
    }

    /**
     * 验证不同租户的数据互相不可见。
     */
    @Test
    void shouldIsolateDataByTenant() {
        DemoContext.setTenant("tenant-a");
        documentFeatureService.create("DOC-A-1", "租户 A 文档", 1L);

        DemoContext.setTenant("tenant-b");
        documentFeatureService.create("DOC-B-1", "租户 B 文档", 1L);

        DemoContext.setTenant("tenant-a");
        List<DocumentRecord> documents = documentFeatureService.list();

        assertThat(documents)
                .extracting(DocumentRecord::getTenantId)
                .containsOnly("tenant-a");
    }

    /**
     * 验证行级数据权限按部门过滤。
     */
    @Test
    void shouldFilterByDepartmentPermission() {
        DemoContext.setTenant("tenant-dept");
        documentFeatureService.create("DOC-D-1", "部门 10 文档", 10L);
        documentFeatureService.create("DOC-D-2", "部门 20 文档", 20L);

        DemoContext.setDeptId(10L);

        assertThat(documentFeatureService.list())
                .extracting(DocumentRecord::getDocNo)
                .containsExactly("DOC-D-1");
    }

    /**
     * 验证逻辑删除后可以恢复。
     */
    @Test
    void shouldRestoreLogicallyDeletedDocument() {
        DemoContext.setTenant("tenant-restore");
        DocumentRecord document = documentFeatureService.create(
                "DOC-R-1",
                "待恢复文档",
                1L
        );

        documentFeatureService.remove(document.getId());
        assertThat(documentFeatureService.list()).isEmpty();

        assertThat(documentFeatureService.restore(document.getId()))
                .isEqualTo(1);
        assertThat(documentFeatureService.list())
                .extracting(DocumentRecord::getDocNo)
                .containsExactly("DOC-R-1");
    }

    /**
     * 验证新增与修改会生成变更历史。
     */
    @Test
    void shouldRecordAuditTrail() {
        DemoContext.setTenant("tenant-audit");
        DocumentRecord document = documentFeatureService.create(
                "DOC-AU-1",
                "原标题",
                1L
        );

        documentFeatureService.rename(document.getId(), "新标题");

        List<AuditEntry> entries = DemoAuditStore.snapshot();

        assertThat(entries)
                .extracting(AuditEntry::action)
                .contains(AuditAction.CREATE, AuditAction.UPDATE);
        assertThat(entries.get(1).changes())
                .anySatisfy(change -> {
                    assertThat(change.field()).isEqualTo("title");
                    assertThat(change.before()).isEqualTo("原标题");
                    assertThat(change.after()).isEqualTo("新标题");
                });

        DocumentRecord managed = entityManager.find(
                DocumentRecord.class,
                document.getId()
        );
        entityManager.remove(managed);
        entityManager.flush();

        assertThat(DemoAuditStore.snapshot())
                .extracting(AuditEntry::action)
                .contains(AuditAction.DELETE);
    }

    /**
     * 验证按文档编号批量 upsert。
     */
    @Test
    void shouldUpsertByDocNo() {
        DemoContext.setTenant("tenant-upsert");
        documentFeatureService.create("DOC-UP-1", "旧标题", 1L);

        DocumentRecord existing = new DocumentRecord();
        existing.setDocNo("DOC-UP-1");
        existing.setTitle("新标题");
        existing.setDeptId(1L);

        DocumentRecord created = new DocumentRecord();
        created.setDocNo("DOC-UP-2");
        created.setTitle("新增标题");
        created.setDeptId(1L);

        var result = documentFeatureService.upsert(
                List.of(existing, created)
        );

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(documentFeatureService.list())
                .extracting(DocumentRecord::getDocNo)
                .containsExactlyInAnyOrder("DOC-UP-1", "DOC-UP-2");
        assertThat(documentFeatureService.list())
                .filteredOn(document -> "DOC-UP-1".equals(document.getDocNo()))
                .extracting(DocumentRecord::getTitle)
                .containsExactly("新标题");
    }
}
