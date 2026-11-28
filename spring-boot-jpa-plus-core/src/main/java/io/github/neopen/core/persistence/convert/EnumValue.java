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
package io.github.neopen.core.persistence.convert;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记枚举常量持久化到数据库时使用的值。
 *
 * <p>可标注在字段或无参方法上，字段或方法的返回值即数据库存储值。
 * 未标注时默认使用 {@link Enum#name()}。</p>
 *
 * <pre>{@code
 * public enum OrderStatus {
 *
 *     @EnumValue("PAID")
 *     PAID,
 *
 *     @EnumValue("CANCELLED")
 *     CANCELLED;
 * }
 * }</pre>
 *
 * <p>与 {@link EnumValueConverter} 配合使用时，实体字段需要显式声明
 * {@code @Convert(converter = StatusConverter.class)}。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface EnumValue {

    /**
     * 数据库存储值。
     *
     * <p>标注在枚举常量上时必须指定；标注在无参方法上时忽略该属性，
     * 直接使用方法返回值。</p>
     *
     * @return 持久化值
     */
    String value() default "";
}
