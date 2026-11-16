package io.github.neopen.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

/**
 * 把指标绑定到数据源相关的 Bean。
 *
 * <p>对路由数据源设置指标收集器，对普通数据源套上
 * {@link SlowSqlMetricsDataSource}。已经在最外层的装饰数据源不会被
 * 重复包装。</p>
 *
 * @author neopen
 * @see JpaPlusMetrics
 * @see SlowSqlMetricsDataSource
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class JpaPlusMetricsBeanPostProcessor
        implements BeanPostProcessor {

    /** 指标收集器提供者。 */
    private final ObjectProvider<JpaPlusMetrics> metricsProvider;

    /** 慢 SQL 阈值毫秒数。 */
    private final long slowThresholdMs;

    /**
     * 创建指标后处理器。
     *
     * @param metricsProvider 指标收集器提供者
     * @param slowThresholdMs 慢 SQL 阈值毫秒数
     */
    public JpaPlusMetricsBeanPostProcessor(
            ObjectProvider<JpaPlusMetrics> metricsProvider,
            long slowThresholdMs
    ) {
        this.metricsProvider = metricsProvider;
        this.slowThresholdMs = slowThresholdMs;
    }

    /**
     * 为数据源绑定指标。
     *
     * @param bean     Bean 实例
     * @param beanName Bean 名称
     * @return 处理后的 Bean
     */
    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName
    ) {
        if (bean instanceof JpaPlusRoutingDataSource routing) {
            JpaPlusMetrics metrics = metricsProvider.getIfAvailable();

            if (metrics != null) {
                routing.setMetrics(metrics);
            }

            return bean;
        }

        if (bean instanceof DataSource dataSource
                && !(bean instanceof SlowSqlMetricsDataSource)) {
            JpaPlusMetrics metrics = metricsProvider.getIfAvailable();

            if (metrics == null) {
                return bean;
            }

            return new SlowSqlMetricsDataSource(
                    dataSource,
                    metrics,
                    slowThresholdMs
            );
        }

        return bean;
    }
}
