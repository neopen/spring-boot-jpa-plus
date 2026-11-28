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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 类型属性内省器。
 *
 * <p>扫描类型及其父类的可映射字段，构建 {@link BeanMetadata} 并按类型缓存。
 * 缓存使用 {@link ClassValue}，不会阻止类型被卸载，适合热部署环境。</p>
 *
 * <p>属性索引同时注册字段名与 JPA 列名别名，例如字段 {@code number}
 * 标注 {@code @Column(name = "order_no")} 后，{@code order_no} 也能命中。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class BeanIntrospector {

    /** 类型元数据缓存。 */
    private static final ClassValue<BeanMetadata> CACHE =
            new ClassValue<>() {

                /**
                 * 构建类型元数据。
                 *
                 * @param type 类型
                 * @return 类型元数据
                 */
                @Override
                protected BeanMetadata computeValue(Class<?> type) {
                    return scan(type);
                }
            };

    /** 工具类不允许实例化。 */
    private BeanIntrospector() {
    }

    /**
     * 获取类型元数据。
     *
     * @param type 类型，不能为 {@code null}
     * @return 类型元数据
     * @throws NullPointerException 类型为 {@code null}
     */
    public static BeanMetadata metadata(Class<?> type) {
        return CACHE.get(Objects.requireNonNull(
                type,
                "Type must not be null"
        ));
    }

    /**
     * 规范化名称，忽略大小写与下划线。
     *
     * @param name 名称
     * @return 规范化后的名称
     */
    public static String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 判断字段是否参与映射。
     *
     * @param field 字段
     * @return 参与映射时返回 {@code true}
     */
    static boolean isMappable(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && !Modifier.isTransient(field.getModifiers())
                && !field.isSynthetic();
    }

    /**
     * 扫描类型及其父类的可映射字段。
     *
     * @param type 类型
     * @return 类型元数据
     */
    private static BeanMetadata scan(Class<?> type) {
        List<PropertyAccessor> properties = new ArrayList<>();
        Map<String, PropertyAccessor> byName = new LinkedHashMap<>();

        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {

            for (Field field : current.getDeclaredFields()) {
                if (!isMappable(field)) {
                    continue;
                }

                PropertyAccessor accessor = PropertyAccessor.create(field);

                properties.add(accessor);

                byName.putIfAbsent(
                        normalize(accessor.name()),
                        accessor
                );

                for (String alias : accessor.columnAliases()) {
                    byName.putIfAbsent(normalize(alias), accessor);
                }
            }
        }

        return new BeanMetadata(type, properties, byName);
    }
}
