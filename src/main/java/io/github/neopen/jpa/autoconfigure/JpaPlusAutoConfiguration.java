package io.github.neopen.jpa.autoconfigure;

import io.github.neopen.jpa.persistence.dao.manager.EntityManagerHqlDao;
import io.github.neopen.jpa.persistence.dao.manager.EntityManagerSqlDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;

/**
 * Registers the HQL and native SQL DAO helpers when Spring Data JPA is available.
 */
@AutoConfiguration
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class, EntityManagerFactory.class})
@ConditionalOnBean(EntityManagerFactory.class)
public class JpaPlusAutoConfiguration {

    @Bean(name = "hqlDao")
    @ConditionalOnMissingBean(name = "hqlDao")
    public EntityManagerHqlDao hqlDao() {
        return new EntityManagerHqlDao();
    }

    @Bean(name = "sqlDao")
    @ConditionalOnMissingBean(name = "sqlDao")
    public EntityManagerSqlDao sqlDao() {
        return new EntityManagerSqlDao();
    }
}
