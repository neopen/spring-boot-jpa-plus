package io.github.neopen.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JdbcBatchHibernateCustomizer} 单元测试。
 */
class JdbcBatchHibernateCustomizerTest {

    /**
     * 验证批量参数写入 Hibernate 属性。
     */
    @Test
    void shouldConfigureBatchProperties() {
        Map<String, Object> properties = new HashMap<>();

        new JdbcBatchHibernateCustomizer(100, true, false)
                .customize(properties);

        assertThat(properties)
                .containsEntry("hibernate.jdbc.batch_size", 100)
                .containsEntry("hibernate.order_inserts", true)
                .containsEntry("hibernate.order_updates", false);
    }

    /**
     * 验证批量大小必须大于 0。
     */
    @Test
    void shouldRejectNonPositiveBatchSize() {
        assertThatThrownBy(() -> new JdbcBatchHibernateCustomizer(0, true, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    /**
     * 验证默认配置关闭 JDBC 批量。
     */
    @Test
    void shouldDisableBatchByDefault() {
        JpaPlusProperties properties = new JpaPlusProperties();
        JpaPlusProperties.JdbcBatch jdbcBatch = properties.getJdbcBatch();

        assertThat(jdbcBatch.isEnabled()).isFalse();
        assertThat(jdbcBatch.getBatchSize()).isEqualTo(50);
        assertThat(jdbcBatch.isOrderInserts()).isTrue();
        assertThat(jdbcBatch.isOrderUpdates()).isTrue();
        assertThatThrownBy(() -> jdbcBatch.setBatchSize(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
