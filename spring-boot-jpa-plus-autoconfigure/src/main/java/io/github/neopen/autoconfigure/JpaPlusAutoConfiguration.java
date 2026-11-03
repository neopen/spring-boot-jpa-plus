package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

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
import org.springframework.orm.jpa.SharedEntityManagerCreator;

/**
 * Spring Boot JPA Plus 自动配置入口。
 *
 * <p>
 * 本类负责将 JPA Plus 的默认组件注册到 Spring IoC 容器。
 * 业务项目引入 Starter 后，无需手动创建执行器。
 * </p>
 *
 * <h3>自动配置条件</h3>
 *
 * <ul>
 *     <li>项目中存在 Jakarta Persistence API。</li>
 *     <li>项目中存在 JPA Plus Core。</li>
 *     <li>neopen.jpa.enabled 不为 false。</li>
 *     <li>Spring Boot 已经创建 EntityManagerFactory。</li>
 * </ul>
 *
 * <h3>自动配置顺序</h3>
 *
 * <p>
 * 通过 after 指定在 Hibernate JPA 自动配置之后执行，
 * 确保创建 EntityManagerFactory 后再初始化 JPA Plus。
 * </p>
 *
 * <h3>用户自定义 Bean</h3>
 *
 * <p>
 * 默认 Bean 使用 ConditionalOnMissingBean 或名称条件，
 * 用户可以自行提供实现来覆盖默认组件。
 * </p>
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
