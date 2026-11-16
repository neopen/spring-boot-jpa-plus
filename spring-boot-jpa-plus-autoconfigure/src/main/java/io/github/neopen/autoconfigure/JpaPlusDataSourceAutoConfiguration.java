package io.github.neopen.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;

/**
 * 动态数据源自动配置。
 *
 * <p>仅在 {@code neopen.jpa.datasource.enabled=true} 且容器中没有
 * 其它数据源时生效。注册的路由数据源取代 Spring Boot 默认数据源，
 * 目标数据源由 {@code neopen.jpa.datasource.targets} 指向业务
 * 自己声明的数据源 Bean。</p>
 *
 * <p>启用后 {@code @DataSource} 注解即可切换数据源。</p>
 *
 * @author neopen
 * @see JpaPlusRoutingDataSource
 * @see DataSourceAspect
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass({AbstractDataSource.class, DataSource.class})
@ConditionalOnProperty(
        prefix = "neopen.jpa.datasource",
        name = "enabled",
        havingValue = "true"
)
@ConditionalOnMissingBean(DataSource.class)
@EnableConfigurationProperties(JpaPlusProperties.class)
public class JpaPlusDataSourceAutoConfiguration {

    /**
     * 创建 JPA Plus 自动配置。
     */
    public JpaPlusDataSourceAutoConfiguration() {
    }

    /**
     * 注册路由数据源。
     *
     * @param applicationContext 容器
     * @param properties         JPA Plus 配置属性
     * @return 路由数据源
     */
    @Bean
    @Primary
    public JpaPlusRoutingDataSource dataSource(
            ApplicationContext applicationContext,
            JpaPlusProperties properties
    ) {
        JpaPlusProperties.Datasource datasource =
                properties.getDatasource();

        return new JpaPlusRoutingDataSource(
                applicationContext,
                datasource.getTargets(),
                datasource.getPrimary()
        );
    }

    /**
     * 注册数据源切面。
     *
     * @param properties JPA Plus 配置属性
     * @return 数据源切面
     */
    @Bean
    @ConditionalOnMissingBean(DataSourceAspect.class)
    public DataSourceAspect dataSourceAspect(JpaPlusProperties properties) {
        return new DataSourceAspect(properties.getDatasource());
    }
}
