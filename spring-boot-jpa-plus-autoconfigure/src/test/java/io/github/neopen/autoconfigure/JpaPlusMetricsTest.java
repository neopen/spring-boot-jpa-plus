package io.github.neopen.autoconfigure;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JpaPlusMetrics} 与 {@link SlowSqlMetricsDataSource} 单元测试。
 */
class JpaPlusMetricsTest {

    /**
     * 验证路由次数按目标计数。
     */
    @Test
    void shouldCountRoutesByTarget() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JpaPlusMetrics metrics = new JpaPlusMetrics(registry);

        metrics.recordRoute("read");
        metrics.recordRoute("read");
        metrics.recordRoute(null);

        assertThat(registry.get("jpa.plus.datasource.routes")
                .tag("target", "read")
                .counter()
                .count()).isEqualTo(2.0);

        assertThat(registry.get("jpa.plus.datasource.routes")
                .tag("target", "default")
                .counter()
                .count()).isEqualTo(1.0);
    }

    /**
     * 验证慢 SQL 按语句类型计数。
     */
    @Test
    void shouldCountSlowSqlByType() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JpaPlusMetrics metrics = new JpaPlusMetrics(registry);

        metrics.recordSlowSql("select * from orders");
        metrics.recordSlowSql(null);

        assertThat(registry.get("jpa.plus.sql.slow")
                .tag("type", "SELECT")
                .counter()
                .count()).isEqualTo(1.0);

        assertThat(registry.get("jpa.plus.sql.slow")
                .tag("type", "UNKNOWN")
                .counter()
                .count()).isEqualTo(1.0);
    }

    /**
     * 验证超过阈值的语句计入慢 SQL。
     *
     * @throws SQLException 执行语句失败时抛出
     */
    @Test
    void shouldRecordSlowStatement() throws SQLException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JpaPlusMetrics metrics = new JpaPlusMetrics(registry);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select 1"))
                .thenReturn(statement);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            Thread.sleep(20L);
            return null;
        });

        SlowSqlMetricsDataSource metricsDataSource =
                new SlowSqlMetricsDataSource(dataSource, metrics, 1L);

        try (Connection acquired = metricsDataSource.getConnection();
             PreparedStatement prepared =
                     acquired.prepareStatement("select 1")) {
            prepared.executeQuery();
        }

        assertThat(registry.get("jpa.plus.sql.slow")
                .tag("type", "SELECT")
                .counter()
                .count()).isEqualTo(1.0);
    }

    /**
     * 验证未超过阈值的语句不计入慢 SQL。
     *
     * @throws SQLException 执行语句失败时抛出
     */
    @Test
    void shouldSkipFastStatement() throws SQLException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JpaPlusMetrics metrics = new JpaPlusMetrics(registry);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select 1"))
                .thenReturn(statement);

        SlowSqlMetricsDataSource metricsDataSource =
                new SlowSqlMetricsDataSource(
                        dataSource,
                        metrics,
                        60_000L
                );

        try (Connection acquired = metricsDataSource.getConnection();
             PreparedStatement prepared =
                     acquired.prepareStatement("select 1")) {
            prepared.executeQuery();
        }

        assertThat(registry.find("jpa.plus.sql.slow").counter()).isNull();
    }

    /**
     * 验证空注册表被拒绝。
     */
    @Test
    void shouldRejectNullRegistry() {
        assertThatThrownBy(() -> new JpaPlusMetrics(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MeterRegistry");
    }
}
