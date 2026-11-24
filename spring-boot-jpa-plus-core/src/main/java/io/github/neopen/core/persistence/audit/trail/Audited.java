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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要记录变更历史的实体。
 *
 * <p>与 {@code @EntityListeners(AuditTrailListener.class)} 配合使用：
 * 新增、修改、删除时生成 {@link AuditEntry} 并交给
 * {@link AuditLogPublisher}。</p>
 *
 * <pre>{@code
 * @Entity
 * @Audited
 * @EntityListeners(AuditTrailListener.class)
 * public class Order {
 * }
 * }</pre>
 *
 * @author neopen
 * @see AuditTrails
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
}
