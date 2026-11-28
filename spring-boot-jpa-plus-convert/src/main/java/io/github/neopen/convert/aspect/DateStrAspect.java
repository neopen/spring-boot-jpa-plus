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
package io.github.neopen.convert.aspect;

import io.github.neopen.convert.annotation.DateStrFormat;
import io.github.neopen.convert.annotation.DateStrValid;
import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.core.persistence.mapping.BeanIntrospector;
import io.github.neopen.core.persistence.mapping.PropertyAccessor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.DateTimeException;
import java.time.temporal.TemporalAccessor;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日期字符串校验切面。
 *
 * <p>在带有 {@link DateStrValid} 注解的方法、类或参数执行前，递归检查
 * 参数对象中标注 {@link DateStrFormat} 的字段。支持父类字段、嵌套对象、
 * 集合与 Map，并限制递归深度与访问次数，避免循环引用导致栈溢出。</p>
 *
 * <p>校验使用线程安全的 {@link DateTimeFormatter} 并缓存格式，
 * 不再对参数做 JSON 序列化。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Aspect
@Component
public class DateStrAspect {

    /** 匹配方法、类型或参数上的日期校验注解。 */
    private static final String AOP_POINTCUT_EXPRESSION =
            "@annotation(io.github.neopen.convert.annotation.DateStrValid)"
                    + " || @within(io.github.neopen.convert.annotation"
                    + ".DateStrValid)"
                    + " || execution(* *(.., @io.github.neopen.convert"
                    + ".annotation.DateStrValid (*), ..))";

    /** 日期格式缓存。 */
    private static final Map<String, DateTimeFormatter> FORMATTERS =
            new ConcurrentHashMap<>();

    /** 最大递归深度。 */
    private static final int MAX_DEPTH = 8;

    /** 无需递归扫描的包前缀。 */
    private static final String[] SKIPPED_PACKAGES = {
            "java.",
            "javax.",
            "jakarta.",
            "org.springframework.",
            "org.aspectj.",
            "org.hibernate.",
            "com.alibaba."
    };

    /**
     * 定义日期校验注解切点。
     */
    @Pointcut(AOP_POINTCUT_EXPRESSION)
    public void paramAspect() {
    }

    /**
     * 在目标方法执行前校验参数。
     *
     * @param joinPoint 当前连接点
     * @throws JpaPlusException 参数访问或日期校验失败时抛出
     */
    @Before("paramAspect()")
    public void doBefore(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        if (args == null || args.length == 0) {
            return;
        }

        Set<Object> visited = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );

        for (Object arg : args) {
            validate(arg, visited, 0);
        }
    }

    /**
     * 递归校验对象中的日期字段。
     *
     * @param value   待校验对象，允许为 {@code null}
     * @param visited 已访问对象集合
     * @param depth   当前深度
     * @throws JpaPlusException 日期格式非法时抛出
     */
    void validate(Object value, Set<Object> visited, int depth) {
        if (value == null || depth > MAX_DEPTH || !visited.add(value)) {
            return;
        }

        Class<?> type = value.getClass();

        if (type.isArray()) {
            int length = Array.getLength(value);

            for (int i = 0; i < length; i++) {
                validate(Array.get(value, i), visited, depth + 1);
            }

            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                validate(item, visited, depth + 1);
            }

            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                validate(item, visited, depth + 1);
            }

            return;
        }

        if (shouldSkip(type)) {
            return;
        }

        for (PropertyAccessor property
                : BeanIntrospector.metadata(type).properties()) {

            Field field = property.field();
            DateStrFormat format = field.getAnnotation(
                    DateStrFormat.class
            );
            Object fieldValue = property.read(value);

            if (format != null) {
                validateFormat(format, fieldValue);
            } else {
                validate(fieldValue, visited, depth + 1);
            }
        }
    }

    /**
     * 判断类型是否无需继续扫描。
     *
     * @param type 类型
     * @return 跳过时返回 {@code true}
     */
    private static boolean shouldSkip(Class<?> type) {
        if (type.isPrimitive() || type.isEnum()) {
            return true;
        }

        if (Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || Boolean.class == type
                || Character.class == type
                || java.util.UUID.class == type
                || java.util.Date.class.isAssignableFrom(type)
                || TemporalAccessor.class.isAssignableFrom(type)
                || Class.class == type
                || ClassLoader.class.isAssignableFrom(type)) {
            return true;
        }

        String name = type.getName();

        for (String prefix : SKIPPED_PACKAGES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 按字段注解校验日期值。
     *
     * @param format 日期格式注解
     * @param value  字段值
     * @throws JpaPlusException 必填值为空或格式非法时抛出
     */
    private static void validateFormat(
            DateStrFormat format,
            Object value
    ) {
        if (format.required() && value == null) {
            throw new JpaPlusException("日期/时间不能为空");
        }

        if (format.ignore() || value == null) {
            return;
        }

        if (!(value instanceof CharSequence text)) {
            return;
        }

        if (!isValidDate(text.toString(), format.value())) {
            throw new JpaPlusException(
                    "日期/时间格式非法（" + format.value() + "）"
            );
        }
    }

    /**
     * 校验字符串是否符合日期格式。
     *
     * @param value   待校验字符串
     * @param pattern 日期格式
     * @return 符合格式时返回 {@code true}
     * @throws JpaPlusException 日期格式非法时抛出
     */
    private static boolean isValidDate(String value, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }

        if (value.length() != pattern.length()) {
            return false;
        }

        DateTimeFormatter formatter = formatter(pattern);

        try {
            TemporalAccessor parsed = formatter.parse(value);

            return formatter.format(parsed).equals(value);
        } catch (DateTimeException ex) {
            return false;
        }
    }

    /**
     * 获取或创建日期格式。
     *
     * @param pattern 日期格式
     * @return 日期格式
     * @throws JpaPlusException 格式非法时抛出
     */
    private static DateTimeFormatter formatter(String pattern) {
        DateTimeFormatter formatter = FORMATTERS.get(pattern);

        if (formatter != null) {
            return formatter;
        }

        try {
            formatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException ex) {
            throw new JpaPlusException("日期格式非法（" + pattern + "）", ex);
        }

        FORMATTERS.putIfAbsent(pattern, formatter);

        return formatter;
    }
}
