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
 * 跳过当前方法或类执行期间的租户过滤。
 *
 * <p>标注后，方法执行期间不追加租户条件，也不会为新实体填充租户
 * 字段。适用于登录、定时任务、运维等需要跨租户访问数据的场景。</p>
 *
 * <pre>{@code
 * @IgnoreTenant
 * public List<Order> listAllTenants() {
 *     return orderRepository.findAll();
 * }
 * }</pre>
 *
 * <p>注解可以标注在类上，也可以标注在方法上；嵌套调用按层计数，
 * 内层结束后自动恢复外层的过滤行为。与
 * {@link io.github.neopen.core.persistence.datascope.IgnoreDataScope}
 * 相互独立，可以同时使用。</p>
 *
 * @author neopen
 * @see Tenants#isIgnored()
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenant {
}
