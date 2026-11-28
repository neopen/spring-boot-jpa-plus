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
package io.github.neopen.core.persistence.mapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型属性元数据。
 *
 * <p>保存一个类型的全部可映射属性，并维护规范化名称到属性的索引。
 * 规范化名称忽略大小写与下划线，同时包含 JPA 列名别名。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class BeanMetadata {

    /** 类型。 */
    private final Class<?> type;

    /** 按声明顺序排列的属性。 */
    private final List<PropertyAccessor> properties;

    /** 规范化名称到属性的索引。 */
    private final Map<String, PropertyAccessor> byName;

    /**
     * 创建类型元数据。
     *
     * @param type       类型
     * @param properties 按声明顺序排列的属性
     * @param byName     规范化名称到属性的索引
     */
    BeanMetadata(
            Class<?> type,
            List<PropertyAccessor> properties,
            Map<String, PropertyAccessor> byName
    ) {
        this.type = type;
        this.properties = List.copyOf(properties);
        this.byName = Collections.unmodifiableMap(
                new LinkedHashMap<>(byName)
        );
    }

    /**
     * 获取类型。
     *
     * @return 类型
     */
    public Class<?> type() {
        return type;
    }

    /**
     * 获取按声明顺序排列的属性。
     *
     * @return 不可修改的属性列表
     */
    public List<PropertyAccessor> properties() {
        return properties;
    }

    /**
     * 按名称查找属性。
     *
     * <p>匹配时忽略大小写与下划线，并支持 JPA 列名别名。</p>
     *
     * @param name 属性名
     * @return 属性访问器；未匹配时返回 {@code null}
     */
    public PropertyAccessor property(String name) {
        if (name == null) {
            return null;
        }

        return byName.get(BeanIntrospector.normalize(name));
    }
}
