package io.github.neopen.autoconfigure;

import io.github.neopen.core.context.JpaPlusContextTaskDecorator;
import io.github.neopen.core.persistence.audit.AuditorProvider;
import io.github.neopen.core.persistence.audit.trail.AuditLogPublisher;
import io.github.neopen.core.persistence.datascope.DataPermissionProvider;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.core.persistence.tenant.TenantProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * Spring Boot JPA Plus 自动配置入口。
 *
 * <p>
 * 本类负责将 JPA Plus 的默认组件注册到 Spring IoC 容器。
 * 业务项目引入 Starter 后，无需手动创建执行器。
 * </p>
 *
 * <h2>自动配置条件</h2>
 *
 * <ul>
 *     <li>项目中存在 Jakarta Persistence API。</li>
 *     <li>项目中存在 JPA Plus Core。</li>
 *     <li>neopen.jpa.enabled 不为 false。</li>
 *     <li>Spring Boot 已经创建 EntityManagerFactory。</li>
 * </ul>
 *
 * <h2>自动配置顺序</h2>
 *
 * <p>
 * 通过 after 指定在 Hibernate JPA 自动配置之后执行，
 * 确保创建 EntityManagerFactory 后再初始化 JPA Plus。
 * </p>
 *
 * <h2>用户自定义 Bean</h2>
 *
 * <p>
 * 默认 Bean 使用 ConditionalOnMissingBean 或名称条件，
 * 用户可以自行提供实现来覆盖默认组件。
 * </p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass({
        EntityManager.class,
        JpaPlusHqlExecutor.class
})
@ConditionalOnProperty(
        prefix = "neopen.jpa",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(JpaPlusProperties.class)
public class JpaPlusAutoConfiguration {

    /**
     * 创建 JPA Plus 自动配置。
     */
    public JpaPlusAutoConfiguration() {
    }

    /**
     * 注册运行时配置器。
     *
     * <p>
     * 逻辑删除、审计和批量操作以静态工具方法提供，
     * 由该组件在启动时把配置属性写入全局设置。
     * 如果容器中存在 {@link AuditorProvider} Bean，
     * 审计字段会优先使用它获取当前操作人。
     * </p>
     *
     * @param properties       JPA Plus 配置属性
     * @param auditorProvider  操作人提供者，允许不存在
     * @param tenantProvider   租户提供者，允许不存在
     * @param permissionProvider 数据权限提供者，允许不存在
     * @param auditLogPublisher  变更历史发布者，允许不存在
     * @return 运行时配置器
     */
    @Bean
    @ConditionalOnMissingBean(JpaPlusRuntimeConfigurer.class)
    public JpaPlusRuntimeConfigurer jpaPlusRuntimeConfigurer(
            JpaPlusProperties properties,
            ObjectProvider<AuditorProvider> auditorProvider,
            ObjectProvider<TenantProvider> tenantProvider,
            ObjectProvider<DataPermissionProvider> permissionProvider,
            ObjectProvider<AuditLogPublisher> auditLogPublisher
    ) {
        return new JpaPlusRuntimeConfigurer(
                properties,
                auditorProvider.getIfUnique(),
                tenantProvider.getIfUnique(),
                permissionProvider.getIfUnique(),
                auditLogPublisher.getIfUnique()
        );
    }

    /**
     * 注册慢 SQL 日志定制器。
     *
     * <p>
     * 仅在 {@code neopen.jpa.slow-sql.enabled=true} 时生效，
     * 通过 Hibernate 内置阈值输出执行时间超过阈值的 SQL。
     * </p>
     *
     * @param properties JPA Plus 配置属性
     * @return Hibernate 属性定制器
     */
    @Bean
    @ConditionalOnMissingBean(SlowSqlHibernateCustomizer.class)
    @ConditionalOnProperty(
            prefix = "neopen.jpa.slow-sql",
            name = "enabled",
            havingValue = "true"
    )
    public SlowSqlHibernateCustomizer jpaPlusSlowSqlCustomizer(
            JpaPlusProperties properties
    ) {
        return new SlowSqlHibernateCustomizer(
                properties.getSlowSql().getThresholdMs()
        );
    }

    /**
     * 注册 Hibernate JDBC 批量定制器。
     *
     * <p>
     * 仅在 {@code neopen.jpa.jdbc-batch.enabled=true} 时生效，
     * 自动设置批量大小与语句排序开关。
     * </p>
     *
     * @param properties JPA Plus 配置属性
     * @return Hibernate 属性定制器
     */
    @Bean
    @ConditionalOnMissingBean(JdbcBatchHibernateCustomizer.class)
    @ConditionalOnProperty(
            prefix = "neopen.jpa.jdbc-batch",
            name = "enabled",
            havingValue = "true"
    )
    public JdbcBatchHibernateCustomizer jpaPlusJdbcBatchCustomizer(
            JpaPlusProperties properties
    ) {
        JpaPlusProperties.JdbcBatch jdbcBatch =
                properties.getJdbcBatch();

        return new JdbcBatchHibernateCustomizer(
                jdbcBatch.getBatchSize(),
                jdbcBatch.isOrderInserts(),
                jdbcBatch.isOrderUpdates()
        );
    }

    /**
     * 注册忽略过滤切面。
     *
     * <p>处理 {@code @IgnoreTenant} 与 {@code @IgnoreDataScope}，
     * 支持方法级与类级标注，嵌套调用按层恢复。</p>
     *
     * @return 忽略过滤切面
     */
    @Bean
    @ConditionalOnMissingBean(IgnoreFilterAspect.class)
    public IgnoreFilterAspect ignoreFilterAspect() {
        return new IgnoreFilterAspect();
    }

    /**
     * 注册跨线程上下文传播装饰器。
     *
     * <p>把提交线程的作用域、数据权限与租户过滤跳过深度、数据源
     * 路由栈传播到线程池任务，任务结束后恢复工作线程原状态。</p>
     *
     * <p>仅在 {@code neopen.jpa.context-propagation.enabled} 不为
     * {@code false} 时注册；容器中已有 {@link TaskDecorator} 时不再
     * 创建默认实现。自动配置不会修改业务已有线程池，需要业务代码
     * 主动设置该装饰器。</p>
     *
     * @return 上下文传播装饰器
     */
    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    @ConditionalOnProperty(
            prefix = "neopen.jpa.context-propagation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public TaskDecorator jpaPlusContextTaskDecorator() {
        return new JpaPlusContextTaskDecorator();
    }

    /**
     * JPA Plus 执行器相关自动配置。
     *
     * <p>
     * 采用嵌套配置类，将 JPA 环境相关 Bean
     * 与外层的全局配置属性判断分离。
     * </p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EntityManager.class)
    @ConditionalOnBean(EntityManagerFactory.class)
    static class JpaPlusExecutorConfiguration {

        /**
         * 创建 JPA Plus 专用的共享 EntityManager 代理。
         *
         * <p>
         * EntityManagerFactory 是线程安全的，
         * 但 EntityManager 本身不是线程安全的。
         * </p>
         *
         * <p>
         * 不能将一个普通 EntityManager 实例作为单例 Bean，
         * 让多个业务线程共同使用。
         * </p>
         *
         * <p>
         * SharedEntityManagerCreator 创建的是共享代理。
         * 每次调用时，它会根据当前事务上下文获取对应的
         * EntityManager，并参与 Spring 事务管理。
         * </p>
         *
         * <p>
         * 使用独立 Bean 名称 jpaPlusEntityManager，
         * 避免与业务项目自定义的 EntityManager Bean 冲突。
         * </p>
         *
         * @param entityManagerFactory Spring Boot 创建的 JPA 工厂
         * @return 共享 EntityManager 代理
         */
        @Bean(name = "jpaPlusEntityManager")
        @ConditionalOnMissingBean(name = "jpaPlusEntityManager")
        public EntityManager jpaPlusEntityManager(
                EntityManagerFactory entityManagerFactory
        ) {
            return SharedEntityManagerCreator
                    .createSharedEntityManager(entityManagerFactory);
        }

        /**
         * 注册 HQL 执行器。
         *
         * <p>
         * HQL 执行器负责实体对象查询，
         * 使用命名参数绑定，避免直接拼接查询参数。
         * </p>
         *
         * <p>
         * 如果用户已经定义自己的 JpaPlusHqlExecutor，
         * 则不再创建默认实例。
         * </p>
         *
         * @param entityManager JPA Plus 共享 EntityManager
         * @return HQL 执行器
         */
        @Bean
        @ConditionalOnMissingBean(JpaPlusHqlExecutor.class)
        @ConditionalOnProperty(
                prefix = "neopen.jpa",
                name = "hql-enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        public JpaPlusHqlExecutor jpaPlusHqlExecutor(
                @Qualifier("jpaPlusEntityManager")
                EntityManager entityManager
        ) {
            return new JpaPlusHqlExecutor(entityManager);
        }

        /**
         * 注册原生 SQL 执行器。
         *
         * <p>
         * SQL 执行器面向数据库表和字段，
         * 适用于复杂多表查询、报表以及数据库特性查询。
         * </p>
         *
         * <p>
         * 当 sql-enabled=false 时，不创建该 Bean。
         * 这不会禁止业务代码直接使用 EntityManager
         * 执行原生 SQL。
         * </p>
         *
         * @param entityManager JPA Plus 共享 EntityManager
         * @return SQL 执行器
         */
        @Bean
        @ConditionalOnMissingBean(JpaPlusSqlExecutor.class)
        @ConditionalOnProperty(
                prefix = "neopen.jpa",
                name = "sql-enabled",
                havingValue = "true",
                matchIfMissing = true
        )
        public JpaPlusSqlExecutor jpaPlusSqlExecutor(
                @Qualifier("jpaPlusEntityManager")
                EntityManager entityManager
        ) {
            return new JpaPlusSqlExecutor(entityManager);
        }
    }
}
