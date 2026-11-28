/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
package io.github.neopen.convert.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明参数中的外键字段映射为实体的关联对象。
 *
 * <p>标注后，参数中的 ID 会通过 {@link jakarta.persistence.EntityManager#getReference}
 * 转为关联实体引用，避免为了设置外键而额外查询整行数据。</p>
 *
 * <pre>{@code
 * public class OrderParam {
 *
 *     @ParamReference("customer")
 *     private Long customerId;
 * }
 *
 * Order order = ParamMapper.toEntity(param, Order.class, entityManager);
 * }</pre>
 *
 * <p>使用该注解时必须提供 {@link jakarta.persistence.EntityManager}，
 * 否则映射会抛出异常。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamReference {

    /**
     * 目标关联字段名。
     *
     * <p>为空时使用参数字段自身的名称。</p>
     *
     * @return 目标关联字段名
     */
    String value() default "";
}
