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

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.extension.annotation.Desensitize;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 脱敏工具。
 *
 * <p>既可以按类型直接脱敏单个字符串，也可以就地处理整个对象：</p>
 *
 * <pre>{@code
 * String masked = Desensitizes.mask("13800001234", DesensitizeType.PHONE);
 *
 * UserView view = Desensitizes.apply(userView);
 * }</pre>
 *
 * <p>{@link #apply(Object)} 会扫描对象及其父类中标注
 * {@link Desensitize} 的字符串字段并写回脱敏结果。字段元数据按类型缓存，
 * 重复调用不会重复反射。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class Desensitizes {

    /** 字段元数据缓存。 */
    private static final Map<Class<?>, List<FieldRule>> CACHE =
            new ConcurrentHashMap<>();

    /** 工具类不允许实例化。 */
    private Desensitizes() {
    }

    /**
     * 对字符串脱敏。
     *
     * @param value 原始值，允许为 {@code null}
     * @param type  脱敏类型，允许为 {@code null}
     * @return 脱敏后的值；类型为空时原样返回
     */
    public static String mask(String value, DesensitizeType type) {
        return type == null ? value : type.mask(value);
    }

    /**
     * 就地脱敏对象中的字符串字段。
     *
     * @param target 目标对象，允许为 {@code null}
     * @param <T>    对象类型
     * @return 处理后的同一对象；入参为 {@code null} 时返回 {@code null}
     * @throws JpaPlusException 字段不可访问或写入失败时抛出
     */
    public static <T> T apply(T target) {
        if (target == null) {
            return null;
        }

        for (FieldRule rule : rulesOf(target.getClass())) {
            Object value = rule.read(target);

            if (!(value instanceof String text) || text.isEmpty()) {
                continue;
            }

            rule.write(target, rule.type().mask(text));
        }

        return target;
    }

    /**
     * 获取字段声明的脱敏类型。
     *
     * @param type      对象类型
     * @param fieldName 字段名
     * @return 脱敏类型；字段未标注时返回 {@code null}
     */
    public static DesensitizeType typeOf(
            Class<?> type,
            String fieldName
    ) {
        if (type == null || fieldName == null) {
            return null;
        }

        for (FieldRule rule : rulesOf(type)) {
            if (rule.field().getName().equals(fieldName)) {
                return rule.type();
            }
        }

        return null;
    }

    /**
     * 获取类型的字段规则，缺失时扫描并缓存。
     *
     * @param type 对象类型
     * @return 字段规则列表
     */
    private static List<FieldRule> rulesOf(Class<?> type) {
        return CACHE.computeIfAbsent(type, Desensitizes::scan);
    }

    /**
     * 扫描类型及其父类中的脱敏字段。
     *
     * @param type 对象类型
     * @return 字段规则列表
     */
    private static List<FieldRule> scan(Class<?> type) {
        Map<String, FieldRule> rules = new LinkedHashMap<>();
        Class<?> current = type;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Desensitize annotation =
                        field.getAnnotation(Desensitize.class);

                if (annotation == null
                        || field.isSynthetic()
                        || Modifier.isStatic(field.getModifiers())
                        || !String.class.equals(field.getType())) {
                    continue;
                }

                field.setAccessible(true);
                rules.putIfAbsent(
                        field.getName(),
                        new FieldRule(field, annotation.type())
                );
            }

            current = current.getSuperclass();
        }

        return Collections.unmodifiableList(
                new ArrayList<>(rules.values())
        );
    }

    /**
     * 脱敏字段规则。
     *
     * @param field 字段
     * @param type  脱敏类型
     */
    private record FieldRule(Field field, DesensitizeType type) {

        /**
         * 读取字段值。
         *
         * @param target 目标对象
         * @return 字段值
         * @throws JpaPlusException 读取失败时抛出
         */
        private Object read(Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new JpaPlusException(
                        "Cannot read field " + field.getName(),
                        e
                );
            }
        }

        /**
         * 写入字段值。
         *
         * @param target 目标对象
         * @param value  字段值
         * @throws JpaPlusException 写入失败时抛出
         */
        private void write(Object target, Object value) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException e) {
                throw new JpaPlusException(
                        "Cannot write field " + field.getName(),
                        e
                );
            }
        }
    }
}
