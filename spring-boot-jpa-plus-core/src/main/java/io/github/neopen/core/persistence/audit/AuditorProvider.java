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
package io.github.neopen.core.persistence.audit;

/**
 * 当前操作人提供者 SPI。
 *
 * <p>审计字段填充时通过该接口获取操作人。业务项目可以注册一个
 * Spring Bean 接入登录用户、租户或调用链上下文：</p>
 *
 * <pre>{@code
 * @Bean
 * public AuditorProvider auditorProvider() {
 *     return () -> SecurityContext.currentUsername();
 * }
 * }</pre>
 *
 * <p>未注册 Bean 时，自动配置回退到
 * {@code neopen.jpa.audit.auditor} 配置的固定值；两者都没有时
 * 不写入操作人字段。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface AuditorProvider {

    /**
     * 获取当前操作人。
     *
     * @return 操作人名称；返回 {@code null} 或空白时不写入操作人字段
     */
    String getAuditor();
}
