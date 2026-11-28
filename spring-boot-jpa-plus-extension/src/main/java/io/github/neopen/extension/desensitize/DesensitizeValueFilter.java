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
package io.github.neopen.extension.desensitize;

import com.alibaba.fastjson2.filter.ValueFilter;
import io.github.neopen.extension.annotation.Desensitize;

/**
 * fastjson2 脱敏值过滤器。
 *
 * <p>序列化时自动对标注 {@link Desensitize} 的字符串字段脱敏：</p>
 *
 * <pre>{@code
 * String json = JSON.toJSONString(
 *         userView,
 *         new DesensitizeValueFilter()
 * );
 * }</pre>
 *
 * <p>过滤器只处理字符串字段，未标注注解的字段原样输出。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class DesensitizeValueFilter implements ValueFilter {

    /**
     * 处理单个字段值。
     *
     * @param object 当前对象，可能为 {@code null}
     * @param name   字段名
     * @param value  字段值
     * @return 脱敏后的值；无匹配规则时返回原值
     */
    @Override
    public Object apply(Object object, String name, Object value) {
        if (object == null
                || name == null
                || !(value instanceof String text)
                || text.isEmpty()) {
            return value;
        }

        DesensitizeType type = Desensitizes.typeOf(
                object.getClass(),
                name
        );

        return type == null ? value : type.mask(text);
    }
}
