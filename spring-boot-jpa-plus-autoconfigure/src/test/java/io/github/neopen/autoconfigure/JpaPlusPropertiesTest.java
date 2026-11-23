package io.github.neopen.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JpaPlusProperties} 单元测试。
 */
class JpaPlusPropertiesTest {

    /**
     * 验证默认配置值。
     */
    @Test
    void shouldUseDefaults() {
        JpaPlusProperties properties = new JpaPlusProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getBatchSize()).isEqualTo(500);
        assertThat(properties.isHqlEnabled()).isTrue();
        assertThat(properties.isSqlEnabled()).isTrue();
        assertThat(properties.getLogicDelete().isEnabled()).isTrue();
        assertThat(properties.getAudit().isEnabled()).isTrue();
        assertThat(properties.getAudit().getZone()).isEqualTo("UTC");
        assertThat(properties.getAudit().getAuditor()).isNull();
        assertThat(properties.getEncryption().isEnabled()).isTrue();
        assertThat(properties.getEncryption().getKey()).isNull();
        assertThat(properties.getEncryption().getKeys()).isEmpty();
        assertThat(properties.getEncryption().getActiveKeyId()).isNull();
        assertThat(properties.getSlowSql().isEnabled()).isFalse();
        assertThat(properties.getSlowSql().getThresholdMs()).isEqualTo(1000L);
        assertThat(properties.getContextPropagation().isEnabled()).isTrue();
    }

    /**
     * 验证上下文传播开关可关闭与重置。
     */
    @Test
    void shouldToggleContextPropagation() {
        JpaPlusProperties properties = new JpaPlusProperties();

        properties.getContextPropagation().setEnabled(false);

        assertThat(properties.getContextPropagation().isEnabled()).isFalse();

        properties.setContextPropagation(
                new JpaPlusProperties.ContextPropagation()
        );

        assertThat(properties.getContextPropagation().isEnabled()).isTrue();
    }

    /**
     * 验证审计时区必须非空且合法。
     */
    @Test
    void shouldRejectInvalidAuditZone() {
        JpaPlusProperties properties = new JpaPlusProperties();

        assertThatThrownBy(() -> properties.getAudit().setZone(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> properties.getAudit().setZone("Mars/Olympus"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a valid zone id");

        properties.getAudit().setZone("Asia/Shanghai");

        assertThat(properties.getAudit().getZone()).isEqualTo("Asia/Shanghai");
    }

    /**
     * 验证慢 SQL 阈值必须大于 0。
     */
    @Test
    void shouldRejectNonPositiveSlowSqlThreshold() {
        JpaPlusProperties properties = new JpaPlusProperties();

        assertThatThrownBy(() -> properties.getSlowSql().setThresholdMs(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold-ms must be greater than zero");

        properties.getSlowSql().setThresholdMs(2000L);

        assertThat(properties.getSlowSql().getThresholdMs())
                .isEqualTo(2000L);
    }

    /**
     * 验证批次大小必须大于 0。
     */
    @Test
    void shouldRejectNonPositiveBatchSize() {
        JpaPlusProperties properties = new JpaPlusProperties();

        assertThatThrownBy(() -> properties.setBatchSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batch-size must be greater than zero");
    }

    /**
     * 验证数据源逻辑名称不能为空白。
     */
    @Test
    void shouldRejectBlankDatasourceTarget() {
        JpaPlusProperties properties = new JpaPlusProperties();

        assertThatThrownBy(() -> properties.getDatasource().setPrimary(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neopen.jpa.datasource.primary");

        assertThatThrownBy(() -> properties.getDatasource().setReadTarget(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neopen.jpa.datasource.read-target");

        assertThatThrownBy(
                () -> properties.getDatasource().setWriteTarget(null)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("neopen.jpa.datasource.write-target");

        properties.getDatasource().setPrimary("main");

        assertThat(properties.getDatasource().getPrimary()).isEqualTo("main");
    }
}
