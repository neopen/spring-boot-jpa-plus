package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 把配置属性应用到 JPA Plus 运行时工具类。
 *
 * <p>逻辑删除、审计和批量操作以静态工具方法的形式提供，
 * 无法直接通过依赖注入配置。本类在 Spring 容器启动时读取
 * {@link JpaPlusProperties}，一次性写入这些全局设置。</p>
 *
 * <p>业务代码仍可在运行期调用 {@link LogicDeletes#setEnabled}、
 * {@link Audits#setEnabled} 等方法动态调整。</p>
 */
public class JpaPlusRuntimeConfigurer {

    /**
     * 应用配置属性。
     *
     * @param properties JPA Plus 配置属性，不能为 {@code null}
     * @throws IllegalArgumentException 配置属性为空
     */
    public JpaPlusRuntimeConfigurer(JpaPlusProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException(
                    "JpaPlusProperties must not be null"
            );
        }

        apply(properties);
    }

    /**
     * 写入全局配置。
     *
     * @param properties 配置属性
     */
    private static void apply(JpaPlusProperties properties) {
        BatchOperation.setDefaultBatchSize(properties.getBatchSize());

        JpaPlusProperties.LogicDelete logicDelete =
                properties.getLogicDelete();

        if (logicDelete != null) {
            LogicDeletes.setEnabled(logicDelete.isEnabled());
        }

        JpaPlusProperties.Audit audit = properties.getAudit();

        if (audit == null) {
            return;
        }

        Audits.setEnabled(audit.isEnabled());

        if (StringUtils.hasText(audit.getZone())) {
            Audits.setClock(Clock.system(
                    ZoneId.of(audit.getZone())
            ));
        }

        if (StringUtils.hasText(audit.getAuditor())) {
            String auditor = audit.getAuditor();
            Audits.setAuditorSupplier(() -> auditor);
        }
    }
}
