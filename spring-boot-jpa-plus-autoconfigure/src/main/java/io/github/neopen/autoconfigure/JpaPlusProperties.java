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
    }
}
