package io.github.neopen.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JPA Plus 全局配置属性。
 *
 * <p>
 * 统一使用 {@code neopen.jpa} 作为配置前缀，
 * 避免不同模块分别定义重复的配置项。
 * </p>
 *
 * <p>
 * 当前属性主要用于：
 * </p>
 *
 * <ul>
 *     <li>控制 JPA Plus 是否整体启用</li>
 *     <li>设置批量操作的默认批次大小</li>
 *     <li>控制 HQL 执行器是否注册</li>
 *     <li>控制原生 SQL 执行器是否注册</li>
 * </ul>
 *
 * <p>
 * 注意：
 * {@code batchSize} 目前由业务 DAO 在构造时显式传入，
 * 自动配置本身不会自动修改已经创建的 DAO 实例。
 * </p>
 */
@ConfigurationProperties(prefix = "neopen.jpa")
public class JpaPlusProperties {

    /**
     * 创建使用默认值的配置属性对象。
     */
    public JpaPlusProperties() {
    }

    /**
     * 是否启用 JPA Plus。
     *
     * <p>
     * 默认值：true。
     * </p>
     *
     * <p>
     * 设置为 false 后，JPA Plus 自动配置整体不生效，
     * 不会注册默认 HQL 和 SQL 执行器。
     * </p>
     */
    private boolean enabled = true;

    /**
     * 默认批量处理数量。
     *
     * <p>
     * 默认值：500。
     * </p>
     *
     * <p>
     * 当业务 DAO 继承 AbstractJpaPlusDao 时，
     * 可以将该值传入父类构造方法。
     * </p>
     *
     * <p>
     * 该值表示每处理多少条记录执行一次 flush 和 clear，
     * 不等同于 Hibernate JDBC 批处理大小。
     * </p>
     */
    private int batchSize = 500;

    /**
     * 是否注册 HQL 执行器。
     *
     * <p>
     * 默认值：true。
     * </p>
     */
    private boolean hqlEnabled = true;

    /**
     * 是否注册原生 SQL 执行器。
     *
     * <p>
     * 默认值：true。
     * </p>
     */
    private boolean sqlEnabled = true;

    /**
     * 获取是否启用 JPA Plus。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 JPA Plus。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取默认批次大小。
     *
     * @return 每批处理的实体数量
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 设置批量操作默认批次大小。
     *
     * @param batchSize 批次大小，必须大于 0
     * @throws IllegalArgumentException 批次大小不大于 0
     */
    public void setBatchSize(int batchSize) {

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "neopen.jpa.batch-size must be greater than zero"
            );
        }

        this.batchSize = batchSize;
    }

    /**
     * 获取是否注册 HQL 执行器。
     *
     * @return 注册时返回 {@code true}
     */
    public boolean isHqlEnabled() {
        return hqlEnabled;
    }

    /**
     * 设置是否注册 HQL 执行器。
     *
     * @param hqlEnabled 是否注册
     */
    public void setHqlEnabled(boolean hqlEnabled) {
        this.hqlEnabled = hqlEnabled;
    }

    /**
     * 获取是否注册原生 SQL 执行器。
     *
     * @return 注册时返回 {@code true}
     */
    public boolean isSqlEnabled() {
        return sqlEnabled;
    }

    /**
     * 设置是否注册原生 SQL 执行器。
     *
     * @param sqlEnabled 是否注册
     */
    public void setSqlEnabled(boolean sqlEnabled) {
        this.sqlEnabled = sqlEnabled;
    }
}
