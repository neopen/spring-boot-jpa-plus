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
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import org.springframework.format.annotation.DateTimeFormat;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 属性值转换入口。
 *
 * <p>内置常见类型转换：字符串、数值、布尔、字符、枚举、UUID、时间类型、
 * 集合与数组元素，以及 JPA {@link AttributeConverter}。自定义转换器可
 * 通过 {@link #register(PropertyConverter)} 全局注册，注册越晚的转换器
 * 优先级越高。</p>
 *
 * <p>目标字段上的 {@code @Convert} 优先于内置枚举转换；
 * {@code @DateTimeFormat} 用于指定时间类型解析格式。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class Converters {

    /** 未处理的标记对象。 */
    private static final Object NOT_HANDLED = new Object();

    /** 自定义转换器，后注册的优先。 */
    private static final CopyOnWriteArrayList<PropertyConverter> CUSTOM =
            new CopyOnWriteArrayList<>();

    /** 内置转换器。 */
    private static final PropertyConverter BUILTIN =
            new BuiltinConverter();

    /** JPA 属性转换器实例缓存。 */
    private static final ClassValue<AttributeConverter<?, ?>> JPA_CONVERTERS =
            new ClassValue<>() {

                /**
                 * 创建 JPA 属性转换器实例。
                 *
                 * @param type 转换器类型
                 * @return 转换器实例
                 */
                @Override
                protected AttributeConverter<?, ?> computeValue(Class<?> type) {
                    return instantiateConverter(type);
                }
            };

    /** 日期格式缓存。 */
    private static final ConcurrentHashMap<String, DateTimeFormatter>
            FORMATTERS = new ConcurrentHashMap<>();

    /** 默认时区。 */
    private static final ZoneId ZONE = ZoneId.systemDefault();

    /** 通用日期时间格式。 */
    private static final DateTimeFormatter SPACE_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    /** 通用日期时间格式，精确到分钟。 */
    private static final DateTimeFormatter SPACE_DATE_TIME_MINUTE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    /** 工具类不允许实例化。 */
    private Converters() {
    }

    /**
     * 注册全局转换器。
     *
     * <p>后注册的转换器优先匹配，可用于覆盖内置行为。</p>
     *
     * @param converter 转换器，不能为 {@code null}
     * @throws NullPointerException 转换器为 {@code null}
     */
    public static void register(PropertyConverter converter) {
        CUSTOM.add(0, Objects.requireNonNull(
                converter,
                "Converter must not be null"
        ));
    }

    /**
     * 移除全局转换器。
     *
     * @param converter 转换器
     * @return 移除成功返回 {@code true}
     */
    public static boolean unregister(PropertyConverter converter) {
        return CUSTOM.remove(converter);
    }

    /**
     * 转换属性值。
     *
     * @param value   原始值，允许为 {@code null}
     * @param context 转换上下文，不能为 {@code null}
     * @return 转换后的值；入参为 {@code null} 时返回 {@code null}
     * @throws JpaPlusException 无法转换时抛出
     */
    public static Object convert(
            Object value,
            ConversionContext context
    ) {
        Objects.requireNonNull(context, "Conversion context must not be null");

        if (value == null) {
            return null;
        }

        for (PropertyConverter converter : CUSTOM) {
            if (converter.supports(context, value)) {
                return converter.convert(value, context);
            }
        }

        return BUILTIN.convert(value, context);
    }

    /**
     * 内置转换器。
     */
    private static final class BuiltinConverter
            implements PropertyConverter {

        /**
         * 执行内置转换。
         *
         * @param value   原始值，不会为 {@code null}
         * @param context 转换上下文
         * @return 转换后的值
         */
        @Override
        public Object convert(Object value, ConversionContext context) {
            Class<?> target = context.targetType();

            Object jpaConverted = convertJpa(value, context);

            if (jpaConverted != NOT_HANDLED) {
                return jpaConverted;
            }

            if (target.isArray()
                    && (value instanceof Collection<?>
                    || value.getClass().isArray())) {
                return convertArray(value, context);
            }

            if (Collection.class.isAssignableFrom(target)
                    && (value instanceof Collection<?>
                    || value.getClass().isArray())) {
                return convertCollection(value, context);
            }

            if (target.isInstance(value)) {
                return value;
            }

            if (target == String.class) {
                return convertToString(value);
            }

            if (target.isEnum()) {
                return convertEnum(value, target);
            }

            if (isTemporal(target)) {
                return convertTemporal(value, context);
            }

            if (isNumber(target)) {
                return convertNumber(value, target);
            }

            if (target == Boolean.class || target == boolean.class) {
                return convertBoolean(value);
            }

            if (target == Character.class || target == char.class) {
                return convertCharacter(value);
            }

            if (target == UUID.class) {
                return convertUuid(value);
            }

            throw cannotConvert(value, target);
        }
    }

    /**
     * 使用 JPA {@link Convert} 声明的转换器转换。
     *
     * @param value   原始值
     * @param context 转换上下文
     * @return 转换结果；未声明或已禁用时返回 {@link #NOT_HANDLED}
     */
    @SuppressWarnings("unchecked")
    private static Object convertJpa(
            Object value,
            ConversionContext context
    ) {
        AnnotatedElement element = context.annotatedElement();

        if (element == null) {
            return NOT_HANDLED;
        }

        Convert convert = element.getAnnotation(Convert.class);

        if (convert == null
                || convert.disableConversion()
                || convert.converter() == void.class) {
            return NOT_HANDLED;
        }

        AttributeConverter<?, ?> converter =
                JPA_CONVERTERS.get(convert.converter());

        try {
            return ((AttributeConverter<Object, Object>) converter)
                    .convertToEntityAttribute(value);
        } catch (RuntimeException ex) {
            throw new JpaPlusException(
                    "Cannot convert value of type "
                            + value.getClass().getName()
                            + " using " + convert.converter().getName(),
                    ex
            );
        }
    }

    /**
     * 实例化 JPA 属性转换器。
     *
     * @param type 转换器类型
     * @return 转换器实例
     * @throws JpaPlusException 实例化失败时抛出
     */
    @SuppressWarnings("unchecked")
    private static AttributeConverter<?, ?> instantiateConverter(
            Class<?> type
    ) {
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.trySetAccessible();

            return (AttributeConverter<?, ?>) constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new JpaPlusException(
                    "Cannot instantiate JPA AttributeConverter "
                            + type.getName()
                            + ": a no-arg constructor is required",
                    ex
            );
        }
    }

    /**
     * 转换集合。
     *
     * @param value   原始集合或数组
     * @param context 转换上下文
     * @return 目标集合
     */
    private static Object convertCollection(
            Object value,
            ConversionContext context
    ) {
        Type elementType = elementType(context.genericType());
        Collection<Object> result = newCollection(context.targetType());

        forEachValue(value, item ->
                result.add(convertElement(item, elementType, context))
        );

        return result;
    }

    /**
     * 转换数组。
     *
     * @param value   原始集合或数组
     * @param context 转换上下文
     * @return 目标数组
     */
    private static Object convertArray(
            Object value,
            ConversionContext context
    ) {
        Class<?> component = context.targetType().getComponentType();
        List<Object> items = new ArrayList<>();

        forEachValue(value, items::add);

        Object result = Array.newInstance(component, items.size());

        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);

            if (item != null && !component.isInstance(item)) {
                item = convert(
                        item,
                        context.elementContext(component, component)
                );
            }

            if (item == null && component.isPrimitive()) {
                continue;
            }

            Array.set(result, i, item);
        }

        return result;
    }

    /**
     * 转换集合或数组元素。
     *
     * @param item        元素
     * @param elementType 元素泛型类型，允许为 {@code null}
     * @param context     转换上下文
     * @return 转换后的元素
     */
    private static Object convertElement(
            Object item,
            Type elementType,
            ConversionContext context
    ) {
        if (item == null || elementType == null) {
            return item;
        }

        Class<?> elementClass = rawClass(elementType);

        if (elementClass == null || elementClass.isInstance(item)) {
            return item;
        }

        return convert(
                item,
                context.elementContext(elementClass, elementType)
        );
    }

    /**
     * 创建目标集合实例。
     *
     * @param type 目标类型
     * @return 集合实例
     */
    @SuppressWarnings("unchecked")
    private static Collection<Object> newCollection(Class<?> type) {
        if (!type.isInterface()
                && !Modifier.isAbstract(type.getModifiers())) {
            try {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.trySetAccessible();

                return (Collection<Object>) constructor.newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new JpaPlusException(
                        "Cannot instantiate collection " + type.getName(),
                        ex
                );
            }
        }

        if (SortedSet.class.isAssignableFrom(type)) {
            return new TreeSet<>();
        }

        if (Set.class.isAssignableFrom(type)) {
            return new LinkedHashSet<>();
        }

        if (Queue.class.isAssignableFrom(type)) {
            return new ArrayDeque<>();
        }

        return new ArrayList<>();
    }

    /**
     * 遍历集合或数组。
     *
     * @param value    集合或数组
     * @param consumer 元素消费者
     */
    private static void forEachValue(
            Object value,
            Consumer<Object> consumer
    ) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(consumer);
            return;
        }

        int length = Array.getLength(value);

        for (int i = 0; i < length; i++) {
            consumer.accept(Array.get(value, i));
        }
    }

    /**
     * 转换字符串。
     *
     * @param value 原始值
     * @return 字符串
     */
    private static String convertToString(Object value) {
        if (value instanceof Enum<?> constant) {
            return EnumValues.toValue(constant);
        }

        return String.valueOf(value);
    }

    /**
     * 转换枚举。
     *
     * @param value      原始值
     * @param targetType 枚举类型
     * @return 枚举常量
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertEnum(
            Object value,
            Class<?> targetType
    ) {
        if (targetType.isInstance(value)) {
            return value;
        }

        try {
            return EnumValues.fromValue(
                    (Class) targetType.asSubclass(Enum.class),
                    String.valueOf(value)
            );
        } catch (JpaPlusException ex) {
            throw new JpaPlusException(
                    "Cannot convert value to " + targetType.getName()
                            + ": " + value,
                    ex
            );
        }
    }

    /**
     * 转换数值。
     *
     * @param value      原始值
     * @param targetType 目标类型
     * @return 数值
     */
    private static Object convertNumber(
            Object value,
            Class<?> targetType
    ) {
        if (targetType == BigInteger.class) {
            if (value instanceof BigInteger integer) {
                return integer;
            }

            if (value instanceof String text) {
                try {
                    return new BigInteger(text.trim());
                } catch (NumberFormatException ex) {
                    throw cannotConvert(value, targetType, ex);
                }
            }

            return BigInteger.valueOf(number(value).longValue());
        }

        BigDecimal decimal;

        if (value instanceof BigDecimal bigDecimal) {
            decimal = bigDecimal;
        } else if (value instanceof String text) {
            try {
                decimal = new BigDecimal(text.trim());
            } catch (NumberFormatException ex) {
                throw cannotConvert(value, targetType, ex);
            }
        } else if (value instanceof Number number) {
            decimal = new BigDecimal(number.toString());
        } else {
            throw cannotConvert(value, targetType);
        }

        if (targetType == BigDecimal.class) {
            return decimal;
        }

        if (targetType == Integer.class || targetType == int.class) {
            return decimal.intValue();
        }

        if (targetType == Long.class || targetType == long.class) {
            return decimal.longValue();
        }

        if (targetType == Double.class || targetType == double.class) {
            return decimal.doubleValue();
        }

        if (targetType == Float.class || targetType == float.class) {
            return decimal.floatValue();
        }

        if (targetType == Short.class || targetType == short.class) {
            return decimal.shortValue();
        }

        return decimal.byteValue();
    }

    /**
     * 将原始值转为数值。
     *
     * @param value 原始值
     * @return 数值
     */
    private static Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }

        return new BigDecimal(String.valueOf(value).trim());
    }

    /**
     * 转换布尔值。
     *
     * @param value 原始值
     * @return 布尔值
     */
    private static Boolean convertBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof Number number) {
            return number.intValue() != 0;
        }

        String text = String.valueOf(value).trim();

        if ("1".equals(text) || "true".equalsIgnoreCase(text)
                || "yes".equalsIgnoreCase(text)
                || "on".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }

        if ("0".equals(text) || "false".equalsIgnoreCase(text)
                || "no".equalsIgnoreCase(text)
                || "off".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }

        throw new JpaPlusException(
                "Cannot convert value to Boolean: " + text
        );
    }

    /**
     * 转换字符。
     *
     * @param value 原始值
     * @return 字符
     */
    private static Character convertCharacter(Object value) {
        String text = String.valueOf(value);

        return text.isEmpty() ? null : text.charAt(0);
    }

    /**
     * 转换 UUID。
     *
     * @param value 原始值
     * @return UUID
     */
    private static UUID convertUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }

        try {
            return UUID.fromString(String.valueOf(value).trim());
        } catch (IllegalArgumentException ex) {
            throw cannotConvert(value, UUID.class, ex);
        }
    }

    /**
     * 判断是否为支持的时间类型。
     *
     * @param type 类型
     * @return 支持时返回 {@code true}
     */
    private static boolean isTemporal(Class<?> type) {
        return type == LocalDate.class
                || type == LocalDateTime.class
                || type == LocalTime.class
                || type == OffsetDateTime.class
                || type == ZonedDateTime.class
                || type == Instant.class
                || type == Date.class
                || type == Year.class
                || type == YearMonth.class
                || type == Duration.class
                || type == Period.class;
    }

    /**
     * 转换时间类型。
     *
     * @param value   原始值
     * @param context 转换上下文
     * @return 时间对象
     */
    private static Object convertTemporal(
            Object value,
            ConversionContext context
    ) {
        Class<?> target = context.targetType();

        if (value instanceof String text) {
            if (target == Duration.class) {
                try {
                    return Duration.parse(text);
                } catch (DateTimeException ex) {
                    throw cannotConvert(value, target, ex);
                }
            }

            if (target == Period.class) {
                try {
                    return Period.parse(text);
                } catch (DateTimeException ex) {
                    throw cannotConvert(value, target, ex);
                }
            }

            DateTimeException failure = null;

            for (DateTimeFormatter formatter : formatters(context)) {
                try {
                    return toTemporal(formatter.parse(text), target);
                } catch (DateTimeException ex) {
                    failure = ex;
                }
            }

            throw cannotConvert(value, target, failure);
        }

        Instant instant = toInstant(value);

        if (instant != null) {
            return fromInstant(instant, target, value);
        }

        throw cannotConvert(value, target);
    }

    /**
     * 将原始值转为瞬时时间。
     *
     * @param value 原始值
     * @return 瞬时时间；无法转换时返回 {@code null}
     */
    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }

        if (value instanceof Date date) {
            return date.toInstant();
        }

        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }

        if (value instanceof LocalDateTime dateTime) {
            return dateTime.atZone(ZONE).toInstant();
        }

        if (value instanceof LocalDate date) {
            return date.atStartOfDay(ZONE).toInstant();
        }

        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant();
        }

        if (value instanceof ZonedDateTime dateTime) {
            return dateTime.toInstant();
        }

        return null;
    }

    /**
     * 将瞬时时间转换为目标类型。
     *
     * @param instant 瞬时时间
     * @param target  目标类型
     * @param raw     原始值，用于异常信息
     * @return 目标时间对象
     */
    private static Object fromInstant(
            Instant instant,
            Class<?> target,
            Object raw
    ) {
        ZonedDateTime zoned = instant.atZone(ZONE);

        if (target == Instant.class) {
            return instant;
        }

        if (target == Date.class) {
            return Date.from(instant);
        }

        if (target == LocalDateTime.class) {
            return zoned.toLocalDateTime();
        }

        if (target == LocalDate.class) {
            return zoned.toLocalDate();
        }

        if (target == LocalTime.class) {
            return zoned.toLocalTime();
        }

        if (target == OffsetDateTime.class) {
            return zoned.toOffsetDateTime();
        }

        if (target == ZonedDateTime.class) {
            return zoned;
        }

        if (target == Year.class) {
            return Year.from(zoned);
        }

        if (target == YearMonth.class) {
            return YearMonth.from(zoned);
        }

        throw cannotConvert(raw, target);
    }

    /**
     * 将解析结果转换为目标时间类型。
     *
     * @param parsed 解析结果
     * @param target 目标类型
     * @return 目标时间对象
     * @throws DateTimeException 目标类型不支持时抛出
     */
    private static Object toTemporal(
            TemporalAccessor parsed,
            Class<?> target
    ) {
        if (target == LocalDate.class) {
            return toLocalDate(parsed);
        }

        if (target == LocalDateTime.class) {
            return toLocalDateTime(parsed);
        }

        if (target == LocalTime.class) {
            return LocalTime.from(parsed);
        }

        if (target == OffsetDateTime.class) {
            try {
                return OffsetDateTime.from(parsed);
            } catch (DateTimeException ignored) {
                return toLocalDateTime(parsed).atZone(ZONE)
                        .toOffsetDateTime();
            }
        }

        if (target == ZonedDateTime.class) {
            try {
                return ZonedDateTime.from(parsed);
            } catch (DateTimeException ignored) {
                return toLocalDateTime(parsed).atZone(ZONE);
            }
        }

        if (target == Instant.class) {
            return toInstant(parsed);
        }

        if (target == Date.class) {
            return Date.from(toInstant(parsed));
        }

        if (target == Year.class) {
            try {
                return Year.from(parsed);
            } catch (DateTimeException ignored) {
                return Year.from(toLocalDate(parsed));
            }
        }

        if (target == YearMonth.class) {
            try {
                return YearMonth.from(parsed);
            } catch (DateTimeException ignored) {
                return YearMonth.from(toLocalDate(parsed));
            }
        }

        throw new DateTimeException(
                "Unsupported temporal type " + target.getName()
        );
    }

    /**
     * 从解析结果提取日期。
     *
     * @param parsed 解析结果
     * @return 日期
     */
    private static LocalDate toLocalDate(TemporalAccessor parsed) {
        try {
            return LocalDate.from(parsed);
        } catch (DateTimeException ignored) {
            return toLocalDateTime(parsed).toLocalDate();
        }
    }

    /**
     * 从解析结果提取日期时间。
     *
     * @param parsed 解析结果
     * @return 日期时间
     */
    private static LocalDateTime toLocalDateTime(
            TemporalAccessor parsed
    ) {
        try {
            return LocalDateTime.from(parsed);
        } catch (DateTimeException ignored) {
            try {
                return LocalDate.from(parsed).atStartOfDay();
            } catch (DateTimeException ignoredAgain) {
                return LocalDateTime.ofInstant(
                        toInstant(parsed),
                        ZONE
                );
            }
        }
    }

    /**
     * 从解析结果提取瞬时时间。
     *
     * @param parsed 解析结果
     * @return 瞬时时间
     */
    private static Instant toInstant(TemporalAccessor parsed) {
        try {
            return Instant.from(parsed);
        } catch (DateTimeException ignored) {
            try {
                return OffsetDateTime.from(parsed).toInstant();
            } catch (DateTimeException ignoredAgain) {
                try {
                    return LocalDateTime.from(parsed)
                            .atZone(ZONE)
                            .toInstant();
                } catch (DateTimeException ignoredDate) {
                    return LocalDate.from(parsed)
                            .atStartOfDay(ZONE)
                            .toInstant();
                }
            }
        }
    }

    /**
     * 获取时间解析格式。
     *
     * @param context 转换上下文
     * @return 格式列表
     */
    private static List<DateTimeFormatter> formatters(
            ConversionContext context
    ) {
        AnnotatedElement element = context.annotatedElement();

        if (element != null) {
            DateTimeFormat format =
                    element.getAnnotation(DateTimeFormat.class);

            if (format != null) {
                if (!format.pattern().isEmpty()) {
                    return List.of(formatter(format.pattern()));
                }

                if (format.iso() != DateTimeFormat.ISO.NONE) {
                    return List.of(isoFormatter(format.iso()));
                }
            }
        }

        return defaultFormatters(context.targetType());
    }

    /**
     * 获取指定类型的默认格式。
     *
     * @param target 目标类型
     * @return 格式列表
     */
    private static List<DateTimeFormatter> defaultFormatters(
            Class<?> target
    ) {
        if (target == LocalDate.class) {
            return List.of(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        if (target == LocalTime.class) {
            return List.of(
                    DateTimeFormatter.ISO_LOCAL_TIME,
                    DateTimeFormatter.ofPattern("HH:mm:ss"),
                    DateTimeFormatter.ofPattern("HH:mm")
            );
        }

        if (target == OffsetDateTime.class) {
            return List.of(
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                    DateTimeFormatter.ISO_DATE_TIME
            );
        }

        if (target == ZonedDateTime.class) {
            return List.of(
                    DateTimeFormatter.ISO_ZONED_DATE_TIME,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
            );
        }

        if (target == Instant.class) {
            return List.of(
                    DateTimeFormatter.ISO_INSTANT,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    SPACE_DATE_TIME
            );
        }

        if (target == Year.class) {
            return List.of(DateTimeFormatter.ofPattern("uuuu"));
        }

        if (target == YearMonth.class) {
            return List.of(DateTimeFormatter.ofPattern("uuuu-MM"));
        }

        return List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                SPACE_DATE_TIME,
                SPACE_DATE_TIME_MINUTE,
                DateTimeFormatter.ISO_LOCAL_DATE
        );
    }

    /**
     * 获取 ISO 格式。
     *
     * @param iso ISO 类型
     * @return 格式
     */
    private static DateTimeFormatter isoFormatter(DateTimeFormat.ISO iso) {
        if (iso == DateTimeFormat.ISO.DATE) {
            return DateTimeFormatter.ISO_DATE;
        }

        if (iso == DateTimeFormat.ISO.TIME) {
            return DateTimeFormatter.ISO_TIME;
        }

        return DateTimeFormatter.ISO_DATE_TIME;
    }

    /**
     * 获取或创建日期格式。
     *
     * @param pattern 格式
     * @return 日期格式
     * @throws JpaPlusException 格式非法时抛出
     */
    private static DateTimeFormatter formatter(String pattern) {
        return FORMATTERS.computeIfAbsent(pattern, value -> {
            try {
                return DateTimeFormatter.ofPattern(value);
            } catch (IllegalArgumentException ex) {
                throw new JpaPlusException(
                        "Invalid date pattern: " + value,
                        ex
                );
            }
        });
    }

    /**
     * 判断是否为数值类型。
     *
     * @param type 类型
     * @return 是否数值类型
     */
    private static boolean isNumber(Class<?> type) {
        return type == Integer.class || type == int.class
                || type == Long.class || type == long.class
                || type == Double.class || type == double.class
                || type == Float.class || type == float.class
                || type == Short.class || type == short.class
                || type == Byte.class || type == byte.class
                || type == BigDecimal.class || type == BigInteger.class;
    }

    /**
     * 获取集合元素类型。
     *
     * @param genericType 泛型类型
     * @return 元素类型；无法解析时返回 {@code null}
     */
    private static Type elementType(Type genericType) {
        if (!(genericType instanceof ParameterizedType parameterized)) {
            return null;
        }

        Type[] arguments = parameterized.getActualTypeArguments();

        if (arguments.length != 1
                || arguments[0] instanceof WildcardType) {
            return null;
        }

        return arguments[0];
    }

    /**
     * 解析泛型类型对应的原始类型。
     *
     * @param type 泛型类型
     * @return 原始类型；无法解析时返回 {@code null}
     */
    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }

        if (type instanceof ParameterizedType parameterized) {
            return rawClass(parameterized.getRawType());
        }

        if (type instanceof WildcardType wildcard
                && wildcard.getUpperBounds().length > 0) {
            return rawClass(wildcard.getUpperBounds()[0]);
        }

        if (type instanceof TypeVariable<?> variable
                && variable.getBounds().length > 0) {
            return rawClass(variable.getBounds()[0]);
        }

        return null;
    }

    /**
     * 创建无法转换异常。
     *
     * @param value  原始值
     * @param target 目标类型
     * @return 异常
     */
    private static JpaPlusException cannotConvert(
            Object value,
            Class<?> target
    ) {
        return new JpaPlusException(
                "Cannot convert value of type "
                        + value.getClass().getName()
                        + " to " + target.getName()
        );
    }

    /**
     * 创建无法转换异常。
     *
     * @param value  原始值
     * @param target 目标类型
     * @param cause  原始异常
     * @return 异常
     */
    private static JpaPlusException cannotConvert(
            Object value,
            Class<?> target,
            Throwable cause
    ) {
        return new JpaPlusException(
                "Cannot convert value of type "
                        + value.getClass().getName()
                        + " to " + target.getName() + ": " + value,
                cause
        );
    }
}
