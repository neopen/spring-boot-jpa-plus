package io.github.neopen.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JPA Plus 全局配置属性。
 *
 * <p>
 * 通过 application.yml 中的 neopen.jpa 前缀绑定配置。
 * </p>
 *
 * <p>
 * 配置示例：
 * </p>
 *
 * <pre>
 * neopen:
 *   jpa:
 *     enabled: true
 *     batch-size: 500
 *     sql-enabled: true
 *     hql-enabled: true
 * </pre>
 *
 * <p>
 * 本类只负责接收配置，不直接执行任何数据库操作。
 * 具体配置由 JpaPlusAutoConfiguration 中的 Bean
 * 根据需要读取。
 * </p>
 */
@ConfigurationProperties(prefix = "neopen.jpa")
public class JpaPlusProperties {

    /**
     * 是否启用 JPA Plus。
     *
     * <p>
     * 默认开启。
     * 设置为 false 后，JPA Plus 自动配置不再注册
     * 自己提供的默认 Bean。
     * </p>
     */
    private boolean enabled = true;

    /**
     * 批量操作每批处理的实体数量。
     *
     * <p>
     * 例如 batch-size = 500：
     * 每处理 500 条记录执行一次 flush 和 clear，
     * 用于控制 Hibernate 一级缓存占用。
     * </p>
     *
     * <p>
     * 注意：
     * 该参数不等同于 Hibernate JDBC 批处理大小。
     * JDBC 批处理还需要配置：
     * spring.jpa.properties.hibernate.jdbc.batch_size
     * </p>
     */
    private int batchSize = 500;

    /**
     * 是否启用原生 SQL 执行器。
     *
     * <p>
     * 设置为 false 后，不注册 JpaPlusSqlExecutor。
     * 这只是控制 JPA Plus 对原生 SQL 的封装入口，
     * 不会禁止用户直接通过 EntityManager 执行原生 SQL。
     * </p>
     */
    private boolean sqlEnabled = true;

    /**
     * 是否启用 HQL 执行器。
     *
     * <p>
     * 设置为 false 后，不注册 JpaPlusHqlExecutor。
     * </p>
     */
    private boolean hqlEnabled = true;

    /**
     * 是否启用批量操作。
     *
     * <p>
     * 关闭后，不注册 BatchOperation 相关的辅助 Bean。
     * 注意：当前 BatchOperation 是静态工具类，
     * 不需要注册为 Spring Bean。
     * </p>
     */
    private boolean batchEnabled = true;

    /**
     * 是否启用动态查询。
     *
     * <p>
     * 控制是否注册 JpaPlusQueryBuilder 的默认 Bean。
     * </p>
     */
    private boolean queryEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "neopen.jpa.batch-size must be greater than zero"
            );
        }

        this.batchSize = batchSize;
    }

    public boolean isSqlEnabled() {
        return sqlEnabled;
    }

    public void setSqlEnabled(boolean sqlEnabled) {
        this.sqlEnabled = sqlEnabled;
    }

    public boolean isHqlEnabled() {
        return hqlEnabled;
    }

    public void setHqlEnabled(boolean hqlEnabled) {
        this.hqlEnabled = hqlEnabled;
    }

    public boolean isBatchEnabled() {
        return batchEnabled;
    }

    public void setBatchEnabled(boolean batchEnabled) {
        this.batchEnabled = batchEnabled;
    }

    public boolean isQueryEnabled() {
        return queryEnabled;
    }

    public void setQueryEnabled(boolean queryEnabled) {
        this.queryEnabled = queryEnabled;
    }
}
