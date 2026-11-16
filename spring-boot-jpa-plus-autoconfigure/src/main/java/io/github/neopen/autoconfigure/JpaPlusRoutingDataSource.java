package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.datasource.DataSourceContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按当前线程绑定目标路由连接的数据源。
 *
 * <p>逻辑名称到真实数据源 Bean 的映射由
 * {@code neopen.jpa.datasource.targets} 提供。未指定目标时使用
 * {@code neopen.jpa.datasource.primary} 对应的数据源。</p>
 *
 * <p>真实数据源在首次使用时才从容器获取，避免启动阶段因数据源
 * 尚未初始化而失败。</p>
 *
 * @author neopen
 * @see io.github.neopen.core.persistence.datasource.DataSource
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class JpaPlusRoutingDataSource extends AbstractDataSource
        implements InitializingBean {

    /** 容器，用于按名称获取真实数据源。 */
    private final ApplicationContext applicationContext;

    /** 逻辑名称到 Bean 名称的映射。 */
    private final Map<String, String> targets;

    /** 默认数据源逻辑名称。 */
    private final String primary;

    /** 已解析的数据源缓存。 */
    private final Map<String, javax.sql.DataSource> resolved =
            new ConcurrentHashMap<>();

    /**
     * 创建路由数据源。
     *
     * @param applicationContext 容器，不能为 {@code null}
     * @param targets            逻辑名称到 Bean 名称的映射
     * @param primary            默认数据源逻辑名称
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public JpaPlusRoutingDataSource(
            ApplicationContext applicationContext,
            Map<String, String> targets,
            String primary
    ) {
        if (applicationContext == null) {
            throw new IllegalArgumentException(
                    "ApplicationContext must not be null"
            );
        }

        if (targets == null || targets.isEmpty()) {
            throw new IllegalArgumentException(
                    "neopen.jpa.datasource.targets must not be empty"
            );
        }

        if (primary == null || primary.isBlank()) {
            throw new IllegalArgumentException(
                    "neopen.jpa.datasource.primary must not be blank"
            );
        }

        if (!targets.containsKey(primary)) {
            throw new IllegalArgumentException(
                    "Primary data source [" + primary
                            + "] is not configured in targets"
            );
        }

        if (targets.containsValue("dataSource")) {
            throw new IllegalArgumentException(
                    "Target bean name must not be dataSource"
            );
        }

        this.applicationContext = applicationContext;
        this.targets = Map.copyOf(targets);
        this.primary = primary;
    }

    /**
     * 校验配置。
     */
    @Override
    public void afterPropertiesSet() {
        if (!targets.containsKey(primary)) {
            throw new IllegalStateException(
                    "Primary data source [" + primary + "] is missing"
            );
        }
    }

    /**
     * 获取当前线程生效的数据源逻辑名称。
     *
     * @return 数据源逻辑名称
     */
    public String currentTarget() {
        String target = DataSourceContext.current();

        return target != null && targets.containsKey(target)
                ? target
                : primary;
    }

    /**
     * 获取连接。
     *
     * @return 数据库连接
     * @throws SQLException 获取连接失败时抛出
     */
    @Override
    public Connection getConnection() throws SQLException {
        return resolve(currentTarget()).getConnection();
    }

    /**
     * 使用指定账号获取连接。
     *
     * @param username 用户名
     * @param password 密码
     * @return 数据库连接
     * @throws SQLException 获取连接失败时抛出
     */
    @Override
    public Connection getConnection(
            String username,
            String password
    ) throws SQLException {
        return resolve(currentTarget()).getConnection(username, password);
    }

    /**
     * 解析指定逻辑名称的数据源。
     *
     * @param target 数据源逻辑名称
     * @return 真实数据源
     */
    private javax.sql.DataSource resolve(String target) {
        return resolved.computeIfAbsent(target, name -> {
            String beanName = targets.get(name);

            if (beanName == null) {
                throw new IllegalStateException(
                        "Unknown data source target: " + name
                );
            }

            return applicationContext.getBean(
                    beanName,
                    javax.sql.DataSource.class
            );
        });
    }
}
