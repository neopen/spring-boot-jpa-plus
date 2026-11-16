package io.github.neopen.demo.config;

import io.github.neopen.core.persistence.audit.trail.AuditLogPublisher;
import io.github.neopen.core.persistence.datascope.DataPermissionProvider;
import io.github.neopen.core.persistence.datascope.DataRule;
import io.github.neopen.core.persistence.tenant.TenantProvider;
import io.github.neopen.demo.bean.entity.DocumentRecord;
import io.github.neopen.demo.support.DemoAuditStore;
import io.github.neopen.demo.support.DemoContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 0.6 能力演示配置。
 *
 * <p>注册租户、数据权限与变更历史三个 SPI，自动配置会把它们
 * 接入对应的静态工具。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Configuration
public class FeatureDataConfiguration {

    /**
     * 创建演示用的 JPA Plus 配置。
     */
    public FeatureDataConfiguration() {
    }

    /**
     * 租户提供者。
     *
     * @return 租户提供者
     */
    @Bean
    public TenantProvider demoTenantProvider() {
        return DemoContext::tenant;
    }

    /**
     * 行级数据权限提供者。
     *
     * @return 数据权限提供者
     */
    @Bean
    public DataPermissionProvider demoDataPermissionProvider() {
        return entityType -> {
            if (entityType != DocumentRecord.class) {
                return List.of();
            }

            Long deptId = DemoContext.deptId();

            return deptId == null
                    ? List.of()
                    : List.of(DataRule.of("deptId", deptId));
        };
    }

    /**
     * 变更历史发布者。
     *
     * @return 变更历史发布者
     */
    @Bean
    public AuditLogPublisher demoAuditLogPublisher() {
        return DemoAuditStore::add;
    }
}
