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
}
