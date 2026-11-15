package io.github.neopen.autoconfigure;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.Map;

/**
 * 慢 SQL 日志的 Hibernate 属性定制器。
 *
 * <p>通过 Hibernate 内置的慢查询阈值记录执行时间超过阈值的 SQL。
 * 开启后 Hibernate 会在 {@code org.hibernate.SQL_SLOW} 日志器上输出
 * 执行耗时和 SQL 原文，日志级别设为 {@code WARN} 即可看到慢查询。</p>
 *
 * <p>Hibernate 只输出 SQL 原文，不包含绑定参数；需要参数时请同时
 * 开启 {@code hibernate.show_sql} 或 SQL 日志器。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class SlowSqlHibernateCustomizer
        implements HibernatePropertiesCustomizer {

    /** Hibernate 慢查询阈值属性名。 */
    static final String SLOW_QUERY_PROPERTY =
            "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS";

    /** 慢 SQL 阈值，单位毫秒。 */
    private final long thresholdMs;

    /**
     * 创建慢 SQL 属性定制器。
     *
     * @param thresholdMs 阈值，单位毫秒，必须大于 0
     * @throws IllegalArgumentException 阈值不大于 0 时抛出
     */
    public SlowSqlHibernateCustomizer(long thresholdMs) {
        if (thresholdMs <= 0) {
            throw new IllegalArgumentException(
                    "Slow SQL threshold must be greater than zero"
            );
        }

        this.thresholdMs = thresholdMs;
    }

    /**
     * 写入 Hibernate 慢查询阈值。
     *
     * @param hibernateProperties Hibernate 属性
     */
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(SLOW_QUERY_PROPERTY, thresholdMs);
    }
}
