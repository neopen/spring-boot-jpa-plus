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
package io.github.neopen.core.persistence.audit.trail;

/**
 * 变更历史发布者。
 *
 * <p>Spring Boot 环境注册该接口的 Bean 即可，自动配置会把它接入
 * {@link AuditTrails}。典型实现是把记录写入审计表或消息队列。</p>
 *
 * <pre>{@code
 * @Bean
 * public AuditLogPublisher auditLogPublisher(AuditLogRepository repo) {
 *     return entry -> repo.save(AuditLog.from(entry));
 * }
 * }</pre>
 *
 * @author neopen
 * @see AuditEntry
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface AuditLogPublisher {

    /**
     * 发布一条变更历史。
     *
     * @param entry 变更历史
     */
    void publish(AuditEntry entry);
}
