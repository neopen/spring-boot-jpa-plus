package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.audit.AuditListener;
import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.persistence.audit.CreatedAt;
import io.github.neopen.core.persistence.audit.CreatedBy;
import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JpaPlusRuntimeConfigurer} 单元测试。
 */
class JpaPlusRuntimeConfigurerTest {

    /**
     * 恢复全局默认值，避免测试间互相影响。
     */
    @AfterEach
    void reset() {
        LogicDeletes.setEnabled(true);
        Audits.setEnabled(true);
        Audits.setClock(null);
        Audits.setAuditorSupplier(null);
        BatchOperation.setDefaultBatchSize(500);
    }

    /**
     * 验证批次大小写入批量工具。
     */
    @Test
    void shouldApplyBatchSize() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.setBatchSize(100);

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(BatchOperation.getDefaultBatchSize()).isEqualTo(100);
    }

    /**
     * 验证可以关闭逻辑删除过滤。
     */
    @Test
    void shouldDisableLogicDeleteFilter() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getLogicDelete().setEnabled(false);

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(LogicDeletes.isEnabled()).isFalse();
    }

    /**
     * 验证审计时区与操作人配置生效。
     */
    @Test
    void shouldApplyAuditZoneAndAuditor() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getAudit().setZone("Asia/Shanghai");
        properties.getAudit().setAuditor("system");

        new JpaPlusRuntimeConfigurer(properties);

        AuditedEntity entity = new AuditedEntity();

        new AuditListener().prePersist(entity);

        assertThat(entity.createdAt.getZone())
                .isEqualTo(ZoneId.of("Asia/Shanghai"));
        assertThat(entity.createdBy).isEqualTo("system");
    }

    /**
     * 验证关闭审计后不再填充字段。
     */
    @Test
    void shouldDisableAuditFilling() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getAudit().setEnabled(false);

        new JpaPlusRuntimeConfigurer(properties);

        AuditedEntity entity = new AuditedEntity();

        new AuditListener().prePersist(entity);

        assertThat(Audits.isEnabled()).isFalse();
        assertThat(entity.createdAt).isNull();
        assertThat(entity.createdBy).isNull();
    }

    /** 审计实体。 */
    static class AuditedEntity {

        /** 创建时间。 */
        @CreatedAt
        private ZonedDateTime createdAt;

        /** 创建人。 */
        @CreatedBy
        private String createdBy;
    }
}
