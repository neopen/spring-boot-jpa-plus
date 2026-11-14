package io.github.neopen.core.persistence.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 查询结果到 DTO 的映射器。
 *
 * <p>支持将 {@link Tuple}、{@code Object[]} 或单列标量结果映射为普通
 * DTO。列名与 DTO 属性匹配时会忽略大小写、下划线和驼峰差异，
 * 例如 {@code order_no}、{@code orderNo}、{@code ORDER_NO} 都能
 * 命中 {@code orderNo} 字段。</p>
 *
 * <p>映射优先使用公开 setter，其次直接写入字段；常见数值、字符串、
 * 布尔和枚举类型会自动转换，其余不兼容类型抛出
 * {@link JpaPlusException}。</p>
 *
 * @param <T> DTO 类型
 */
public final class ResultMapper<T> {

    /** DTO 类型。 */
    private final Class<T> targetType;

    /** 规范化列名到属性访问器的映射。 */
    private final Map<String, Accessor> accessors;

    /** 按声明顺序排列的属性访问器。 */
    private final List<Accessor> orderedAccessors;

    /**
     * 创建结果映射器。
     *
     * @param targetType DTO 类型
     */
    private ResultMapper(Class<T> targetType) {
        this.targetType = targetType;
        this.orderedAccessors = scan(targetType);
        this.accessors = new LinkedHashMap<>();

        for (Accessor accessor : orderedAccessors) {
            accessors.putIfAbsent(
                    normalize(accessor.name()),
                    accessor
            );
        }
    }

    /**
     * 创建指定 DTO 类型的结果映射器。
     *
     * @param targetType DTO 类型，不能为 {@code null}
     * @param <T>        DTO 类型
     * @return 结果映射器
     * @throws IllegalArgumentException DTO 类型为空或无可用属性时抛出
     */
    public static <T> ResultMapper<T> of(Class<T> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException(
                    "Target type must not be null"
            );
        }

        if (targetType.isInterface()
                || targetType.isPrimitive()
                || targetType.isArray()) {
            throw new IllegalArgumentException(
                    "Unsupported target type: " + targetType.getName()
            );
        }

