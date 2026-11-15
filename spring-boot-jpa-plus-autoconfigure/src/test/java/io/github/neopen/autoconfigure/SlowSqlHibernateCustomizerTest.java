package io.github.neopen.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SlowSqlHibernateCustomizer} 单元测试。
 */
class SlowSqlHibernateCustomizerTest {

    /**
     * 验证慢查询阈值写入 Hibernate 属性。
     */
    @Test
    void shouldConfigureSlowQueryThreshold() {
        Map<String, Object> properties = new HashMap<>();

        new SlowSqlHibernateCustomizer(1500L).customize(properties);

        assertThat(properties).containsEntry(
                "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS",
                1500L
        );
    }

    /**
     * 验证阈值必须大于 0。
     */
    @Test
    void shouldRejectNonPositiveThreshold() {
        assertThatThrownBy(() -> new SlowSqlHibernateCustomizer(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }
}
