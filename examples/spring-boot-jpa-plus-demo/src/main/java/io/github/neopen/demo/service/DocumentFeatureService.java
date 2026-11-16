package io.github.neopen.demo.service;

import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import io.github.neopen.core.persistence.tenant.Tenants;
import io.github.neopen.demo.bean.entity.DocumentRecord;
import io.github.neopen.demo.persistence.repo.DocumentRecordRepo;
import io.github.neopen.demo.support.DemoAuditStore;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档能力演示服务。
 *
 * <p>覆盖多租户过滤、行级数据权限、变更历史、逻辑删除恢复与
 * 批量 upsert。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Service
public class DocumentFeatureService {

    /** 实体管理器。 */
    private final EntityManager entityManager;

    /** 文档仓库。 */
    private final DocumentRecordRepo documentRecordRepo;

    /**
     * 创建服务。
     *
     * @param entityManager       实体管理器
     * @param documentRecordRepo  文档仓库
     */
    public DocumentFeatureService(
            EntityManager entityManager,
            DocumentRecordRepo documentRecordRepo
    ) {
        this.entityManager = entityManager;
        this.documentRecordRepo = documentRecordRepo;
    }

    /**
     * 新增文档，租户由监听器自动填充。
     *
     * @param docNo  文档编号
     * @param title  标题
     * @param deptId 部门标识
     * @return 新增的文档
     */
    @Transactional
    public DocumentRecord create(String docNo, String title, Long deptId) {
        DocumentRecord document = new DocumentRecord();
        document.setDocNo(docNo);
        document.setTitle(title);
        document.setDeptId(deptId);

        entityManager.persist(document);
        entityManager.flush();

        return document;
    }

    /**
     * 查询当前租户与数据权限范围内的文档。
     *
     * @return 文档列表
     */
    @Transactional(readOnly = true)
    public List<DocumentRecord> list() {
        return JpaPlusSpecExecutor.of(entityManager, DocumentRecord.class)
                .findAll(null);
    }

    /**
     * 修改标题，触发变更历史。
     *
     * @param id    主键
     * @param title 新标题
     * @return 修改后的文档
     */
    @Transactional
    public DocumentRecord rename(Long id, String title) {
        DocumentRecord document = entityManager.find(
                DocumentRecord.class,
                id
        );

        if (document == null) {
            throw new IllegalArgumentException(
                    "Document not found: " + id
            );
        }

        document.setTitle(title);
        entityManager.flush();

        return document;
    }

    /**
     * 逻辑删除文档。
     *
     * @param id 主键
     * @return 受影响记录数
     */
    @Transactional
    public int remove(Long id) {
        int affected = LogicDeletes.deleteById(
                entityManager,
                DocumentRecord.class,
                id
        );
        entityManager.flush();

        return affected;
    }

    /**
     * 恢复逻辑删除的文档。
     *
     * @param id 主键
     * @return 受影响记录数
     */
    @Transactional
    public int restore(Long id) {
        return LogicDeletes.restoreById(
                entityManager,
                DocumentRecord.class,
                id
        );
    }

    /**
     * 按文档编号批量新增或更新。
     *
     * @param documents 文档列表
     * @return 新增与更新数量
     */
    @Transactional
    public BatchOperation.UpsertResult upsert(
            List<DocumentRecord> documents
    ) {
        String tenant = Tenants.currentTenant();

        for (DocumentRecord document : documents) {
            if (document.getTenantId() == null) {
                document.setTenantId(tenant);
            }

            if (document.getDeleted() == null) {
                document.setDeleted(Boolean.FALSE);
            }
        }

        return BatchOperation.upsertAll(
                entityManager,
                DocumentRecord.class,
                documents,
                DocumentRecord::getDocNo,
                "docNo",
                50
        );
    }

    /**
     * 获取变更历史。
     *
     * @return 变更历史列表
     */
    public List<io.github.neopen.core.persistence.audit.trail.AuditEntry>
    auditEntries() {
        return DemoAuditStore.snapshot();
    }

    /**
     * 统计文档数量。
     *
     * @return 文档数量
     */
    public long count() {
        return documentRecordRepo.count();
    }
}
