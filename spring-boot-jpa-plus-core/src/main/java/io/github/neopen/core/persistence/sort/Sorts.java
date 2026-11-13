package io.github.neopen.core.persistence.sort;

import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * 安全排序解析工具。
 *
 * <p>把前端传入的排序表达式解析为 Spring Data {@link Sort}，并校验
 * 排序字段是否合法，避免外部输入直接拼接字段导致越权排序或注入。</p>
 *
 * <p>表达式格式为 {@code 字段,方向;字段,方向}，方向支持
 * {@code asc} 与 {@code desc}（忽略大小写），省略方向时默认升序：</p>
 *
 * <pre>{@code
 * Sorts.parse("createTime,desc;id,asc", Order.class);
 * Sorts.parse("id,desc", Set.of("id", "totalAmount"));
 * }</pre>
 *
 * <p>按实体类型解析时支持嵌套属性路径，逐段校验字段是否存在，
 * 最多允许 3 层：</p>
 *
 * <pre>{@code
 * Sorts.parse("customer.name,asc", Order.class);
 * }</pre>
 *
 * <p>需要对外暴露稳定字段名时，可以传入别名映射，把外部字段名
 * 转换成实体属性路径；别名目标同样会做字段校验：</p>
 *
 * <pre>{@code
 * Sorts.parse("name,desc", Order.class, Map.of("name", "customer.name"));
 * }</pre>
 *
 * <p>空表达式返回 {@link Sort#unsorted()}，非法字段、非法路径或
 * 非法方向抛出 {@link IllegalArgumentException}。</p>
 */
public final class Sorts {

    /** 排序路径允许的最大层级。 */
    private static final int MAX_PATH_DEPTH = 3;

    /** 工具类不允许实例化。 */
    private Sorts() {
    }

    /**
     * 按实体类型校验并解析排序表达式。
     *
     * <p>支持 {@code customer.name} 这类嵌套属性路径，校验时会
     * 逐段检查实体及其父类的字段。</p>
     *
     * @param expression 排序表达式，允许为 {@code null} 或空白
     * @param entityType 实体类型
     * @return 排序规则；表达式为空时返回未排序
     * @throws IllegalArgumentException 实体类型为空、排序字段非法、
     *                                 路径过深或排序方向非法时抛出
     */
    public static Sort parse(String expression, Class<?> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        return parse(expression, field -> resolvePath(entityType, field));
    }

    /**
     * 按实体类型和别名映射校验并解析排序表达式。
     *
     * <p>字段名先尝试通过别名映射转换为实体属性路径，未登记为别名
     * 时按原字段名处理；最终属性路径始终做字段校验。</p>
     *
     * @param expression 排序表达式，允许为 {@code null} 或空白
     * @param entityType 实体类型
     * @param aliases    外部字段名到实体属性路径的映射，不能为
     *                   {@code null}
     * @return 排序规则；表达式为空时返回未排序
     * @throws IllegalArgumentException 实体类型为空、别名映射为空、
     *                                 排序字段非法或排序方向非法时抛出
     */
    public static Sort parse(
            String expression,
            Class<?> entityType,
            Map<String, String> aliases
    ) {
        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        if (aliases == null) {
            throw new IllegalArgumentException(
                    "Aliases must not be null"
            );
        }

        return parse(expression, field -> resolvePath(
                entityType,
                aliases.getOrDefault(field, field)
        ));
    }

    /**
     * 按允许字段集合校验并解析排序表达式。
     *
     * <p>允许字段集合按完整字段名精确匹配，不做嵌套路径拆解。</p>
     *
     * @param expression    排序表达式，允许为 {@code null} 或空白
     * @param allowedFields 允许排序的字段集合
     * @return 排序规则；表达式为空时返回未排序
     * @throws IllegalArgumentException 允许字段为空、排序字段非法或
     *                                 排序方向非法时抛出
     */
    public static Sort parse(String expression, Set<String> allowedFields) {
        if (allowedFields == null) {
            throw new IllegalArgumentException(
                    "Allowed sort fields must not be null"
            );
        }

        return parse(expression, field -> {
            if (!allowedFields.contains(field)) {
                throw new IllegalArgumentException(
                        "Unsupported sort field: " + field
                );
            }

            return field;
        });
    }

    /**
     * 按字段解析器解析排序表达式。
     *
     * @param expression 排序表达式，允许为 {@code null} 或空白
     * @param resolver   字段解析器，负责校验并返回实体属性路径
     * @return 排序规则；表达式为空时返回未排序
     */
    private static Sort parse(
            String expression,
            UnaryOperator<String> resolver
    ) {
        if (expression == null || expression.isBlank()) {
            return Sort.unsorted();
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (String item : expression.split(";")) {
            if (item.isBlank()) {
                continue;
            }

            orders.add(toOrder(item, resolver));
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * 解析单个排序项。
     *
     * @param item     排序项
     * @param resolver 字段解析器
     * @return 排序项
     */
    private static Sort.Order toOrder(
            String item,
            UnaryOperator<String> resolver
    ) {
        String[] parts = item.split(",", -1);
        String field = parts[0].trim();

        if (parts.length > 2 || field.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid sort expression: " + item
            );
        }

        return new Sort.Order(
                resolveDirection(parts, item),
                resolver.apply(field)
        );
    }

    /**
     * 解析排序方向。
     *
     * @param parts 排序项片段
     * @param item  原始排序项
     * @return 排序方向
     */
    private static Sort.Direction resolveDirection(
            String[] parts,
            String item
    ) {
        if (parts.length == 1 || parts[1].isBlank()) {
            return Sort.Direction.ASC;
        }

        String direction = parts[1].trim();

        if ("asc".equalsIgnoreCase(direction)) {
            return Sort.Direction.ASC;
        }

        if ("desc".equalsIgnoreCase(direction)) {
            return Sort.Direction.DESC;
        }

        throw new IllegalArgumentException(
                "Invalid sort direction: " + direction + " in " + item
        );
    }

    /**
     * 校验嵌套属性路径并返回规范化路径。
     *
     * @param rootType 根实体类型
     * @param path     属性路径
     * @return 规范化后的属性路径
     */
    private static String resolvePath(Class<?> rootType, String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "Sort field must not be blank"
            );
        }

        String[] segments = path.split("\\.", -1);

        if (segments.length > MAX_PATH_DEPTH) {
            throw new IllegalArgumentException(
                    "Sort path is too deep: " + path
            );
        }

        Class<?> current = rootType;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();

            if (segment.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid sort path: " + path
                );
            }

            Field field = findField(current, segment);

            if (field == null) {
                throw new IllegalArgumentException(
                        "Unsupported sort field: " + path
                );
            }

            segments[i] = segment;

            if (i < segments.length - 1) {
                current = field.getType();

                if (isSimpleType(current)) {
                    throw new IllegalArgumentException(
                            "Sort path is not sortable: " + path
                    );
                }
            }
        }

        return String.join(".", segments);
    }

    /**
     * 在类型及其父类中查找实例字段。
     *
     * @param type 类型
     * @param name 字段名
     * @return 字段；不存在时返回 {@code null}
     */
    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }

                if (field.getName().equals(name)) {
                    return field;
                }
            }
        }

        return null;
    }

    /**
     * 判断类型是否为不可继续展开的简单类型。
     *
     * @param type 类型
     * @return 是否简单类型
     */
    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type.isArray()
                || type.getName().startsWith("java.");
    }
}
