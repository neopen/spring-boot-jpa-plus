package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.persistence.audit.AuditorProvider;
import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.encrypt.EncryptedFields;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;

/**
 * 把配置属性应用到 JPA Plus 运行时工具类。
 *
 * <p>逻辑删除、审计和批量操作以静态工具方法的形式提供，
 * 无法直接通过依赖注入配置。本类在 Spring 容器启动时读取
 * {@link JpaPlusProperties}，一次性写入这些全局设置。</p>
 *
 * <p>业务代码仍可在运行期调用 {@link LogicDeletes#setEnabled}、
 * {@link Audits#setEnabled} 等方法动态调整。</p>
 *
 * <p>{@code neopen.jpa.scopes} 下的配置会按作用域名称写入对应的
 * 隔离开关，配合 {@code JpaPlusScope} 在访问不同数据源时生效。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class JpaPlusRuntimeConfigurer {

    /**
     * 应用配置属性。
     *
     * @param properties JPA Plus 配置属性，不能为 {@code null}
     * @throws IllegalArgumentException 配置属性为空
     */
    public JpaPlusRuntimeConfigurer(JpaPlusProperties properties) {
        this(properties, null);
    }

    /**
     * 应用配置属性并接入操作人提供者。
     *
     * @param properties       JPA Plus 配置属性，不能为 {@code null}
     * @param auditorProvider 操作人提供者，允许为 {@code null}
     * @throws IllegalArgumentException 配置属性为空
     */
    public JpaPlusRuntimeConfigurer(
            JpaPlusProperties properties,
            AuditorProvider auditorProvider
    ) {
        if (properties == null) {
            throw new IllegalArgumentException(
                    "JpaPlusProperties must not be null"
            );
        }

        apply(properties, auditorProvider);
    }

    /**
     * 写入全局配置。
     *
     * @param properties      配置属性
     * @param auditorProvider 操作人提供者，允许为 {@code null}
     */
    private static void apply(
            JpaPlusProperties properties,
            AuditorProvider auditorProvider
    ) {
        BatchOperation.setDefaultBatchSize(properties.getBatchSize());

        JpaPlusProperties.LogicDelete logicDelete =
                properties.getLogicDelete();

        if (logicDelete != null) {
            LogicDeletes.setEnabled(logicDelete.isEnabled());
        }

        applyEncryption(properties.getEncryption());
        applyAudit(properties.getAudit(), auditorProvider);
        applyScopes(properties.getScopes());
    }

    /**
     * 写入按数据源作用域隔离的开关。
     *
     * <p>只覆盖作用域中显式配置的项，未配置的项继续沿用全局设置。</p>
     *
     * @param scopes 作用域名称到开关配置的映射，允许为 {@code null}
     */
    private static void applyScopes(Map<String, JpaPlusProperties.Scope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return;
        }

        scopes.forEach((name, scope) -> {
            if (scope == null) {
                return;
            }

            if (scope.getBatchSize() != null) {
                BatchOperation.setDefaultBatchSize(
                        name,
                        scope.getBatchSize()
                );
            }

            if (scope.getLogicDeleteEnabled() != null) {
                LogicDeletes.setEnabled(
                        name,
                        scope.getLogicDeleteEnabled()
                );
            }

            if (scope.getAuditEnabled() != null) {
                Audits.setEnabled(name, scope.getAuditEnabled());
            }

            if (StringUtils.hasText(scope.getAuditAuditor())) {
                String auditor = scope.getAuditAuditor();
                Audits.setAuditorSupplier(name, () -> auditor);
            }

            applyScopeEncryption(name, scope);
        });
    }

    /**
     * 写入作用域的加密配置。
     *
     * @param name  作用域名称
     * @param scope 作用域配置
     */
    private static void applyScopeEncryption(
            String name,
            JpaPlusProperties.Scope scope
    ) {
        if (scope.getEncryptionEnabled() != null) {
            EncryptedFields.setEnabled(
                    name,
                    scope.getEncryptionEnabled()
            );
        }

        Map<String, String> keys = scope.getEncryptionKeys();

        if (keys != null && !keys.isEmpty()) {
            EncryptedFields.setKeys(
                    name,
                    keys,
                    scope.getEncryptionActiveKeyId()
            );
        } else if (StringUtils.hasText(scope.getEncryptionKey())) {
            EncryptedFields.setKey(
                    name,
                    scope.getEncryptionKey()
            );
        }
    }

    /**
     * 写入加密配置。
     *
     * <p>配置了多密钥时优先使用密钥仓库；只配置单密钥时沿用
     * 无前缀的历史密文格式。</p>
     *
     * @param encryption 加密配置
     */
    private static void applyEncryption(
            JpaPlusProperties.Encryption encryption
    ) {
        if (encryption == null) {
            return;
        }

        EncryptedFields.setEnabled(encryption.isEnabled());

        Map<String, String> keys = encryption.getKeys();

        if (keys != null && !keys.isEmpty()) {
            EncryptedFields.setKeys(keys, encryption.getActiveKeyId());
        } else if (StringUtils.hasText(encryption.getKey())) {
            EncryptedFields.setKey(encryption.getKey());
        }
    }

    /**
     * 写入审计配置。
     *
     * <p>操作人提供者优先于固定操作人配置。</p>
     *
     * @param audit           审计配置
     * @param auditorProvider 操作人提供者，允许为 {@code null}
     */
    private static void applyAudit(
            JpaPlusProperties.Audit audit,
            AuditorProvider auditorProvider
    ) {
        if (audit == null) {
            return;
        }

        Audits.setEnabled(audit.isEnabled());

        if (StringUtils.hasText(audit.getZone())) {
            Audits.setClock(Clock.system(
                    ZoneId.of(audit.getZone())
            ));
        }

        if (auditorProvider != null) {
            Audits.setAuditorProvider(auditorProvider);
        } else if (StringUtils.hasText(audit.getAuditor())) {
            String auditor = audit.getAuditor();
            Audits.setAuditorSupplier(() -> auditor);
        }
    }
}
