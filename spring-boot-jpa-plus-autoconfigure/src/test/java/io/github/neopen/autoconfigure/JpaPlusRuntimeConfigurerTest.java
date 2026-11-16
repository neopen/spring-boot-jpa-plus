package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.audit.AuditListener;
import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.persistence.audit.CreatedAt;
import io.github.neopen.core.persistence.audit.CreatedBy;
import io.github.neopen.core.persistence.audit.trail.AuditTrails;
import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.encrypt.EncryptedFields;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import io.github.neopen.core.persistence.tenant.Tenants;
import io.github.neopen.core.scope.JpaPlusScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

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
        EncryptedFields.setEnabled(true);
        EncryptedFields.clearKey();
        LogicDeletes.clearScope("secondary");
        Audits.clearScope("secondary");
        BatchOperation.clearScope("secondary");
        EncryptedFields.clearScope("secondary");
        Tenants.setEnabled(true);
        Tenants.setProvider(null);
        Tenants.clearScope("secondary");
        DataScopes.setEnabled(true);
        DataScopes.setProvider(null);
        DataScopes.clearScope("secondary");
        AuditTrails.setEnabled(true);
        AuditTrails.setPublisher(null);
        AuditTrails.clearScope("secondary");
        JpaPlusScope.clear();
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

    /**
     * 验证加密密钥与开关写入加密工具。
     */
    @Test
    void shouldApplyEncryptionConfig() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getEncryption().setKey("demo-key");

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(EncryptedFields.hasKey()).isTrue();

        String encrypted = EncryptedFields.encrypt("value");

        assertThat(EncryptedFields.decrypt(encrypted)).isEqualTo("value");
    }

    /**
     * 验证可以关闭字段加密。
     */
    @Test
    void shouldDisableEncryption() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getEncryption().setEnabled(false);

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(EncryptedFields.isEnabled()).isFalse();
    }

    /**
     * 验证未配置密钥时不会覆盖已有密钥。
     */
    @Test
    void shouldKeepKeyWhenNotConfigured() {
        EncryptedFields.setKey("existing-key");

        new JpaPlusRuntimeConfigurer(new JpaPlusProperties());

        assertThat(EncryptedFields.decrypt(EncryptedFields.encrypt("v")))
                .isEqualTo("v");
    }

    /**
     * 验证多密钥配置与轮换。
     */
    @Test
    void shouldApplyEncryptionKeys() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getEncryption().getKeys().put("v1", "secret-1");
        properties.getEncryption().getKeys().put("v2", "secret-2");
        properties.getEncryption().setActiveKeyId("v2");

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(EncryptedFields.getActiveKeyId()).isEqualTo("v2");
        assertThat(EncryptedFields.encrypt("value")).startsWith("v2:");
        assertThat(EncryptedFields.decrypt(EncryptedFields.encrypt("value")))
                .isEqualTo("value");
    }

    /**
     * 验证操作人提供者优先于固定操作人配置。
     */
    @Test
    void shouldPreferAuditorProvider() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getAudit().setAuditor("system");

        new JpaPlusRuntimeConfigurer(
                properties,
                () -> "from-provider"
        );

        AuditedEntity entity = new AuditedEntity();

        new AuditListener().prePersist(entity);

        assertThat(entity.createdBy).isEqualTo("from-provider");
    }

    /**
     * 验证按数据源作用域写入隔离开关与密钥。
     */
    @Test
    void shouldApplyScopeConfiguration() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getEncryption().setKey("global-key");
        properties.getAudit().setAuditor("global-auditor");

        JpaPlusProperties.Scope scope = new JpaPlusProperties.Scope();
        scope.setBatchSize(100);
        scope.setLogicDeleteEnabled(false);
        scope.setAuditEnabled(false);
        scope.setEncryptionEnabled(true);
        scope.setEncryptionKeys(
                Map.of("v1", "secondary-secret")
        );
        scope.setEncryptionActiveKeyId("v1");

        properties.getScopes().put("secondary", scope);

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(BatchOperation.getDefaultBatchSize()).isEqualTo(500);
        assertThat(BatchOperation.getDefaultBatchSize("secondary"))
                .isEqualTo(100);
        assertThat(LogicDeletes.isEnabled()).isTrue();
        assertThat(LogicDeletes.isEnabled("secondary")).isFalse();
        assertThat(Audits.isEnabled("secondary")).isFalse();

        String scoped = JpaPlusScope.call(
                "secondary",
                () -> EncryptedFields.encrypt("value")
        );

        assertThat(scoped).startsWith("v1:");
        assertThat(JpaPlusScope.call(
                "secondary",
                () -> EncryptedFields.decrypt(scoped)
        )).isEqualTo("value");
    }

    /**
     * 验证作用域未配置的项沿用全局设置。
     */
    @Test
    void shouldKeepGlobalSettingsForUnconfiguredScopeItems() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.setBatchSize(300);

        properties.getScopes().put(
                "secondary",
                new JpaPlusProperties.Scope()
        );

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(BatchOperation.getDefaultBatchSize("secondary"))
                .isEqualTo(300);
        assertThat(LogicDeletes.isEnabled("secondary")).isTrue();
        assertThat(Audits.isEnabled("secondary")).isTrue();
    }

    /**
     * 验证租户与数据权限提供者写入对应工具。
     */
    @Test
    void shouldApplyTenantAndPermissionProviders() {
        JpaPlusProperties properties = new JpaPlusProperties();

        new JpaPlusRuntimeConfigurer(
                properties,
                null,
                () -> "t1",
                type -> java.util.List.of(
                        io.github.neopen.core.persistence.datascope.DataRule.of(
                                "deptId",
                                1L
                        )
                ),
                null
        );

        assertThat(Tenants.currentTenant()).isEqualTo("t1");
        assertThat(DataScopes.isActive()).isTrue();
    }

    /**
     * 验证变更历史开关与发布者生效。
     */
    @Test
    void shouldApplyAuditTrailConfig() {
        JpaPlusProperties properties = new JpaPlusProperties();
        properties.getAudit().setTrailEnabled(false);

        new JpaPlusRuntimeConfigurer(
                properties,
                null,
                null,
                null,
                entry -> {
                }
        );

        assertThat(AuditTrails.isEnabled()).isFalse();
    }

    /**
     * 验证作用域的租户与变更历史开关独立生效。
     */
    @Test
    void shouldApplyScopedTenantAndTrail() {
        JpaPlusProperties properties = new JpaPlusProperties();
        JpaPlusProperties.Scope scope = new JpaPlusProperties.Scope();
        scope.setTenantEnabled(false);
        scope.setTenant("t2");
        scope.setTrailEnabled(false);

        properties.getScopes().put("secondary", scope);

        new JpaPlusRuntimeConfigurer(properties);

        assertThat(Tenants.isEnabled()).isTrue();
        assertThat(Tenants.isEnabled("secondary")).isFalse();
        assertThat(AuditTrails.isEnabled()).isTrue();
        assertThat(AuditTrails.isEnabled("secondary")).isFalse();
        assertThat(JpaPlusScope.call("secondary", Tenants::currentTenant))
                .isNull();
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
