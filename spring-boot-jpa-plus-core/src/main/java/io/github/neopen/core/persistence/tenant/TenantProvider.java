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
package io.github.neopen.core.persistence.tenant;

/**
 * 当前租户提供者。
 *
 * <p>Spring Boot 环境注册该接口的 Bean 即可，自动配置会把
 * 它接入 {@link Tenants}。</p>
 *
 * @author neopen
 * @see Tenants
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface TenantProvider {

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户；没有租户上下文时返回 {@code null}
     */
    String currentTenant();
}
