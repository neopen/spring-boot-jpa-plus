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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体上的租户字段。
 *
 * <p>配合 {@link TenantListener} 使用：插入时自动写入当前租户，
 * 查询时由 {@link Tenants#filter(Class,
 * org.springframework.data.jpa.domain.Specification)} 自动追加租户条件。</p>
 *
 * <pre>{@code
 * @Entity
 * @EntityListeners(TenantListener.class)
 * public class Order {
 *
 *     @TenantId
 *     private String tenantId;
 * }
 * }</pre>
 *
 * <p>字段类型支持 {@link String}、{@link Long}、{@link Integer}
 * 和 {@link java.util.UUID}。</p>
 *
 * @author neopen
 * @see Tenants
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantId {
}
