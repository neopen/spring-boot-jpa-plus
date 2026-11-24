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

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * JPA Plus 指标自动配置。
 *
 * <p>仅在 {@code neopen.jpa.metrics.enabled=true} 且类路径中存在
 * Micrometer 时生效。注册 {@link JpaPlusMetrics} 与数据源后处理器，
 * 不使用指标功能时不会引入任何额外依赖。</p>
 *
 * @author neopen
 * @see JpaPlusMetrics
 * @see SlowSqlMetricsDataSource
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@AutoConfiguration
@ConditionalOnClass({MeterRegistry.class, DataSource.class})
@ConditionalOnProperty(
        prefix = "neopen.jpa.metrics",
        name = "enabled",
        havingValue = "true"
)
@EnableConfigurationProperties(JpaPlusProperties.class)
public class JpaPlusMetricsAutoConfiguration {

    /**
     * 创建指标自动配置。
     */
    public JpaPlusMetricsAutoConfiguration() {
    }

    /**
     * 注册指标收集器。
     *
     * @param registry 指标注册表
     * @return 指标收集器
     */
    @Bean
    @ConditionalOnMissingBean
    public JpaPlusMetrics jpaPlusMetrics(MeterRegistry registry) {
        return new JpaPlusMetrics(registry);
    }

    /**
     * 注册指标后处理器。
     *
     * @param metrics    指标收集器提供者
     * @param properties JPA Plus 配置属性
     * @return 指标后处理器
     */
    @Bean
    public static JpaPlusMetricsBeanPostProcessor
            jpaPlusMetricsBeanPostProcessor(
                    ObjectProvider<JpaPlusMetrics> metrics,
                    JpaPlusProperties properties
            ) {
        return new JpaPlusMetricsBeanPostProcessor(
                metrics,
                properties.getMetrics().getSlowThresholdMs()
        );
    }
}
