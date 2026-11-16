package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.datasource.DataSourceContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JpaPlusRoutingDataSource} 单元测试。
 */
class JpaPlusRoutingDataSourceTest {

    /**
     * 清理线程上下文。
     */
    @AfterEach
    void reset() {
        DataSourceContext.clear();
    }

    /**
     * 验证默认使用主数据源。
     *
     * @throws SQLException 获取连接失败时抛出
     */
    @Test
    void shouldUsePrimaryByDefault() throws SQLException {
        ApplicationContext context = mock(ApplicationContext.class);
        DataSource primary = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(context.getBean("primaryDs", DataSource.class))
                .thenReturn(primary);
        when(primary.getConnection()).thenReturn(connection);

        JpaPlusRoutingDataSource routing = routing(context);

        assertThat(routing.getConnection()).isSameAs(connection);
        assertThat(routing.currentTarget()).isEqualTo("primary");
    }

    /**
     * 验证线程上下文可以切换到只读数据源。
     *
     * @throws SQLException 获取连接失败时抛出
     */
    @Test
    void shouldRouteToContextTarget() throws SQLException {
        ApplicationContext context = mock(ApplicationContext.class);
        DataSource read = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(context.getBean("readDs", DataSource.class)).thenReturn(read);
        when(read.getConnection()).thenReturn(connection);

        JpaPlusRoutingDataSource routing = routing(context);

        Connection result = DataSourceContext.call(
                "read",
                () -> {
                    try {
                        return routing.getConnection();
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );

        assertThat(result).isSameAs(connection);
        verify(read).getConnection();
    }

    /**
     * 验证路由次数写入指标。
     *
     * @throws SQLException 获取连接失败时抛出
     */
    @Test
    void shouldRecordRouteMetric() throws SQLException {
        ApplicationContext context = mock(ApplicationContext.class);
        DataSource read = mock(DataSource.class);
        Connection connection = mock(Connection.class);

        when(context.getBean("readDs", DataSource.class)).thenReturn(read);
        when(read.getConnection()).thenReturn(connection);

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JpaPlusRoutingDataSource routing = routing(context);
        routing.setMetrics(new JpaPlusMetrics(registry));

        Connection result = DataSourceContext.call(
                "read",
                () -> {
                    try {
                        return routing.getConnection();
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                }
        );

        assertThat(result).isSameAs(connection);
        assertThat(registry.get("jpa.plus.datasource.routes")
                .tag("target", "read")
                .counter()
                .count()).isEqualTo(1.0);
    }

    /**
     * 验证未知目标回退到主数据源。
     */
    @Test
    void shouldFallbackToPrimaryForUnknownTarget() {
        ApplicationContext context = mock(ApplicationContext.class);
        JpaPlusRoutingDataSource routing = routing(context);

        String target = DataSourceContext.call(
                "unknown",
                routing::currentTarget
        );

        assertThat(target).isEqualTo("primary");
    }

    /**
     * 验证缺少主数据源配置时拒绝创建。
     */
    @Test
    void shouldRejectMissingPrimaryTarget() {
        ApplicationContext context = mock(ApplicationContext.class);

        assertThatThrownBy(() -> new JpaPlusRoutingDataSource(
                context,
                Map.of("read", "readDs"),
                "primary"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not configured");
    }

    /**
     * 验证空目标映射被拒绝。
     */
    @Test
    void shouldRejectEmptyTargets() {
        ApplicationContext context = mock(ApplicationContext.class);

        assertThatThrownBy(() -> new JpaPlusRoutingDataSource(
                context,
                Map.of(),
                "primary"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    /**
     * 验证目标 Bean 不能命名为 dataSource。
     */
    @Test
    void shouldRejectDataSourceBeanName() {
        ApplicationContext context = mock(ApplicationContext.class);

        assertThatThrownBy(() -> new JpaPlusRoutingDataSource(
                context,
                Map.of("primary", "dataSource"),
                "primary"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be dataSource");
    }

    /**
     * 创建路由数据源。
     *
     * @param context 容器
     * @return 路由数据源
     */
    private JpaPlusRoutingDataSource routing(ApplicationContext context) {
        return new JpaPlusRoutingDataSource(
                context,
                Map.of(
                        "primary", "primaryDs",
                        "read", "readDs",
                        "write", "writeDs"
                ),
                "primary"
        );
    }
}
