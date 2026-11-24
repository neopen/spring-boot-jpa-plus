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
package io.github.neopen.core.persistence.datasource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指定方法或类使用的数据源。
 *
 * <p>标注后，方法执行期间会把目标数据源名称绑定到当前线程，
 * 由 {@code JpaPlusRoutingDataSource} 决定实际使用的连接。
 * 注解可以标注在类上，也可以标注在方法上，方法上的配置优先。</p>
 *
 * <pre>{@code
 * @DataSource("read")
 * public List<Order> listOrders() {
 *     return orderRepository.findAll();
 * }
 *
 * @DataSource(readOnly = true)
 * public OrderDetail detail(Long id) {
 *     return orderRepository.findById(id);
 * }
 * }</pre>
 *
 * <p>数据源切换不会跨事务生效：请把注解标注在事务边界外层，
 * 否则事务开启后连接已经绑定，切换不会生效。</p>
 *
 * @author neopen
 * @see DataSourceContext
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {

    /**
     * 目标数据源名称。
     *
     * <p>对应 {@code neopen.jpa.datasource.targets} 中的逻辑名称。
     * 为空时按 {@link #readOnly()} 选择读写数据源。</p>
     *
     * @return 目标数据源名称
     */
    String value() default "";

    /**
     * 是否使用只读数据源。
     *
     * <p>{@link #value()} 为空时生效：{@code true} 使用
     * {@code neopen.jpa.datasource.read-target}，否则使用
     * {@code neopen.jpa.datasource.write-target}。</p>
     *
     * @return 是否只读
     */
    boolean readOnly() default false;
}
