package io.github.neopen.convert.mapping;

import io.github.neopen.core.exception.JpaPlusException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 请求参数到实体的映射器。
 *
 * <p>用于写方向的参数转换：把 controller 的 {@code Param} 对象
 * 拷贝到新实体或已有实体上。字段按名称匹配，忽略大小写与下划线，
 * 例如 {@code order_no}、{@code orderNo}、{@code ORDER_NO} 都能
 * 命中 {@code orderNo} 字段。</p>
 *
 * <p>核心约定：</p>
 *
 * <ul>
 *     <li>只拷贝非 {@code null} 的参数值，天然支持部分更新，
 *         不会把实体已有字段清空。</li>
 *     <li>优先调用实体的公开 setter，其次直接写入字段。</li>
 *     <li>参数字段可用 {@link ParamField} 指定目标字段名，
 *         或用 {@link ParamIgnore} 排除。</li>
 *     <li>常见数值、字符串、布尔、字符和枚举类型自动转换。</li>
 * </ul>
 *
 * <pre>{@code
 * OrderParam param = new OrderParam();
 * param.setNumber("NO-1");
 *
 * Order order = ParamMapper.toEntity(param, Order.class);
 *
 * // 更新场景：只覆盖参数中非空的字段
 * ParamMapper.apply(param, existingOrder);
 * }</pre>
 *
 * @param <P> 参数类型
 * @param <E> 实体类型
 */
public final class ParamMapper<P, E> {

    /** 参数类型与实体类型组合到映射器的缓存。 */
    private static final Map<Key, ParamMapper<?, ?>> CACHE =
            new ConcurrentHashMap<>();

    /** 参数类型。 */
    private final Class<P> paramType;

    /** 实体类型。 */
    private final Class<E> entityType;

    /** 字段绑定关系。 */
    private final List<Binding> bindings;

    /**
     * 创建参数到实体的映射器。
     *
     * @param paramType  参数类型
     * @param entityType 实体类型
     */
    private ParamMapper(Class<P> paramType, Class<E> entityType) {
        this.paramType = paramType;
        this.entityType = entityType;
        this.bindings = scan(
                paramType,
                scanTargets(entityType)
        );
    }

    /**
     * 创建实体实例并拷贝参数中的非空字段。
     *
     * @param param      参数对象，允许为 {@code null}
     * @param entityType 实体类型，必须提供无参构造器
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 新建实体；参数为空时返回 {@code null}
     * @throws JpaPlusException 实体无法实例化或字段写入失败时抛出
     */
    public static <P, E> E toEntity(P param, Class<E> entityType) {
        if (param == null) {
            return null;
        }

        Objects.requireNonNull(
                entityType,
                "Entity type must not be null"
        );

        E entity = newInstance(entityType);

        apply(param, entity);

        return entity;
    }

    /**
     * 使用工厂创建实体并拷贝参数中的非空字段。
     *
     * @param param   参数对象，允许为 {@code null}
     * @param factory 实体工厂，不能为 {@code null}
     * @param <P>     参数类型
     * @param <E>     实体类型
     * @return 新建实体；参数为空时返回 {@code null}
     * @throws IllegalArgumentException 实体工厂为空时抛出
     */
    public static <P, E> E toEntity(
            P param,
            Supplier<E> factory
    ) {
        if (param == null) {
            return null;
        }

        Objects.requireNonNull(
                factory,
                "Entity factory must not be null"
        );

        E entity = factory.get();

        if (entity == null) {
            throw new IllegalArgumentException(
                    "Entity factory returned null"
            );
        }

        apply(param, entity);

        return entity;
    }

    /**
     * 把参数中的非空字段覆盖到已有实体。
     *
     * <p>参数或实体为空时直接返回，不做任何处理。</p>
     *
     * @param param  参数对象
     * @param entity 目标实体
     * @param <P>    参数类型
     * @param <E>    实体类型
     * @throws JpaPlusException 字段读取或写入失败时抛出
     */
    public static <P, E> void apply(P param, E entity) {
        if (param == null || entity == null) {
            return;
        }

        mapper(param.getClass(), entity.getClass())
                .copy(param, entity);
    }

