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
package io.github.neopen.core.persistence.datascope;

import java.util.List;

/**
 * 行级数据权限提供者。
 *
 * <p>按实体类型返回当前用户可见的数据范围。返回空列表表示该实体
 * 不限制数据范围。</p>
 *
 * <pre>{@code
 * @Bean
 * public DataPermissionProvider dataPermissionProvider() {
 *     return entityType -> LoginUser.isAdmin()
 *             ? List.of()
 *             : List.of(DataRule.of("deptId", LoginUser.deptIds()));
 * }
 * }</pre>
 *
 * @author neopen
 * @see DataScopes
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface DataPermissionProvider {

    /**
     * 获取实体的数据权限规则。
     *
     * @param entityType 实体类型
     * @return 规则列表；返回 {@code null} 或空列表表示不限制
     */
    List<DataRule> rulesFor(Class<?> entityType);
}
