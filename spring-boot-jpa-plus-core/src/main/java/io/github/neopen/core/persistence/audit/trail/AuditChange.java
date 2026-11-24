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
 * 单个字段的变更。
 *
 * <p>新增与删除时只有一侧有值：新增时 {@code before} 为
 * {@code null}，删除时 {@code after} 为 {@code null}。</p>
 *
 * @param field  字段名
 * @param before 变更前值
 * @param after  变更后值
 * @author neopen
 * @see AuditEntry
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record AuditChange(String field, Object before, Object after) {
}