    /**
     * 批量转换参数为实体。
     *
     * @param params     参数集合，允许为 {@code null} 或空集合
     * @param entityType 实体类型
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 实体列表；空参数元素会被跳过
     */
    public static <P, E> List<E> toEntities(
            Collection<P> params,
            Class<E> entityType
    ) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }

        List<E> result = new ArrayList<>(params.size());

        for (P param : params) {
            if (param != null) {
                result.add(toEntity(param, entityType));
            }
        }

        return result;
    }

    /**
     * 批量转换参数为实体。
     *
     * @param params  参数集合，允许为 {@code null} 或空集合
     * @param factory 实体工厂
     * @param <P>     参数类型
     * @param <E>     实体类型
     * @return 实体列表；空参数元素会被跳过
     */
    public static <P, E> List<E> toEntities(
            Collection<P> params,
            Supplier<E> factory
    ) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }

        List<E> result = new ArrayList<>(params.size());

        for (P param : params) {
            if (param != null) {
                result.add(toEntity(param, factory));
            }
        }

        return result;
    }

    /**
     * 拷贝参数值到实体。
     *
     * @param param  参数对象
     * @param entity 目标实体
     */
    private void copy(Object param, Object entity) {
        for (Binding binding : bindings) {
            Object value = binding.read(param);

            if (value == null) {
                continue;
            }

            Object converted = convert(value, binding.target().type());

            if (converted == null && binding.target().type().isPrimitive()) {
                continue;
            }

            try {
                binding.target().write(entity, converted);
            } catch (ReflectiveOperationException e) {
                throw new JpaPlusException(
                        "Cannot write property "
                                + binding.target().name()
                                + " of " + entityType.getName(),
                        e
                );
            }
        }
    }

    /**
     * 获取指定类型的映射器。
     *
     * @param paramType  参数类型
     * @param entityType 实体类型
     * @return 映射器
     */
    @SuppressWarnings("unchecked")
    private static ParamMapper<Object, Object> mapper(
            Class<?> paramType,
            Class<?> entityType
    ) {
        return (ParamMapper<Object, Object>) CACHE.computeIfAbsent(
                new Key(paramType, entityType),
                key -> new ParamMapper<>(key.paramType(), key.entityType())
        );
    }

    /**
     * 创建实体实例。
     *
     * @param entityType 实体类型
     * @param <E>        实体类型
     * @return 实体实例
     * @throws JpaPlusException 无可用无参构造器时抛出
     */
    private static <E> E newInstance(Class<E> entityType) {
        try {
            Constructor<E> constructor =
                    entityType.getDeclaredConstructor();

            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }

            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JpaPlusException(
                    "Cannot instantiate entity " + entityType.getName()
                            + ": a no-arg constructor is required",
                    e
            );
        }
    }

    /**
     * 扫描实体的可写字段。
     *
     * @param entityType 实体类型
     * @return 规范化字段名到访问器的映射
     */
    private static Map<String, TargetAccessor> scanTargets(
            Class<?> entityType
    ) {
        Map<String, TargetAccessor> result = new LinkedHashMap<>();

        for (Class<?> current = entityType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!isMappable(field)) {
                    continue;
                }

                field.setAccessible(true);

                result.putIfAbsent(
                        normalize(field.getName()),
                        new TargetAccessor(
                                field.getName(),
                                field.getType(),
                                field,
                                findSetter(current, field)
                        )
                );
            }
        }

        return result;
    }

    /**
     * 扫描参数字段并建立到实体的绑定。
     *
     * @param paramType 参数类型
     * @param targets   实体字段访问器
     * @return 字段绑定关系
     */
    private static List<Binding> scan(
            Class<?> paramType,
            Map<String, TargetAccessor> targets
    ) {
        List<Binding> result = new ArrayList<>();
        Set<String> bound = new LinkedHashSet<>();

        for (Class<?> current = paramType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!isMappable(field)
                        || field.isAnnotationPresent(ParamIgnore.class)) {
                    continue;
                }

                String name = targetName(field);
                String normalized = normalize(name);

                if (!bound.add(normalized)) {
                    continue;
                }

                TargetAccessor target = targets.get(normalized);

                if (target == null) {
                    continue;
                }

                field.setAccessible(true);

                result.add(new Binding(
                        field.getName(),
                        field,
                        target
                ));
            }
        }

        return List.copyOf(result);
    }

    /**
     * 判断字段是否参与映射。
     *
     * @param field 字段
     * @return 参与映射时返回 {@code true}
     */
    private static boolean isMappable(Field field) {
        return !Modifier.isStatic(field.getModifiers())
                && !Modifier.isTransient(field.getModifiers())
                && !field.isSynthetic();
    }

    /**
     * 解析参数字段对应的实体字段名。
     *
     * @param field 参数字段
     * @return 目标字段名
     */
    private static String targetName(Field field) {
        ParamField mapping = field.getAnnotation(ParamField.class);

        return mapping == null || mapping.value().isBlank()
                ? field.getName()
                : mapping.value();
    }

    /**
     * 查找 setter 方法。
     *
     * @param type  声明字段的类型
     * @param field 字段
     * @return setter 方法；不存在时返回 {@code null}
     */
    private static Method findSetter(Class<?> type, Field field) {
        String name = "set" + Character.toUpperCase(
                field.getName().charAt(0)
        ) + field.getName().substring(1);

        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(
                        name,
                        field.getType()
                );

                if (!Modifier.isStatic(method.getModifiers())) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // 继续沿父类查找
            }
        }

        return null;
    }

    /**
     * 规范化字段名，忽略大小写与下划线。
     *
     * @param name 字段名
     * @return 规范化后的名称
     */
    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 转换参数值到实体字段类型。
     *
     * @param value      参数值
     * @param targetType 实体字段类型
     * @return 转换后的值
     * @throws JpaPlusException 无法转换时抛出
     */
    private static Object convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return String.valueOf(value);
        }

        if (value instanceof Number number && isNumber(targetType)) {
            return convertNumber(number, targetType);
        }

        if (value instanceof String text && isNumber(targetType)) {
            return convertNumber(
                    new BigDecimal(text.trim()),
                    targetType
            );
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return convertBoolean(value);
        }

        if (targetType.isEnum()) {
            return convertEnum(value, targetType);
        }

        if (targetType == Character.class || targetType == char.class) {
            String text = String.valueOf(value);
            return text.isEmpty() ? null : text.charAt(0);
        }

        throw new JpaPlusException(
                "Cannot convert value of type " + value.getClass().getName()
                        + " to " + targetType.getName()
        );
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
     * 转换数值类型。
     *
     * @param number     原始数值
     * @param targetType 目标类型
     * @return 转换后的数值
     */
    private static Object convertNumber(
            Number number,
            Class<?> targetType
    ) {
        if (targetType == Integer.class || targetType == int.class) {
            return number.intValue();
        }

        if (targetType == Long.class || targetType == long.class) {
            return number.longValue();
        }

        if (targetType == Double.class || targetType == double.class) {
            return number.doubleValue();
        }

        if (targetType == Float.class || targetType == float.class) {
            return number.floatValue();
        }

        if (targetType == Short.class || targetType == short.class) {
            return number.shortValue();
        }

        if (targetType == Byte.class || targetType == byte.class) {
            return number.byteValue();
        }

        if (targetType == BigDecimal.class) {
            return number instanceof BigDecimal decimal
                    ? decimal
                    : new BigDecimal(number.toString());
        }

        return number instanceof BigInteger integer
                ? integer
                : BigInteger.valueOf(number.longValue());
    }

    /**
     * 转换布尔类型。
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
     * 转换枚举类型。
     *
     * @param value      原始值
     * @param targetType 枚举类型
     * @return 枚举值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertEnum(Object value, Class<?> targetType) {
        try {
            return Enum.valueOf(
                    (Class) targetType.asSubclass(Enum.class),
                    String.valueOf(value)
            );
        } catch (IllegalArgumentException e) {
            throw new JpaPlusException(
                    "Cannot convert value to " + targetType.getName() + ": "
                            + value,
                    e
            );
        }
    }

    /**
     * 参数类型与实体类型组合的缓存键。
     *
     * @param paramType  参数类型
     * @param entityType 实体类型
     */
    private record Key(Class<?> paramType, Class<?> entityType) {
    }

    /**
     * 参数字段到实体字段的绑定。
     *
     * @param name   参数字段名
     * @param source 参数字段
     * @param target 实体字段访问器
     */
    private record Binding(
            String name,
            Field source,
            TargetAccessor target
    ) {

        /**
         * 读取参数字段值。
         *
         * @param param 参数对象
         * @return 字段值
         * @throws JpaPlusException 读取失败时抛出
         */
        Object read(Object param) {
            try {
                return source.get(param);
            } catch (IllegalAccessException e) {
                throw new JpaPlusException(
                        "Cannot read parameter field " + name,
                        e
                );
            }
        }
    }

    /**
     * 实体字段访问器。
     *
     * @param name   字段名
     * @param type   字段类型
     * @param field  字段
     * @param setter setter 方法，允许为 {@code null}
     */
    private record TargetAccessor(
            String name,
            Class<?> type,
            Field field,
            Method setter
    ) {

        /**
         * 写入实体字段。
         *
         * @param target 实体
         * @param value  字段值
         * @throws ReflectiveOperationException 反射写入失败时抛出
         */
        void write(Object target, Object value)
                throws ReflectiveOperationException {
            if (setter != null) {
                setter.invoke(target, value);
            } else {
                field.set(target, value);
            }
        }
    }
}
