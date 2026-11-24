/*
 * Copyright 2026 neopen
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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

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
 *     <li>控制逻辑删除过滤与审计字段填充</li>
 * </ul>
 *
 * <p>
 * 注意：
 * {@code batchSize} 会在自动配置启动时写入
 * {@code BatchOperation} 的默认批次大小，
 * 未显式传入批次大小的批量方法会使用该值。
 * </p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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
     * 逻辑删除过滤配置。
     *
     * <p>
     * 默认值：启用。
     * </p>
     */
    private LogicDelete logicDelete = new LogicDelete();

    /**
     * 审计字段填充配置。
     *
     * <p>
     * 默认值：启用，UTC 时区，不写入操作人。
     * </p>
     */
    private Audit audit = new Audit();

    /**
     * 字段加密配置。
     *
     * <p>
     * 默认值：启用，未配置密钥。
     * </p>
     */
    private Encryption encryption = new Encryption();

    /**
     * 慢 SQL 日志配置。
     *
     * <p>
     * 默认值：关闭。
     * </p>
     */
    private SlowSql slowSql = new SlowSql();

    /**
     * Hibernate JDBC 批量配置。
     *
     * <p>
     * 默认值：关闭。开启后自动设置 Hibernate 的批量写入参数。
     * </p>
     */
    private JdbcBatch jdbcBatch = new JdbcBatch();

    /**
     * Micrometer 指标配置。
     *
     * <p>
     * 默认值：关闭。开启后需要容器中存在 {@code MeterRegistry}。
     * </p>
     */
    private Metrics metrics = new Metrics();

    /**
     * 多租户配置。
     *
     * <p>
     * 默认值：启用，未配置租户提供者。
     * </p>
     */
    private Tenant tenant = new Tenant();

    /**
     * 行级数据权限配置。
     *
     * <p>
     * 默认值：启用，未配置权限提供者。
     * </p>
     */
    private DataScope dataScope = new DataScope();

    /**
     * 动态数据源配置。
     *
     * <p>
     * 默认值：关闭，使用 Spring Boot 自带的数据源。
     * </p>
     */
    private Datasource datasource = new Datasource();

    /**
     * 按数据源作用域配置的开关。
     *
     * <p>
     * 键为作用域名称，与 {@code JpaPlusScope} 绑定时使用的名称一致。
     * 未配置的项沿用全局配置。默认值：空。
     * </p>
     */
    private Map<String, Scope> scopes = new LinkedHashMap<>();

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

    /**
     * 获取逻辑删除过滤配置。
     *
     * @return 逻辑删除配置
     */
    public LogicDelete getLogicDelete() {
        return logicDelete;
    }

    /**
     * 设置逻辑删除过滤配置。
     *
     * @param logicDelete 逻辑删除配置
     */
    public void setLogicDelete(LogicDelete logicDelete) {
        this.logicDelete = logicDelete;
    }

    /**
     * 获取审计字段填充配置。
     *
     * @return 审计配置
     */
    public Audit getAudit() {
        return audit;
    }

    /**
     * 设置审计字段填充配置。
     *
     * @param audit 审计配置
     */
    public void setAudit(Audit audit) {
        this.audit = audit;
    }

    /**
     * 获取字段加密配置。
     *
     * @return 加密配置
     */
    public Encryption getEncryption() {
        return encryption;
    }

    /**
     * 设置字段加密配置。
     *
     * @param encryption 加密配置
     */
    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    /**
     * 获取慢 SQL 日志配置。
     *
     * @return 慢 SQL 配置
     */
    public SlowSql getSlowSql() {
        return slowSql;
    }

    /**
     * 获取 Hibernate JDBC 批量配置。
     *
     * @return JDBC 批量配置
     */
    public JdbcBatch getJdbcBatch() {
        return jdbcBatch;
    }

    /**
     * 获取 Micrometer 指标配置。
     *
     * @return 指标配置
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * 获取多租户配置。
     *
     * @return 多租户配置
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * 设置多租户配置。
     *
     * @param tenant 多租户配置
     */
    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * 获取行级数据权限配置。
     *
     * @return 行级数据权限配置
     */
    public DataScope getDataScope() {
        return dataScope;
    }

    /**
     * 设置行级数据权限配置。
     *
     * @param dataScope 行级数据权限配置
     */
    public void setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
    }

    /**
     * 获取动态数据源配置。
     *
     * @return 动态数据源配置
     */
    public Datasource getDatasource() {
        return datasource;
    }

    /**
     * 设置动态数据源配置。
     *
     * @param datasource 动态数据源配置
     */
    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    /**
     * 获取按数据源作用域配置的开关。
     *
     * @return 作用域名称到开关配置的映射
     */
    public Map<String, Scope> getScopes() {
        return scopes;
    }

    /**
     * 设置按数据源作用域配置的开关。
     *
     * @param scopes 作用域名称到开关配置的映射
     */
    public void setScopes(Map<String, Scope> scopes) {
        this.scopes = scopes;
    }

    /**
     * 设置慢 SQL 日志配置。
     *
     * @param slowSql 慢 SQL 配置
     */
    public void setSlowSql(SlowSql slowSql) {
        this.slowSql = slowSql;
    }

    /**
     * 设置 Hibernate JDBC 批量配置。
     *
     * @param jdbcBatch JDBC 批量配置
     */
    public void setJdbcBatch(JdbcBatch jdbcBatch) {
        this.jdbcBatch = jdbcBatch;
    }

    /**
     * 设置 Micrometer 指标配置。
     *
     * @param metrics 指标配置
     */
    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * 逻辑删除过滤配置。
     */
    public static class LogicDelete {

        /**
         * 创建默认逻辑删除配置。
         */
        public LogicDelete() {
        }

        /**
         * 是否启用逻辑删除过滤。
         *
         * <p>
         * 默认值：true。
         * </p>
         */
        private boolean enabled = true;

        /**
         * 获取是否启用逻辑删除过滤。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用逻辑删除过滤。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 字段加密配置。
     */
    public static class Encryption {

        /**
         * 创建默认加密配置。
         */
        public Encryption() {
        }

        /**
         * 是否启用字段加密。
         *
         * <p>
         * 默认值：true。关闭后标注加密转换器的字段按明文读写。
         * </p>
         */
        private boolean enabled = true;

        /**
         * 加密密钥。
         *
         * <p>
         * 任意字符串，启动时经 SHA-256 派生为 256 位密钥。
         * 未配置时使用加密转换器的字段会在读写时抛出异常。
         * </p>
         */
        private String key;

        /**
         * 多密钥配置，用于密钥轮换。
         *
         * <p>
         * 键为密钥编号，值为密钥字符串。配置后新数据使用
         * {@link #activeKeyId} 对应的密钥加密并在密文前加编号前缀，
         * 旧编号的密文仍可正常解密。
         * </p>
         */
        private Map<String, String> keys = new LinkedHashMap<>();

        /**
         * 当前加密使用的密钥编号。
         *
         * <p>
         * 配置多把密钥时必须指定；未配置且只有一把密钥时自动使用
         * 该密钥。默认值：空。
         * </p>
         */
        private String activeKeyId;

        /**
         * 获取是否启用字段加密。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用字段加密。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取加密密钥。
         *
         * @return 加密密钥；未配置时返回 {@code null}
         */
        public String getKey() {
            return key;
        }

        /**
         * 设置加密密钥。
         *
         * @param key 加密密钥
         */
        public void setKey(String key) {
            this.key = key;
        }

        /**
         * 获取多密钥配置。
         *
         * @return 密钥编号到密钥字符串的映射
         */
        public Map<String, String> getKeys() {
            return keys;
        }

        /**
         * 设置多密钥配置。
         *
         * @param keys 密钥编号到密钥字符串的映射
         */
        public void setKeys(Map<String, String> keys) {
            this.keys = keys;
        }

        /**
         * 获取当前加密使用的密钥编号。
         *
         * @return 密钥编号；未配置时返回 {@code null}
         */
        public String getActiveKeyId() {
            return activeKeyId;
        }

        /**
         * 设置当前加密使用的密钥编号。
         *
         * @param activeKeyId 密钥编号
         */
        public void setActiveKeyId(String activeKeyId) {
            this.activeKeyId = activeKeyId;
        }
    }

    /**
     * 慢 SQL 日志配置。
     *
     * <p>
     * 开启后通过 Hibernate 内置的慢查询阈值记录执行时间超过阈值的
     * SQL，日志级别由 {@code org.hibernate.SQL_SLOW} 控制。
     * </p>
     */
    public static class SlowSql {

        /**
         * 创建默认慢 SQL 配置。
         */
        public SlowSql() {
        }

        /**
         * 是否启用慢 SQL 日志。
         *
         * <p>
         * 默认值：false。开启后对 JDBC 语句有少量计时开销。
         * </p>
         */
        private boolean enabled = false;

        /**
         * 慢 SQL 阈值，单位毫秒。
         *
         * <p>
         * 默认值：1000。执行时间大于该值的语句会被记录。
         * </p>
         */
        private long thresholdMs = 1000L;

        /**
         * 获取是否启用慢 SQL 日志。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用慢 SQL 日志。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取慢 SQL 阈值。
         *
         * @return 阈值，单位毫秒
         */
        public long getThresholdMs() {
            return thresholdMs;
        }

        /**
         * 设置慢 SQL 阈值。
         *
         * @param thresholdMs 阈值，单位毫秒，必须大于 0
         * @throws IllegalArgumentException 阈值不大于 0 时抛出
         */
        public void setThresholdMs(long thresholdMs) {
            if (thresholdMs <= 0) {
                throw new IllegalArgumentException(
                        "neopen.jpa.slow-sql.threshold-ms must be "
                                + "greater than zero"
                );
            }

            this.thresholdMs = thresholdMs;
        }
    }

    /**
     * Hibernate JDBC 批量配置。
     *
     * <p>
     * 开启后自动设置 {@code hibernate.jdbc.batch_size}、
     * {@code hibernate.order_inserts} 与
     * {@code hibernate.order_updates}，无需再手工维护
     * {@code spring.jpa.properties}。
     * </p>
     */
    public static class JdbcBatch {

        /**
         * 创建默认 JDBC 批量配置。
         */
        public JdbcBatch() {
        }

        /**
         * 是否启用 JDBC 批量自动配置。
         *
         * <p>
         * 默认值：false。关闭时不覆盖任何 Hibernate 批量参数。
         * </p>
         */
        private boolean enabled = false;

        /**
         * JDBC 批量大小。
         *
         * <p>
         * 默认值：50。对应 {@code hibernate.jdbc.batch_size}。
         * </p>
         */
        private int batchSize = 50;

        /**
         * 是否对同类型插入语句排序。
         *
         * <p>
         * 默认值：true。对应 {@code hibernate.order_inserts}。
         * </p>
         */
        private boolean orderInserts = true;

        /**
         * 是否对同类型更新语句排序。
         *
         * <p>
         * 默认值：true。对应 {@code hibernate.order_updates}。
         * </p>
         */
        private boolean orderUpdates = true;

        /**
         * 获取是否启用 JDBC 批量自动配置。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 JDBC 批量自动配置。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取 JDBC 批量大小。
         *
         * @return 批量大小
         */
        public int getBatchSize() {
            return batchSize;
        }

        /**
         * 设置 JDBC 批量大小。
         *
         * @param batchSize 批量大小，必须大于 0
         * @throws IllegalArgumentException 批量大小不大于 0 时抛出
         */
        public void setBatchSize(int batchSize) {
            if (batchSize <= 0) {
                throw new IllegalArgumentException(
                        "neopen.jpa.jdbc-batch.batch-size must be "
                                + "greater than zero"
                );
            }

            this.batchSize = batchSize;
        }

        /**
         * 获取是否排序插入语句。
         *
         * @return 排序时返回 {@code true}
         */
        public boolean isOrderInserts() {
            return orderInserts;
        }

        /**
         * 设置是否排序插入语句。
         *
         * @param orderInserts 是否排序
         */
        public void setOrderInserts(boolean orderInserts) {
            this.orderInserts = orderInserts;
        }

        /**
         * 获取是否排序更新语句。
         *
         * @return 排序时返回 {@code true}
         */
        public boolean isOrderUpdates() {
            return orderUpdates;
        }

        /**
         * 设置是否排序更新语句。
         *
         * @param orderUpdates 是否排序
         */
        public void setOrderUpdates(boolean orderUpdates) {
            this.orderUpdates = orderUpdates;
        }
    }

    /**
     * Micrometer 指标配置。
     *
     * <p>
     * 开启后需要容器中存在 {@code MeterRegistry}，并自动注册数据源
     * 路由次数与慢 SQL 次数两个计数器。
     * </p>
     */
    public static class Metrics {

        /**
         * 创建默认指标配置。
         */
        public Metrics() {
        }

        /**
         * 是否启用指标采集。
         *
         * <p>
         * 默认值：false。
         * </p>
         */
        private boolean enabled = false;

        /**
         * SQL 超过该毫秒数时计入慢 SQL。
         *
         * <p>
         * 默认值：1000。
         * </p>
         */
        private long slowThresholdMs = 1000L;

        /**
         * 获取是否启用指标采集。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用指标采集。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取慢 SQL 阈值。
         *
         * @return 阈值毫秒数
         */
        public long getSlowThresholdMs() {
            return slowThresholdMs;
        }

        /**
         * 设置慢 SQL 阈值。
         *
         * @param slowThresholdMs 阈值毫秒数，必须大于 0
         * @throws IllegalArgumentException 阈值不大于 0 时抛出
         */
        public void setSlowThresholdMs(long slowThresholdMs) {
            if (slowThresholdMs <= 0) {
                throw new IllegalArgumentException(
                        "neopen.jpa.metrics.slow-threshold-ms must be "
                                + "greater than zero"
                );
            }

            this.slowThresholdMs = slowThresholdMs;
        }
    }

    /**
     * 审计字段填充配置。
     */
    public static class Audit {

        /**
         * 创建默认审计配置。
         */
        public Audit() {
        }

        /**
         * 是否启用审计字段填充。
         *
         * <p>
         * 默认值：true。
         * </p>
         */
        private boolean enabled = true;

        /**
         * 固定的操作人名称。
         *
         * <p>
         * 配置后写入 {@code @CreatedBy} 与 {@code @UpdatedBy} 字段；
         * 未配置时保留实体原值。默认值：空。
         * </p>
         */
        private String auditor;

        /**
         * 审计时区。
         *
         * <p>
         * 默认值：UTC。写入 {@code LocalDateTime}、
         * {@code ZonedDateTime} 等字段时使用该时区。
         * </p>
         */
        private String zone = "UTC";

        /**
         * 是否启用变更历史。
         *
         * <p>
         * 默认值：true。只有标注 {@code @Audited} 并注册
         * {@code AuditLogPublisher} 的实体会产生记录。
         * </p>
         */
        private boolean trailEnabled = true;

        /**
         * 获取是否启用审计字段填充。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用审计字段填充。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取固定操作人名称。
         *
         * @return 操作人名称；未配置时返回 {@code null}
         */
        public String getAuditor() {
            return auditor;
        }

        /**
         * 设置固定操作人名称。
         *
         * @param auditor 操作人名称
         */
        public void setAuditor(String auditor) {
            this.auditor = auditor;
        }

        /**
         * 获取审计时区。
         *
         * @return 时区编号
         */
        public String getZone() {
            return zone;
        }

        /**
         * 设置审计时区。
         *
         * @param zone 时区编号，例如 {@code Asia/Shanghai}
         */
        public void setZone(String zone) {
            this.zone = zone;
        }

        /**
         * 获取是否启用变更历史。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isTrailEnabled() {
            return trailEnabled;
        }

        /**
         * 设置是否启用变更历史。
         *
         * @param trailEnabled 是否启用
         */
        public void setTrailEnabled(boolean trailEnabled) {
            this.trailEnabled = trailEnabled;
        }
    }

    /**
     * 多租户配置。
     */
    public static class Tenant {

        /**
         * 创建默认多租户配置。
         */
        public Tenant() {
        }

        /**
         * 是否启用租户过滤。
         *
         * <p>
         * 默认值：true。只有实体声明 {@code @TenantId} 字段时才会
         * 追加租户条件。
         * </p>
         */
        private boolean enabled = true;

        /**
         * 获取是否启用租户过滤。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用租户过滤。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 行级数据权限配置。
     */
    public static class DataScope {

        /**
         * 创建默认数据权限配置。
         */
        public DataScope() {
        }

        /**
         * 是否启用行级数据权限。
         *
         * <p>
         * 默认值：true。只有注册 {@code DataPermissionProvider}
         * 并返回非空规则时才会限制数据范围。
         * </p>
         */
        private boolean enabled = true;

        /**
         * 获取是否启用行级数据权限。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用行级数据权限。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 动态数据源配置。
     *
     * <p>
     * 开启后会注册 {@code jpaPlusRoutingDataSource} 作为主数据源，
     * 逻辑名称通过 {@link #targets} 映射到容器中的数据源 Bean。
     * </p>
     */
    public static class Datasource {

        /**
         * 创建默认动态数据源配置。
         */
        public Datasource() {
        }

        /**
         * 是否启用动态数据源。
         *
         * <p>
         * 默认值：false。开启后必须先定义目标数据源 Bean，
         * 否则启动时无法建立连接。
         * </p>
         */
        private boolean enabled = false;

        /**
         * 默认数据源逻辑名称。
         *
         * <p>
         * 默认值：{@code primary}。未指定目标时使用该数据源。
         * </p>
         */
        private String primary = "primary";

        /**
         * 只读数据源逻辑名称。
         *
         * <p>
         * 默认值：{@code read}。{@code @DataSource(readOnly = true)}
         * 未指定名称时使用该数据源。
         * </p>
         */
        private String readTarget = "read";

        /**
         * 读写数据源逻辑名称。
         *
         * <p>
         * 默认值：{@code write}。
         * {@code @DataSource(readOnly = false)} 未指定名称时使用该数据源。
         * </p>
         */
        private String writeTarget = "write";

        /**
         * 逻辑名称到数据源 Bean 名称的映射。
         *
         * <p>
         * 默认值：空。例如 {@code primary: dataSourcePrimary}。
         * </p>
         */
        private Map<String, String> targets = new LinkedHashMap<>();

        /**
         * 获取是否启用动态数据源。
         *
         * @return 启用时返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用动态数据源。
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取默认数据源逻辑名称。
         *
         * @return 默认数据源逻辑名称
         */
        public String getPrimary() {
            return primary;
        }

        /**
         * 设置默认数据源逻辑名称。
         *
         * @param primary 默认数据源逻辑名称
         */
        public void setPrimary(String primary) {
            this.primary = primary;
        }

        /**
         * 获取只读数据源逻辑名称。
         *
         * @return 只读数据源逻辑名称
         */
        public String getReadTarget() {
            return readTarget;
        }

        /**
         * 设置只读数据源逻辑名称。
         *
         * @param readTarget 只读数据源逻辑名称
         */
        public void setReadTarget(String readTarget) {
            this.readTarget = readTarget;
        }

        /**
         * 获取读写数据源逻辑名称。
         *
         * @return 读写数据源逻辑名称
         */
        public String getWriteTarget() {
            return writeTarget;
        }

        /**
         * 设置读写数据源逻辑名称。
         *
         * @param writeTarget 读写数据源逻辑名称
         */
        public void setWriteTarget(String writeTarget) {
            this.writeTarget = writeTarget;
        }

        /**
         * 获取逻辑名称到数据源 Bean 名称的映射。
         *
         * @return 逻辑名称到 Bean 名称的映射
         */
        public Map<String, String> getTargets() {
            return targets;
        }

        /**
         * 设置逻辑名称到数据源 Bean 名称的映射。
         *
         * @param targets 逻辑名称到 Bean 名称的映射
         */
        public void setTargets(Map<String, String> targets) {
            this.targets = targets;
        }
    }

    /**
     * 单个数据源作用域的开关配置。
     *
     * <p>
     * 所有字段默认 {@code null}，表示沿用全局配置；只有显式配置的
     * 项才会覆盖全局值。绑定示例：
     * </p>
     *
     * <pre>{@code
     * neopen:
     *   jpa:
     *     scopes:
     *       secondary:
     *         batch-size: 200
     *         logic-delete-enabled: false
     *         audit-enabled: false
     *         encryption-keys:
     *           v1: secondary-secret
     * }</pre>
     */
    public static class Scope {

        /**
         * 创建默认作用域配置。
         */
        public Scope() {
        }

        /**
         * 该作用域的批次大小。
         *
         * <p>
         * 默认值：{@code null}，沿用全局 {@code batch-size}。
         * </p>
         */
        private Integer batchSize;

        /**
         * 该作用域是否启用逻辑删除过滤。
         *
         * <p>
         * 默认值：{@code null}，沿用全局配置。
         * </p>
         */
        private Boolean logicDeleteEnabled;

        /**
         * 该作用域是否启用审计字段填充。
         *
         * <p>
         * 默认值：{@code null}，沿用全局配置。
         * </p>
         */
        private Boolean auditEnabled;

        /**
         * 该作用域使用的固定操作人。
         *
         * <p>
         * 默认值：{@code null}，沿用全局操作人或容器中的
         * {@code AuditorProvider}。
         * </p>
         */
        private String auditAuditor;

        /**
         * 该作用域是否启用字段加密。
         *
         * <p>
         * 默认值：{@code null}，沿用全局配置。
         * </p>
         */
        private Boolean encryptionEnabled;

        /**
         * 该作用域的加密密钥。
         *
         * <p>
         * 默认值：{@code null}，沿用全局密钥。
         * </p>
         */
        private String encryptionKey;

        /**
         * 该作用域的多密钥配置。
         *
         * <p>
         * 键为密钥编号，值为密钥字符串。默认值：空。
         * </p>
         */
        private Map<String, String> encryptionKeys = new LinkedHashMap<>();

        /**
         * 该作用域当前加密使用的密钥编号。
         *
         * <p>
         * 默认值：空。
         * </p>
         */
        private String encryptionActiveKeyId;

        /**
         * 该作用域是否启用租户过滤。
         *
         * <p>
         * 默认值：{@code null}，沿用全局配置。
         * </p>
         */
        private Boolean tenantEnabled;

        /**
         * 该作用域固定的租户。
         *
         * <p>
         * 配置后忽略全局租户提供者，适用于归档、运维等场景。
         * 默认值：空。
         * </p>
         */
        private String tenant;

        /**
         * 该作用域是否启用变更历史。
         *
         * <p>
         * 默认值：{@code null}，沿用全局配置。
         * </p>
         */
        private Boolean trailEnabled;

        /**
         * 获取该作用域的批次大小。
         *
         * @return 批次大小；未配置时返回 {@code null}
         */
        public Integer getBatchSize() {
            return batchSize;
        }

        /**
         * 设置该作用域的批次大小。
         *
         * @param batchSize 批次大小，必须大于 0
         * @throws IllegalArgumentException 批次大小不大于 0
         */
        public void setBatchSize(Integer batchSize) {
            if (batchSize != null && batchSize <= 0) {
                throw new IllegalArgumentException(
                        "neopen.jpa.scopes.*.batch-size must be "
                                + "greater than zero"
                );
            }

            this.batchSize = batchSize;
        }

        /**
         * 获取该作用域是否启用逻辑删除过滤。
         *
         * @return 是否启用；未配置时返回 {@code null}
         */
        public Boolean getLogicDeleteEnabled() {
            return logicDeleteEnabled;
        }

        /**
         * 设置该作用域是否启用逻辑删除过滤。
         *
         * @param logicDeleteEnabled 是否启用
         */
        public void setLogicDeleteEnabled(Boolean logicDeleteEnabled) {
            this.logicDeleteEnabled = logicDeleteEnabled;
        }

        /**
         * 获取该作用域是否启用审计字段填充。
         *
         * @return 是否启用；未配置时返回 {@code null}
         */
        public Boolean getAuditEnabled() {
            return auditEnabled;
        }

        /**
         * 设置该作用域是否启用审计字段填充。
         *
         * @param auditEnabled 是否启用
         */
        public void setAuditEnabled(Boolean auditEnabled) {
            this.auditEnabled = auditEnabled;
        }

        /**
         * 获取该作用域的固定操作人。
         *
         * @return 操作人名称；未配置时返回 {@code null}
         */
        public String getAuditAuditor() {
            return auditAuditor;
        }

        /**
         * 设置该作用域的固定操作人。
         *
         * @param auditAuditor 操作人名称
         */
        public void setAuditAuditor(String auditAuditor) {
            this.auditAuditor = auditAuditor;
        }

        /**
         * 获取该作用域是否启用字段加密。
         *
         * @return 是否启用；未配置时返回 {@code null}
         */
        public Boolean getEncryptionEnabled() {
            return encryptionEnabled;
        }

        /**
         * 设置该作用域是否启用字段加密。
         *
         * @param encryptionEnabled 是否启用
         */
        public void setEncryptionEnabled(Boolean encryptionEnabled) {
            this.encryptionEnabled = encryptionEnabled;
        }

        /**
         * 获取该作用域的加密密钥。
         *
         * @return 加密密钥；未配置时返回 {@code null}
         */
        public String getEncryptionKey() {
            return encryptionKey;
        }

        /**
         * 设置该作用域的加密密钥。
         *
         * @param encryptionKey 加密密钥
         */
        public void setEncryptionKey(String encryptionKey) {
            this.encryptionKey = encryptionKey;
        }

        /**
         * 获取该作用域的多密钥配置。
         *
         * @return 密钥编号到密钥字符串的映射
         */
        public Map<String, String> getEncryptionKeys() {
            return encryptionKeys;
        }

        /**
         * 设置该作用域的多密钥配置。
         *
         * @param encryptionKeys 密钥编号到密钥字符串的映射
         */
        public void setEncryptionKeys(Map<String, String> encryptionKeys) {
            this.encryptionKeys = encryptionKeys;
        }

        /**
         * 获取该作用域当前加密使用的密钥编号。
         *
         * @return 密钥编号；未配置时返回 {@code null}
         */
        public String getEncryptionActiveKeyId() {
            return encryptionActiveKeyId;
        }

        /**
         * 设置该作用域当前加密使用的密钥编号。
         *
         * @param encryptionActiveKeyId 密钥编号
         */
        public void setEncryptionActiveKeyId(String encryptionActiveKeyId) {
            this.encryptionActiveKeyId = encryptionActiveKeyId;
        }

        /**
         * 获取该作用域是否启用租户过滤。
         *
         * @return 是否启用；未配置时返回 {@code null}
         */
        public Boolean getTenantEnabled() {
            return tenantEnabled;
        }

        /**
         * 设置该作用域是否启用租户过滤。
         *
         * @param tenantEnabled 是否启用
         */
        public void setTenantEnabled(Boolean tenantEnabled) {
            this.tenantEnabled = tenantEnabled;
        }

        /**
         * 获取该作用域固定的租户。
         *
         * @return 固定租户；未配置时返回 {@code null}
         */
        public String getTenant() {
            return tenant;
        }

        /**
         * 设置该作用域固定的租户。
         *
         * @param tenant 固定租户
         */
        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        /**
         * 获取该作用域是否启用变更历史。
         *
         * @return 是否启用；未配置时返回 {@code null}
         */
        public Boolean getTrailEnabled() {
            return trailEnabled;
        }

        /**
         * 设置该作用域是否启用变更历史。
         *
         * @param trailEnabled 是否启用
         */
        public void setTrailEnabled(Boolean trailEnabled) {
            this.trailEnabled = trailEnabled;
        }
    }
}
