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
package io.github.neopen.convert.annotation;

import java.lang.annotation.*;

/**
 * 声明日期字符串的格式及校验规则。
 *
 * <p>可标注在方法参数或字段上，由日期校验切面读取。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DateStrFormat {

    /**
     * 获取日期格式。
     *
     * @return 兼容 {@link java.text.SimpleDateFormat} 的格式，例如
     *         {@code yyyy-MM-dd HH:mm:ss}；空字符串表示不校验格式
     */
	String value() default "";

    /**
     * 获取日期值是否必填。
     *
     * @return 必填时返回 {@code true}
     */
	boolean required() default false;

    /**
     * 获取是否忽略日期格式校验。
     *
     * @return 忽略格式校验时返回 {@code true}
     */
	boolean ignore() default false;
}
