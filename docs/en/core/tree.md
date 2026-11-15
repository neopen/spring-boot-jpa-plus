# Tree Structure

`TreeUtils` converts a flat list of parent-child relationships into a tree, without depending on an entity base class or specific field names.

## Usage

```java
List<Category> tree = TreeUtils.build(
        categories,
        Category::getId,
        Category::getParentId,
        Category::setChildren
);
```

Method signature:

```java
static <T, ID> List<T> build(
        Collection<T> nodes,
        Function<T, ID> idGetter,
        Function<T, ID> parentIdGetter,
        BiConsumer<T, List<T>> childrenSetter
)
```

## Behavior

| Scenario | Behavior |
| --- | --- |
| Input is `null` or an empty collection | Returns an empty list |
| Parent ID is `null` | Treated as a root node |
| Parent node is not in the collection | Treated as a root node |
| Node ID is `null` | Throws `IllegalArgumentException` |
| Duplicate node ID | Throws `IllegalArgumentException` |
| Node is its own parent | Throws `IllegalArgumentException` |
| Parent-child relationship contains a cycle | Throws `IllegalArgumentException` |
| An existing child collection | Reset to an empty list first, then attached in input order |

Both root nodes and sibling nodes keep the order of the input collection.

## Example

```java
class Category {

    private Long id;
    private Long parentId;
    private List<Category> children = new ArrayList<>();

    // getters / setters
}
```

Given the input `(2, 1)`, `(1, null)`, `(3, 1)`, the output has a single root node `1`, whose children are ordered `2`, `3`, matching the input order.
