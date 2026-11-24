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
package io.github.neopen.extension.annotation;

import io.github.neopen.extension.desensitize.DesensitizeType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要脱敏输出或处理的字符串字段。
 *
 * <p>可通过 {@link io.github.neopen.extension.desensitize.Desensitizes#apply(Object)}
 * 就地脱敏对象，也可以把 {@link io.github.neopen.extension.desensitize.DesensitizeValueFilter}
 * 交给 fastjson2，在序列化 JSON 时自动脱敏。</p>
 *
 * <pre>{@code
 * public class UserView {
 *
 *     @Desensitize(DesensitizeType.NAME)
 *     private String name;
 *
 *     @Desensitize(DesensitizeType.PHONE)
 *     private String phone;
 * }
 * }</pre>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Desensitize {

    /**
     * 脱敏类型。
     *
     * @return 脱敏类型
     */
    DesensitizeType type();
}
