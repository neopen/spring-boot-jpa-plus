package io.github.neopen.autoconfigure;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Locale;

/**
 * JPA Plus 的 Micrometer 指标。
 *
 * <p>提供两个计数器：</p>
 * <ul>
 *     <li>{@code jpa.plus.datasource.routes}：按目标数据源统计路由
 *     次数，标签为 {@code target}；</li>
 *     <li>{@code jpa.plus.sql.slow}：按语句类型统计慢 SQL 次数，
 *     标签为 {@code type}（SELECT、INSERT 等）。</li>
 * </ul>
 *
 * <p>仅在 {@code neopen.jpa.metrics.enabled=true} 且容器中存在
 * {@link MeterRegistry} 时注册，未引入 Micrometer 的项目不受影响。</p>
 *
 * @author neopen
 * @see JpaPlusMetricsAutoConfiguration
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class JpaPlusMetrics {

    /** 数据源路由次数指标名。 */
    static final String ROUTE_METRIC = "jpa.plus.datasource.routes";

    /** 慢 SQL 次数指标名。 */
    static final String SLOW_SQL_METRIC = "jpa.plus.sql.slow";

    /** 默认数据源标签值。 */
    private static final String DEFAULT_TARGET = "default";

    /** 未知语句类型。 */
    private static final String UNKNOWN_TYPE = "UNKNOWN";

    /** 指标注册表。 */
    private final MeterRegistry registry;

    /**
     * 创建指标收集器。
     *
     * @param registry 指标注册表，不能为 {@code null}
     * @throws IllegalArgumentException 注册表为空时抛出
     */
    public JpaPlusMetrics(MeterRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException(
                    "MeterRegistry must not be null"
            );
        }

        this.registry = registry;
    }

    /**
     * 记录一次数据源路由。
     *
     * @param target 目标数据源逻辑名称，允许为 {@code null}
     */
    public void recordRoute(String target) {
        String tag = target == null || target.isBlank()
                ? DEFAULT_TARGET
                : target;

        Counter.builder(ROUTE_METRIC)
                .description("Data source routing count")
                .tag("target", tag)
                .register(registry)
                .increment();
    }

    /**
     * 记录一次慢 SQL。
     *
     * @param sql 慢 SQL 语句，允许为 {@code null}
     */
    public void recordSlowSql(String sql) {
        Counter.builder(SLOW_SQL_METRIC)
                .description("Slow SQL count")
                .tag("type", statementType(sql))
                .register(registry)
                .increment();
    }

    /**
     * 解析 SQL 语句类型。
     *
     * @param sql SQL 语句，允许为 {@code null}
     * @return 语句类型，无法解析时返回 {@code UNKNOWN}
     */
    static String statementType(String sql) {
        if (sql == null || sql.isBlank()) {
            return UNKNOWN_TYPE;
        }

        String trimmed = sql.strip();
        int end = 0;

        while (end < trimmed.length()
                && Character.isLetter(trimmed.charAt(end))) {
            end++;
        }

        return end == 0
                ? UNKNOWN_TYPE
                : trimmed.substring(0, end).toUpperCase(Locale.ROOT);
    }
}
