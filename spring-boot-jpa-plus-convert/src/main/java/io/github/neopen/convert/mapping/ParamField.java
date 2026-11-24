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
package io.github.neopen.convert.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明请求参数字段与实体字段的映射关系。
 *
 * <p>当参数类字段名与实体字段名不一致时，在参数字段上标注该注解
 * 指定目标实体字段名；未标注时按同名字段自动匹配。</p>
 *
 * <pre>{@code
 * public class OrderParam {
 *
 *     @ParamField("orderNo")
 *     private String number;
 * }
 * }</pre>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamField {

    /**
     * 目标实体字段名。
     *
     * @return 实体字段名
     */
    String value();
}
