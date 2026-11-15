package io.github.neopen.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树形结构构建工具。
 *
 * <p>根据平铺列表和父子关系构建树，保留输入顺序：</p>
 *
 * <pre>{@code
 * List<Category> tree = TreeUtils.build(
 *         categories,
 *         Category::getId,
 *         Category::getParentId,
 *         Category::setChildren
 * );
 * }</pre>
 *
 * <p>规则：</p>
 *
 * <ul>
 *     <li>父子 ID 为 {@code null} 或父节点不存在时视为根节点；</li>
 *     <li>ID 为空、ID 重复、自引用或存在环时抛出
 *     {@link IllegalArgumentException}；</li>
 *     <li>所有节点的子集合都会被重置为空列表，再按输入顺序挂接子节点。</li>
 * </ul>
 */
public final class TreeUtils {

    /** 工具类不允许实例化。 */
    private TreeUtils() {
    }

    /**
     * 构建树形结构。
     *
     * @param nodes          平铺节点集合，允许为 {@code null}
     * @param idGetter       节点 ID 读取器
     * @param parentIdGetter 父节点 ID 读取器
     * @param childrenSetter 子节点写回器
     * @param <T>            节点类型
     * @param <ID>           ID 类型
     * @return 根节点列表；入参为空时返回空列表
     * @throws IllegalArgumentException 参数为空、ID 为空、ID 重复、
     *                                  自引用或存在环时抛出
     */
    public static <T, ID> List<T> build(
            Collection<T> nodes,
            Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter,
            BiConsumer<T, List<T>> childrenSetter
    ) {
        if (idGetter == null) {
            throw new IllegalArgumentException(
                    "Id getter must not be null"
            );
        }

        if (parentIdGetter == null) {
            throw new IllegalArgumentException(
                    "Parent id getter must not be null"
            );
        }

        if (childrenSetter == null) {
            throw new IllegalArgumentException(
                    "Children setter must not be null"
            );
        }

        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        Map<ID, T> byId = new LinkedHashMap<>();
        Map<ID, ID> parentById = new LinkedHashMap<>();

        for (T node : nodes) {
            if (node == null) {
                throw new IllegalArgumentException(
                        "Tree node must not be null"
                );
            }

            ID id = idGetter.apply(node);

            if (id == null) {
                throw new IllegalArgumentException(
                        "Tree node id must not be null"
                );
            }

            if (byId.putIfAbsent(id, node) != null) {
                throw new IllegalArgumentException(
                        "Duplicated tree node id: " + id
                );
            }

            parentById.put(id, parentIdGetter.apply(node));
        }

        Map<ID, List<T>> children = new LinkedHashMap<>();
        List<T> roots = new ArrayList<>();

        for (Map.Entry<ID, T> entry : byId.entrySet()) {
            ID id = entry.getKey();
            ID parentId = parentById.get(id);

            if (parentId != null && parentId.equals(id)) {
                throw new IllegalArgumentException(
                        "Tree node cannot be its own parent: " + id
                );
            }

            children.put(id, new ArrayList<>());

            if (parentId == null || !byId.containsKey(parentId)) {
                roots.add(entry.getValue());
            }
        }

        detectCycle(parentById, byId.keySet());

        for (Map.Entry<ID, T> entry : byId.entrySet()) {
            ID id = entry.getKey();
            ID parentId = parentById.get(id);

            if (parentId != null && children.containsKey(parentId)) {
                children.get(parentId).add(entry.getValue());
            }
        }

        for (Map.Entry<ID, T> entry : byId.entrySet()) {
            childrenSetter.accept(
                    entry.getValue(),
                    children.get(entry.getKey())
            );
        }

        return roots;
    }

    /**
     * 检测父子关系中的环。
     *
     * @param parentById 节点 ID 到父 ID 的映射
     * @param ids        全部节点 ID
     * @param <ID>       ID 类型
     * @throws IllegalArgumentException 存在环时抛出
     */
    private static <ID> void detectCycle(
            Map<ID, ID> parentById,
            Set<ID> ids
    ) {
        Set<ID> visited = new HashSet<>();

        for (ID id : ids) {
            if (visited.contains(id)) {
                continue;
            }

            Set<ID> chain = new HashSet<>();
            ID current = id;

            while (current != null && ids.contains(current)) {
                if (!chain.add(current)) {
                    throw new IllegalArgumentException(
                            "Cycle detected in tree at id: " + current
                    );
                }

                current = parentById.get(current);
            }

            visited.addAll(chain);
        }
    }
}
