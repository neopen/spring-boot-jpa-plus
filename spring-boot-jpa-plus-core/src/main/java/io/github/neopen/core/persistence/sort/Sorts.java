package io.github.neopen.core.persistence.sort;

import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
 * <p>按实体类型解析时会扫描实体及其父类的全部实例字段；空表达式
 * 返回 {@link Sort#unsorted()}，非法字段或方向抛出
 * {@link IllegalArgumentException}。</p>
 */
public final class Sorts {

    private Sorts() {
    }

    /**
     * 按实体类型校验并解析排序表达式。
     *
     * @param expression 排序表达式，允许为 {@code null} 或空白
     * @param entityType 实体类型
     * @return 排序规则；表达式为空时返回未排序
     * @throws IllegalArgumentException 实体类型为空、排序字段非法或
     *                                 排序方向非法时抛出
     */
    public static Sort parse(String expression, Class<?> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        return parse(expression, fieldNames(entityType));
    }

    /**
     * 按允许字段集合校验并解析排序表达式。
     *
     * @param expression    排序表达式，允许为 {@code null} 或空白
     * @param allowedFields 允许排序的字段集合
     * @return 排序规则；表达式为空时返回未排序
     * @throws IllegalArgumentException 允许字段为空、排序字段非法或
     *                                 排序方向非法时抛出
     */
    public static Sort parse(String expression, Set<String> allowedFields) {
        if (expression == null || expression.isBlank()) {
            return Sort.unsorted();
        }

        if (allowedFields == null) {
            throw new IllegalArgumentException(
                    "Allowed sort fields must not be null"
            );
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (String item : expression.split(";")) {
            if (item.isBlank()) {
                continue;
            }

            orders.add(toOrder(item, allowedFields));
        }

        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }

    /**
     * 解析单个排序项。
     *
     * @param item          排序项
     * @param allowedFields 允许排序的字段集合
     * @return 排序项
     */
    private static Sort.Order toOrder(
            String item,
            Set<String> allowedFields
    ) {
        String[] parts = item.split(",", -1);
        String field = parts[0].trim();

        if (parts.length > 2 || field.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid sort expression: " + item
            );
        }

        if (!allowedFields.contains(field)) {
            throw new IllegalArgumentException(
                    "Unsupported sort field: " + field
            );
        }

        return new Sort.Order(resolveDirection(parts, item), field);
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
     * 收集实体及其父类的实例字段名。
     *
     * @param type 实体类型
     * @return 字段名集合
     */
    private static Set<String> fieldNames(Class<?> type) {
        Set<String> names = new LinkedHashSet<>();

        for (Class<?> current = type;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())
                        || field.isSynthetic()) {
                    continue;
                }

                names.add(field.getName());
            }
        }

        return names;
    }
}
