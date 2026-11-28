/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
}
