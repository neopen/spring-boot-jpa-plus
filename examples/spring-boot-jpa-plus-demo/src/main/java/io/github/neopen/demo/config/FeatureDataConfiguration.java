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
package io.github.neopen.demo.config;

import io.github.neopen.core.persistence.audit.trail.AsyncAuditLogPublisher;
import io.github.neopen.core.persistence.audit.trail.AuditLogPublisher;
import io.github.neopen.core.persistence.datascope.DataPermissionProvider;
import io.github.neopen.core.persistence.datascope.DataRule;
import io.github.neopen.core.persistence.tenant.TenantProvider;
import io.github.neopen.demo.bean.entity.DocumentRecord;
import io.github.neopen.demo.support.DemoAuditStore;
import io.github.neopen.demo.support.DemoContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
     * <p>使用异步发布者避免写入审计存储时阻塞业务事务，
     * 容器关闭时由 {@code destroyMethod} 等待队列处理完毕。</p>
     *
     * @return 异步变更历史发布者
     */
    @Bean(destroyMethod = "close")
    public AuditLogPublisher demoAuditLogPublisher() {
        return new AsyncAuditLogPublisher(DemoAuditStore::add, 256);
    }

    /**
     * 演示用指标注册表。
     *
     * <p>真实项目引入 Actuator 后由 Spring Boot 自动提供；
     * 未引入时手动注册一个简单实现即可让指标生效。</p>
     *
     * @return 简单指标注册表
     */
    @Bean
    public MeterRegistry demoMeterRegistry() {
        return new SimpleMeterRegistry();
    }
}
