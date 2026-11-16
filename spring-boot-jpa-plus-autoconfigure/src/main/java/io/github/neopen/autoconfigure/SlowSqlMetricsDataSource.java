package io.github.neopen.autoconfigure;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

/**
 * 统计慢 SQL 的数据源装饰器。
 *
 * <p>通过动态代理拦截 JDBC 语句执行，耗时超过阈值的语句计入
 * {@code jpa.plus.sql.slow} 计数器，业务代码无需改动。</p>
 *
 * <p>仅在 {@code neopen.jpa.metrics.enabled=true} 时由自动配置
 * 自动套在数据源外层；也可以手动包装：</p>
 *
 * <pre>{@code
 * DataSource dataSource = new SlowSqlMetricsDataSource(
 *         hikariDataSource, metrics, 1000L
 * );
 * }</pre>
 *
 * <p>装饰器只改变连接的返回类型，连接、事务与语句行为全部委托给
 * 原始数据源。</p>
 *
 * @author neopen
 * @see JpaPlusMetrics
 * @see JpaPlusMetricsAutoConfiguration
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class SlowSqlMetricsDataSource extends DelegatingDataSource {

    /** 指标收集器。 */
    private final JpaPlusMetrics metrics;

    /** 慢 SQL 阈值，单位纳秒。 */
    private final long thresholdNanos;

    /**
     * 创建慢 SQL 指标数据源。
     *
     * @param target        原始数据源，不能为 {@code null}
     * @param metrics       指标收集器，不能为 {@code null}
     * @param thresholdMs   慢 SQL 阈值毫秒数，必须大于 0
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public SlowSqlMetricsDataSource(
            DataSource target,
            JpaPlusMetrics metrics,
            long thresholdMs
    ) {
        super(target);

        if (metrics == null) {
            throw new IllegalArgumentException(
                    "JpaPlusMetrics must not be null"
            );
        }

        if (thresholdMs <= 0) {
            throw new IllegalArgumentException(
                    "Slow SQL threshold must be greater than zero"
            );
        }

        this.metrics = metrics;
        this.thresholdNanos = TimeUnit.MILLISECONDS.toNanos(thresholdMs);
    }

    /**
     * 获取代理连接。
     *
     * @return 代理连接
     * @throws SQLException 获取连接失败时抛出
     */
    @Override
    public Connection getConnection() throws SQLException {
        return wrap(super.getConnection());
    }

    /**
     * 使用指定账号获取代理连接。
     *
     * @param username 用户名
     * @param password 密码
     * @return 代理连接
     * @throws SQLException 获取连接失败时抛出
     */
    @Override
    public Connection getConnection(
            String username,
            String password
    ) throws SQLException {
        return wrap(super.getConnection(username, password));
    }

    /**
     * 包装连接。
     *
     * @param connection 原始连接，允许为 {@code null}
     * @return 代理连接
     */
    private Connection wrap(Connection connection) {
        if (connection == null) {
            return null;
        }

        return (Connection) Proxy.newProxyInstance(
                SlowSqlMetricsDataSource.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new ConnectionHandler(connection)
        );
    }

    /**
     * 包装语句。
     *
     * @param statement 原始语句，不能为 {@code null}
     * @param sql       语句 SQL，允许为 {@code null}
     * @return 代理语句
     */
    private Statement wrap(Statement statement, String sql) {
        Class<?> type = Statement.class;

        if (statement instanceof CallableStatement) {
            type = CallableStatement.class;
        } else if (statement instanceof PreparedStatement) {
            type = PreparedStatement.class;
        }

        return (Statement) Proxy.newProxyInstance(
                SlowSqlMetricsDataSource.class.getClassLoader(),
                new Class<?>[]{type},
                new StatementHandler(statement, sql)
        );
    }

    /**
     * 反射调用并展开目标异常。
     *
     * @param target 目标对象
     * @param method 方法
     * @param args   参数
     * @return 调用结果
     * @throws Throwable 目标方法异常
     */
    private static Object invoke(
            Object target,
            Method method,
            Object[] args
    ) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * 连接代理。
     */
    private final class ConnectionHandler implements InvocationHandler {

        /** 原始连接。 */
        private final Connection target;

        /**
         * 创建连接代理。
         *
         * @param target 原始连接
         */
        private ConnectionHandler(Connection target) {
            this.target = target;
        }

        /**
         * 委托调用并包装返回的语句。
         *
         * @param proxy  代理对象
         * @param method 方法
         * @param args   参数
         * @return 调用结果
         * @throws Throwable 调用异常
         */
        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] args
        ) throws Throwable {
            Object result = SlowSqlMetricsDataSource.invoke(
                    target,
                    method,
                    args
            );

            if (!(result instanceof Statement statement)) {
                return result;
            }

            String sql = args != null
                    && args.length > 0
                    && args[0] instanceof String text
                    ? text
                    : null;

            return wrap(statement, sql);
        }
    }

    /**
     * 语句代理。
     */
    private final class StatementHandler implements InvocationHandler {

        /** 原始语句。 */
        private final Statement target;

        /** 语句 SQL。 */
        private final String sql;

        /**
         * 创建语句代理。
         *
         * @param target 原始语句
         * @param sql    语句 SQL，允许为 {@code null}
         */
        private StatementHandler(Statement target, String sql) {
            this.target = target;
            this.sql = sql;
        }

        /**
         * 统计执行耗时并记录慢 SQL。
         *
         * @param proxy  代理对象
         * @param method 方法
         * @param args   参数
         * @return 调用结果
         * @throws Throwable 调用异常
         */
        @Override
        public Object invoke(
                Object proxy,
                Method method,
                Object[] args
        ) throws Throwable {
            if (!method.getName().startsWith("execute")) {
                return SlowSqlMetricsDataSource.invoke(
                        target,
                        method,
                        args
                );
            }

            long start = System.nanoTime();

            try {
                return SlowSqlMetricsDataSource.invoke(
                        target,
                        method,
                        args
                );
            } finally {
                if (System.nanoTime() - start >= thresholdNanos) {
                    metrics.recordSlowSql(sql);
                }
            }
        }
    }
}
