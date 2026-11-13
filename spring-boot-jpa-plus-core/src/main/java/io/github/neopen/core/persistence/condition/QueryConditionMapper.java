package io.github.neopen.core.persistence.condition;

import io.github.neopen.core.exception.JpaPlusException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 查询参数条件映射器。
 *
 * <p>扫描参数对象（含父类）上标注了 {@link QueryCondition} 的字段，
 * 按注解声明的运算符把非空参数解析为 {@link QueryConditionEntry}。</p>
 *
 * <p>支持嵌套参数对象：字段本身未标注 {@link QueryCondition} 且类型为
 * 业务参数对象时，会递归扫描其字段，最多 3 层；JDK 类型、集合、Map、
 * 数组与枚举不会被递归。嵌套对象的条件不添加字段前缀，字段路径可以
 * 通过 {@link QueryCondition#value()} 显式指定。</p>
 *
 * <p>过滤规则：</p>
 * <ul>
 *     <li>参数对象为 {@code null} 时不做任何处理；</li>
 *     <li>字段值为 {@code null} 时忽略；</li>
 *     <li>字段值为空白字符串且 {@link QueryCondition#ignoreBlank()}
 *         为 {@code true} 时忽略；</li>
 *     <li>字段值为空集合或空数组时忽略；</li>
 *     <li>{@link ConditionOperator#IS_NULL} 与
 *         {@link ConditionOperator#IS_NOT_NULL} 把字段值当作开关，
 *         值为 {@code Boolean.FALSE} 时忽略。</li>
 * </ul>
 *
 * <p>注解元数据与嵌套字段按参数类型缓存，重复调用不会重复反射扫描。</p>
 */
public final class QueryConditionMapper {

    /** 嵌套参数对象允许的最大层级。 */
    private static final int MAX_NESTED_DEPTH = 3;

    /** 参数类型与字段绑定的缓存。 */
    private static final Map<Class<?>, List<Binding>> CACHE =
            new ConcurrentHashMap<>();

    /** 参数类型与嵌套字段的缓存。 */
    private static final Map<Class<?>, List<Field>> NESTED_CACHE =
            new ConcurrentHashMap<>();

    private QueryConditionMapper() {
    }

    /**
     * 将参数对象上的条件应用到查询构造器。
     *
     * <p>条件按注解解析顺序依次应用，分组信息会被忽略；需要按分组
     * 组装 OR 条件时请使用 {@link #resolve(Object)}。</p>
     *
     * @param param   查询参数，允许为 {@code null}
     * @param applier 条件应用回调
     * @throws IllegalArgumentException 注解配置非法或回调为空时抛出
     * @throws JpaPlusException         字段不可读取时抛出
     */
    public static void apply(Object param, ConditionApplier applier) {
        if (param == null) {
            return;
        }

        if (applier == null) {
            throw new IllegalArgumentException(
                    "Condition applier must not be null"
            );
        }

        for (QueryConditionEntry entry : resolve(param)) {
            applier.apply(
                    entry.field(),
                    entry.operator(),
                    entry.value(),
                    entry.second()
            );
        }
    }

    /**
     * 解析参数对象上的全部查询条件。
     *
     * <p>返回顺序与字段声明顺序一致，并包含嵌套参数对象中的条件。</p>
     *
     * @param param 查询参数，允许为 {@code null}
     * @return 查询条件列表；参数为空时返回空列表
     * @throws IllegalArgumentException 注解配置非法时抛出
     * @throws JpaPlusException         字段不可读取时抛出
     */
    public static List<QueryConditionEntry> resolve(Object param) {
        if (param == null) {
            return List.of();
        }

        List<QueryConditionEntry> entries = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(
                new IdentityHashMap<>()
        );
        visited.add(param);

        collect(param, param.getClass(), 0, visited, entries);

        return List.copyOf(entries);
    }

    /**
     * 收集当前参数对象与嵌套参数对象上的条件。
     *
     * @param param   参数对象
     * @param type    参数类型
     * @param depth   当前嵌套层级
     * @param visited 已访问参数对象，用于循环检测
     * @param entries 条件收集列表
     */
    private static void collect(
            Object param,
            Class<?> type,
            int depth,
            Set<Object> visited,
            List<QueryConditionEntry> entries
    ) {
        for (Binding binding : bindingsOf(type)) {
            QueryConditionEntry entry = binding.resolve(param);

            if (entry != null) {
                entries.add(entry);
            }
        }

        if (depth >= MAX_NESTED_DEPTH) {
            return;
        }

        for (Field field : nestedFieldsOf(type)) {
            Object value = read(field, param);

            if (value == null || !visited.add(value)) {
                continue;
            }

            collect(value, field.getType(), depth + 1, visited, entries);
            visited.remove(value);
        }
    }

    /**
     * 获取参数类型的字段绑定，首次访问时扫描并缓存。
     *
     * @param type 参数类型
     * @return 字段绑定列表
     */
    private static List<Binding> bindingsOf(Class<?> type) {
        return CACHE.computeIfAbsent(type, QueryConditionMapper::scan);
    }

    /**
     * 扫描参数类型上标注了 {@link QueryCondition} 的字段。
     *
     * @param type 参数类型
     * @return 字段绑定列表
     */
    private static List<Binding> scan(Class<?> type) {
        List<Binding> bindings = new ArrayList<>();

        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                QueryCondition annotation =
                        field.getAnnotation(QueryCondition.class);

                if (annotation == null
                        || Modifier.isStatic(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }

                field.setAccessible(true);
                bindings.add(createBinding(type, field, annotation));
            }
        }

        return List.copyOf(bindings);
    }

    /**
     * 获取可递归扫描的嵌套参数字段。
     *
     * @param type 参数类型
     * @return 嵌套字段列表
     */
    private static List<Field> nestedFieldsOf(Class<?> type) {
        return NESTED_CACHE.computeIfAbsent(
                type,
                QueryConditionMapper::scanNestedFields
        );
    }

    /**
     * 扫描可作为嵌套参数对象的字段。
     *
     * @param type 参数类型
     * @return 嵌套字段列表
     */
    private static List<Field> scanNestedFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();

        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.isSynthetic()
                        || field.isAnnotationPresent(QueryCondition.class)
                        || !isNestedType(field.getType())) {
                    continue;
                }

                field.setAccessible(true);
                fields.add(field);
            }
        }

        return List.copyOf(fields);
    }

    /**
     * 判断类型是否可以作为嵌套参数对象继续扫描。
     *
     * @param type 字段类型
     * @return 可以递归时返回 {@code true}
     */
    private static boolean isNestedType(Class<?> type) {
        return !type.isPrimitive()
                && !type.isEnum()
                && !type.isArray()
                && !type.isInterface()
                && !Collection.class.isAssignableFrom(type)
                && !Map.class.isAssignableFrom(type)
                && !type.getName().startsWith("java.");
    }

    /**
     * 创建字段绑定。
     *
     * @param type       参数类型
     * @param field      被标注字段
     * @param annotation 条件注解
     * @return 字段绑定
     */
    private static Binding createBinding(
            Class<?> type,
            Field field,
            QueryCondition annotation
    ) {
        ConditionOperator operator = annotation.operator();
        String name = annotation.value().isBlank()
                ? field.getName()
                : annotation.value().trim();
        String group = annotation.group().isBlank()
                ? ""
                : annotation.group().trim();
        Field endField = null;

        if (operator.parameterCount() > 1) {
            if (annotation.endField().isBlank()) {
                throw new IllegalArgumentException(
                        "@QueryCondition on " + field.getName()
                                + " requires endField for " + operator
                );
            }

            endField = findField(type, annotation.endField().trim());

            if (endField == null) {
                throw new IllegalArgumentException(
                        "End field " + annotation.endField()
                                + " not found on " + type.getName()
                );
            }

            endField.setAccessible(true);
        }

        return new Binding(
                field,
                name,
                operator,
                annotation.ignoreBlank(),
                endField,
                group
        );
    }

    /**
     * 在类型及其父类中查找字段。
     *
     * @param type 参数类型
     * @param name 字段名
     * @return 字段；不存在时返回 {@code null}
     */
    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 继续向父类查找
            }
        }

        return null;
    }

    /**
     * 读取字段值。
     *
     * @param field  字段
     * @param target 目标对象
     * @return 字段值
     */
    private static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            throw new JpaPlusException(
                    "Cannot read query condition field: " + field.getName(),
                    ex
            );
        }
    }

    /**
     * 单个字段的条件绑定。
     */
    private static final class Binding {

        /** 被标注字段。 */
        private final Field field;

        /** 查询字段名或列名。 */
        private final String name;

        /** 条件运算符。 */
        private final ConditionOperator operator;

        /** 是否忽略空白字符串。 */
        private final boolean ignoreBlank;

        /** 区间结束字段，仅 BETWEEN 使用。 */
        private final Field endField;

        /** 条件分组名。 */
        private final String group;

        /**
         * 创建字段绑定。
         *
         * @param field       被标注字段
         * @param name        查询字段名或列名
         * @param operator    条件运算符
         * @param ignoreBlank 是否忽略空白字符串
         * @param endField    区间结束字段
         * @param group       条件分组名
         */
        private Binding(
                Field field,
                String name,
                ConditionOperator operator,
                boolean ignoreBlank,
                Field endField,
                String group
        ) {
            this.field = field;
            this.name = name;
            this.operator = operator;
            this.ignoreBlank = ignoreBlank;
            this.endField = endField;
            this.group = group;
        }

        /**
         * 读取参数并解析条件。
         *
         * @param param 查询参数
         * @return 查询条件；参数应忽略时返回 {@code null}
         */
        private QueryConditionEntry resolve(Object param) {
            Object value = read(field, param);

            if (skip(value)) {
                return null;
            }

            if (operator.parameterCount() == 0) {
                return new QueryConditionEntry(
                        group,
                        name,
                        operator,
                        null,
                        null
                );
            }

            Object second = null;

            if (endField != null) {
                second = read(endField, param);

                if (skip(second)) {
                    return null;
                }
            }

            return new QueryConditionEntry(
                    group,
                    name,
                    operator,
                    value,
                    second
            );
        }

        /**
         * 判断字段值是否应跳过。
         *
         * @param value 字段值
         * @return 应跳过时返回 {@code true}
         */
        private boolean skip(Object value) {
            if (value == null) {
                return true;
            }

            if (operator.parameterCount() == 0) {
                return Boolean.FALSE.equals(value);
            }

            if (value instanceof CharSequence text) {
                return ignoreBlank && text.toString().isBlank();
            }

            if (value instanceof Collection<?> collection) {
                return collection.isEmpty();
            }

            if (value instanceof Map<?, ?> map) {
                return map.isEmpty();
            }

            if (value.getClass().isArray()) {
                return Array.getLength(value) == 0;
            }

            return false;
        }
    }
}