        return new ResultMapper<>(targetType);
    }

    /**
     * 映射单行结果。
     *
     * @param row 查询结果行，允许为 {@code null}
     * @return 映射后的 DTO；结果行为空时返回 {@code null}
     */
    public T map(Object row) {
        if (row == null) {
            return null;
        }

        if (targetType.isInstance(row)) {
            return targetType.cast(row);
        }

        T target = newInstance();

        if (row instanceof Tuple tuple) {
            mapTuple(tuple, target);
            return target;
        }

        if (row instanceof Object[] values) {
            return map(values, null);
        }

        if (orderedAccessors.size() == 1) {
            write(orderedAccessors.get(0), target, row);
            return target;
        }

        throw new JpaPlusException(
                "Cannot map scalar result to " + targetType.getName()
                        + ": the DTO must declare exactly one field"
        );
    }

    /**
     * 按列名映射一行数组结果。
     *
     * <p>列名为空时按 DTO 属性声明顺序映射；此时列数与属性数必须一致。</p>
     *
     * @param values  列值数组
     * @param aliases 列名数组，允许为 {@code null}
     * @return 映射后的 DTO
     * @throws JpaPlusException 列数与属性数不一致时抛出
     */
    public T map(Object[] values, String[] aliases) {
        if (values == null) {
            return null;
        }

        T target = newInstance();

        if (aliases != null && aliases.length == values.length) {
            for (int i = 0; i < values.length; i++) {
                Accessor accessor = find(aliases[i]);

                if (accessor != null) {
                    write(accessor, target, values[i]);
                }
            }

            return target;
        }

        if (values.length != orderedAccessors.size()) {
            throw new JpaPlusException(
                    "Cannot map result without aliases to "
                            + targetType.getName() + ": expected "
                            + orderedAccessors.size() + " columns but got "
                            + values.length
            );
        }

        for (int i = 0; i < values.length; i++) {
            write(orderedAccessors.get(i), target, values[i]);
        }

        return target;
    }

    /**
     * 映射多行结果。
     *
     * @param rows 查询结果行
     * @return 映射后的 DTO 列表
     */
    public List<T> mapAll(Collection<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<T> result = new ArrayList<>(rows.size());

        for (Object row : rows) {
            result.add(map(row));
        }

        return result;
    }

    /**
     * 映射单行结果。
     *
     * @param row        查询结果行
     * @param targetType DTO 类型
     * @param <T>        DTO 类型
     * @return 映射后的 DTO
     */
    public static <T> T mapRow(Object row, Class<T> targetType) {
        return of(targetType).map(row);
    }

    /**
     * 按列名映射一行数组结果。
     *
     * @param values     列值数组
     * @param aliases    列名数组，允许为 {@code null}
     * @param targetType DTO 类型
     * @param <T>        DTO 类型
     * @return 映射后的 DTO
     */
    public static <T> T mapRow(
            Object[] values,
            String[] aliases,
            Class<T> targetType
    ) {
        return of(targetType).map(values, aliases);
    }

    /**
     * 映射多行结果。
     *
     * @param rows       查询结果行
     * @param targetType DTO 类型
     * @param <T>        DTO 类型
     * @return 映射后的 DTO 列表
     */
    public static <T> List<T> mapRows(
            Collection<?> rows,
            Class<T> targetType
    ) {
        return of(targetType).mapAll(rows);
    }

    /**
     * 映射 Tuple 结果。
     *
     * @param tuple  查询结果
     * @param target 目标对象
     */
    private void mapTuple(Tuple tuple, T target) {
        for (TupleElement<?> element : tuple.getElements()) {
            String alias = element.getAlias();

            if (alias == null || alias.isBlank()) {
                continue;
            }

            Accessor accessor = find(alias);

            if (accessor != null) {
                write(accessor, target, tuple.get(element));
            }
        }
    }

    /**
     * 创建 DTO 实例。
     *
     * @return DTO 实例
     * @throws JpaPlusException 无可用无参构造器时抛出
     */
    private T newInstance() {
        try {
            Constructor<T> constructor =
                    targetType.getDeclaredConstructor();

            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }

            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JpaPlusException(
                    "Cannot instantiate DTO " + targetType.getName()
                            + ": a no-arg constructor is required",
                    e
            );
        }
    }

    /**
     * 按列名查找属性访问器。
     *
     * @param alias 列名
     * @return 属性访问器；未匹配时返回 {@code null}
     */
    private Accessor find(String alias) {
        return alias == null ? null : accessors.get(normalize(alias));
    }

    /**
     * 写入属性值。
     *
     * @param accessor 属性访问器
     * @param target   目标对象
     * @param value    原始值
     */
    private void write(Accessor accessor, T target, Object value) {
        Object converted = convert(value, accessor.type());

        if (converted == null && accessor.type().isPrimitive()) {
            return;
        }

        try {
            accessor.write(target, converted);
        } catch (ReflectiveOperationException e) {
            throw new JpaPlusException(
                    "Cannot write property " + accessor.name()
                            + " of " + targetType.getName(),
                    e
            );
        }
    }

    /**
     * 转换列值到目标类型。
     *
     * @param value      原始值
     * @param targetType 目标类型
     * @return 转换后的值
     */
    private Object convert(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        if (targetType == String.class) {
            return String.valueOf(value);
        }

        if (value instanceof Number number && isNumber(targetType)) {
            return convertNumber(number, targetType);
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            return convertBoolean(value);
        }

        if (targetType.isEnum()) {
            return Enum.valueOf(
                    targetType.asSubclass(Enum.class),
                    String.valueOf(value)
            );
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
    private boolean isNumber(Class<?> type) {
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
    private Object convertNumber(Number number, Class<?> targetType) {
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
    private Boolean convertBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }

        if (value instanceof Number number) {
            return number.intValue() != 0;
        }

        String text = String.valueOf(value).trim();

        if ("1".equals(text) || "true".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }

        if ("0".equals(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }

        throw new JpaPlusException(
                "Cannot convert value to Boolean: " + text
        );
    }

    /**
     * 扫描 DTO 属性。
     *
     * @param type DTO 类型
     * @return 属性访问器列表
     */
    private static List<Accessor> scan(Class<?> type) {
        List<Accessor> result = new ArrayList<>();

        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }

                field.setAccessible(true);
                result.add(new Accessor(
                        field.getName(),
                        field.getType(),
                        field,
                        findSetter(current, field)
                ));
            }
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "DTO " + type.getName() + " declares no writable field"
            );
        }

        return result;
    }

    /**
     * 查找 setter 方法。
     *
     * @param type      声明属性的类型
     * @param field     属性字段
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
     * 规范化列名，忽略大小写与下划线。
     *
     * @param name 列名
     * @return 规范化后的名称
     */
    private static String normalize(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    /**
     * DTO 属性访问器。
     *
     * @param name   属性名
     * @param type   属性类型
     * @param field  字段
     * @param setter setter 方法，允许为 {@code null}
     */
    private record Accessor(
            String name,
            Class<?> type,
            Field field,
            Method setter
    ) {

        /**
         * 写入属性值。
         *
         * @param target 目标对象
         * @param value  属性值
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
