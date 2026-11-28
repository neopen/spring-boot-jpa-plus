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

import io.github.neopen.core.exception.JpaPlusException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举持久化值解析工具。
 *
 * <p>支持两种声明方式，二选一：</p>
 *
 * <ul>
 *     <li>在枚举常量上标注 {@link EnumValue} 并指定持久化值；</li>
 *     <li>在枚举的无参方法上标注 {@link EnumValue}，方法返回值即持久化值。</li>
 * </ul>
 *
 * <p>都未标注时使用 {@link Enum#name()}。解析结果按枚举类型缓存，
 * 重复调用不会重复反射。</p>
 *
 * <pre>{@code
 * String value = EnumValues.toValue(OrderStatus.PAID);
 * OrderStatus status = EnumValues.fromValue(OrderStatus.class, "PAID");
 * }</pre>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class EnumValues {

    /** 枚举类型元数据缓存。 */
    private static final Map<Class<?>, EnumMetadata> CACHE =
            new ConcurrentHashMap<>();

    /** 工具类不允许实例化。 */
    private EnumValues() {
    }

    /**
     * 获取枚举常量的持久化值。
     *
     * @param constant 枚举常量，允许为 {@code null}
     * @return 持久化值；入参为 {@code null} 时返回 {@code null}
     * @throws JpaPlusException 读取 {@link EnumValue} 失败时抛出
     */
    public static String toValue(Enum<?> constant) {
        if (constant == null) {
            return null;
        }

        return metadata(constant.getDeclaringClass()).valueOf(constant);
    }

    /**
     * 按持久化值查找枚举常量。
     *
     * <p>查找顺序为：{@link EnumValue} 声明的值、{@link Enum#name()}、
     * 忽略大小写的名称匹配。均未命中时抛出异常。</p>
     *
     * @param type  枚举类型
     * @param value 持久化值，允许为 {@code null}
     * @param <E>   枚举类型
     * @return 枚举常量；入参为 {@code null} 或空串时返回 {@code null}
     * @throws IllegalArgumentException 枚举类型为空时抛出
     * @throws JpaPlusException         无法匹配枚举常量时抛出
     */
    public static <E extends Enum<E>> E fromValue(
            Class<E> type,
            String value
    ) {
        if (type == null) {
            throw new IllegalArgumentException(
                    "Enum type must not be null"
            );
        }

        if (value == null || value.isEmpty()) {
            return null;
        }

        E constant = metadata(type).resolve(value);

        if (constant != null) {
            return constant;
        }

        throw new JpaPlusException(
                "Unknown value '" + value + "' for enum "
                        + type.getName()
        );
    }

    /**
     * 获取枚举类型元数据，缺失时创建并缓存。
     *
     * @param type 枚举类型
     * @return 枚举元数据
     */
    private static EnumMetadata metadata(Class<?> type) {
        return CACHE.computeIfAbsent(type, EnumMetadata::new);
    }

    /**
     * 枚举类型元数据。
     *
     * <p>保存常量的持久化值以及值到常量的索引。</p>
     */
    private static final class EnumMetadata {

        /** 枚举类型。 */
        private final Class<?> type;

        /** 枚举常量到持久化值的索引。 */
        private final Map<Enum<?>, String> valueByConstant;

        /** 持久化值到枚举常量的索引。 */
        private final Map<String, Enum<?>> byValue;

        /** 忽略大小写的名称到枚举常量的索引。 */
        private final Map<String, Enum<?>> byNameIgnoreCase;

        /**
         * 解析枚举类型。
         *
         * @param type 枚举类型
         * @throws JpaPlusException 声明冲突或持久化值重复时抛出
         */
        private EnumMetadata(Class<?> type) {
            this.type = type;

            Map<String, Field> constantFields = constantFields(type);
            Method method = valueMethod(type);

            if (!constantFields.isEmpty() && method != null) {
                throw new JpaPlusException(
                        "Use either constant @EnumValue or method "
                                + "@EnumValue, not both: " + type.getName()
                );
            }

            Map<String, Enum<?>> values = new LinkedHashMap<>();
            Map<Enum<?>, String> constantValues = new LinkedHashMap<>();
            Map<String, Enum<?>> names = new LinkedHashMap<>();

            for (Object item : type.getEnumConstants()) {
                Enum<?> constant = (Enum<?>) item;
                String value = resolveValue(
                        constant,
                        constantFields.get(constant.name()),
                        method
                );

                if (values.putIfAbsent(value, constant) != null) {
                    throw new JpaPlusException(
                            "Duplicated enum value '" + value
                                    + "' in enum " + type.getName()
                    );
                }

                constantValues.put(constant, value);
                names.putIfAbsent(
                        constant.name().toLowerCase(),
                        constant
                );
            }

            this.valueByConstant =
                    Collections.unmodifiableMap(constantValues);
            this.byValue = Collections.unmodifiableMap(values);
            this.byNameIgnoreCase = Collections.unmodifiableMap(names);
        }

        /**
         * 获取枚举常量的持久化值。
         *
         * @param constant 枚举常量
         * @return 持久化值
         */
        private String valueOf(Enum<?> constant) {
            String value = valueByConstant.get(constant);

            if (value == null) {
                throw new JpaPlusException(
                        "Cannot resolve enum value for "
                                + constant.getDeclaringClass().getName()
                                + "." + constant.name()
                );
            }

            return value;
        }

        /**
         * 按持久化值查找枚举常量。
         *
         * @param value 持久化值
         * @param <E>   枚举类型
         * @return 枚举常量；未匹配时返回 {@code null}
         */
        @SuppressWarnings("unchecked")
        private <E extends Enum<E>> E resolve(String value) {
            Enum<?> constant = byValue.get(value);

            if (constant == null) {
                constant = byNameIgnoreCase.get(value.toLowerCase());
            }

            return (E) constant;
        }

        /**
         * 解析单个常量的持久化值。
         *
         * @param constant 枚举常量
         * @param field    常量字段；未标注时为 {@code null}
         * @param method   值方法；未标注时为 {@code null}
         * @return 持久化值
         */
        private String resolveValue(
                Enum<?> constant,
                Field field,
                Method method
        ) {
            if (field != null) {
                return requireText(
                        field.getAnnotation(EnumValue.class).value(),
                        constant
                );
            }

            if (method != null) {
                try {
                    Object result = method.invoke(constant);

                    if (result == null) {
                        throw new JpaPlusException(
                                "@EnumValue method must not return null: "
                                        + type.getName() + "."
                                        + method.getName()
                        );
                    }

                    return requireText(
                            String.valueOf(result),
                            constant
                    );
                } catch (IllegalAccessException
                         | InvocationTargetException e) {
                    throw new JpaPlusException(
                            "Cannot invoke @EnumValue method "
                                    + type.getName() + "."
                                    + method.getName(),
                            e
                    );
                }
            }

            return constant.name();
        }

        /**
         * 收集标注 {@link EnumValue} 的枚举常量字段。
         *
         * @param type 枚举类型
         * @return 常量名到字段的映射
         */
        private static Map<String, Field> constantFields(Class<?> type) {
            Map<String, Field> fields = new LinkedHashMap<>();

            for (Field field : type.getDeclaredFields()) {
                if (field.isEnumConstant()
                        && field.isAnnotationPresent(EnumValue.class)) {
                    fields.put(field.getName(), field);
                }
            }

            return fields;
        }

        /**
         * 查找标注 {@link EnumValue} 的无参方法。
         *
         * @param type 枚举类型
         * @return 方法；未标注时返回 {@code null}
         * @throws JpaPlusException 存在多个标注方法时抛出
         */
        private static Method valueMethod(Class<?> type) {
            Method found = null;

            for (Method method : type.getDeclaredMethods()) {
                if (method.isSynthetic()
                        || method.getParameterCount() != 0
                        || !method.isAnnotationPresent(EnumValue.class)) {
                    continue;
                }

                if (found != null) {
                    throw new JpaPlusException(
                            "Only one @EnumValue method is allowed in enum "
                                    + type.getName()
                    );
                }

                method.setAccessible(true);
                found = method;
            }

            return found;
        }

        /**
         * 校验持久化值非空。
         *
         * @param value    持久化值
         * @param constant 枚举常量
         * @return 原始值
         * @throws JpaPlusException 值为空白时抛出
         */
        private static String requireText(String value, Enum<?> constant) {
            if (value == null || value.isBlank()) {
                throw new JpaPlusException(
                        "@EnumValue must not be blank: "
                                + constant.getDeclaringClass().getName()
                                + "." + constant.name()
                );
            }

            return value;
        }
    }
}
