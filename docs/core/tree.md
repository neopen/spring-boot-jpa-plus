# 树形结构

`TreeUtils` 把平铺的父子列表转换成树，不依赖实体基类或特定字段名。

## 用法

```java
List<Category> tree = TreeUtils.build(
        categories,
        Category::getId,
        Category::getParentId,
        Category::setChildren
);
```

方法签名：

```java
static <T, ID> List<T> build(
        Collection<T> nodes,
        Function<T, ID> idGetter,
        Function<T, ID> parentIdGetter,
        BiConsumer<T, List<T>> childrenSetter
)
```

## 行为约定

| 场景 | 行为 |
| --- | --- |
| 入参为 `null` 或空集合 | 返回空列表 |
| 父 ID 为 `null` | 视为根节点 |
| 父节点不在集合中 | 视为根节点 |
| 节点 ID 为 `null` | 抛出 `IllegalArgumentException` |
| 节点 ID 重复 | 抛出 `IllegalArgumentException` |
| 节点以自己为父节点 | 抛出 `IllegalArgumentException` |
| 父子关系存在环 | 抛出 `IllegalArgumentException` |
| 已有子集合 | 先重置为空列表，再按输入顺序挂接 |

根节点与同级节点都保持输入集合的顺序。

## 示例

```java
class Category {

    private Long id;
    private Long parentId;
    private List<Category> children = new ArrayList<>();

    // getters / setters
}
```

输入 `(2, 1)`、`(1, null)`、`(3, 1)`，输出只有一个根节点 `1`，其子节点顺序为 `2`、`3`，与输入顺序一致。
