# Specification Executor

`JpaPlusSpecExecutor` centralizes pagination, slicing, counting and fetch join pagination for `Specification` queries, so that each repository does not need to reimplement count logic.

## Creation

```java
JpaPlusSpecExecutor<Order> executor =
        JpaPlusSpecExecutor.of(entityManager, Order.class);

// 主键属性不是 id 时显式指定
JpaPlusSpecExecutor<Order> custom =
        JpaPlusSpecExecutor.of(entityManager, Order.class, "orderId");
```

| Method | Description |
| --- | --- |
| `static <T> JpaPlusSpecExecutor<T> of(EntityManager, Class<T>)` | The default primary key attribute is `id` |
| `static <T> JpaPlusSpecExecutor<T> of(EntityManager, Class<T>, String idAttribute)` | Specify the primary key attribute |
| `logicDelete(boolean enabled)` | Control whether the logical deletion filter is appended automatically, enabled by default |

## Query Methods

| Method | Description |
| --- | --- |
| `List<T> findAll(Specification<T> spec)` | Query a list |
| `Page<T> findPage(Specification<T> spec, Pageable pageable)` | Paginated query |
| `Page<T> findPage(Specification<T> spec, Pageable pageable, List<String> fetchAttributes)` | Fetch join pagination |
| `Slice<T> findSlice(Specification<T> spec, Pageable pageable)` | Slice query |
| `Stream<T> stream(Specification<T> spec)` | Streaming query, ordered by primary key ascending |
| `long count(Specification<T> spec)` | Count |
| `boolean exists(Specification<T> spec)` | Check whether records exist, based on count |

## Stream Queries

`stream` does not load the whole result into memory, which suits large exports and batch processing:

```java
try (Stream<Order> stream = executor.stream(spec)) {
    stream.map(Order::getOrderNo).forEach(this::export);
}
```

- Results are ordered by primary key ascending, so the order is stable.
- The stream is bound to the current transaction: consume it inside the transaction and prefer try-with-resources.
- Logical-deletion, tenant and data-permission conditions are still appended automatically.
- The HQL and native SQL executors provide `stream(...)` overloads as well.

## Pagination and Counting

```java
Page<Order> page = executor.findPage(
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        PageRequest.of(0, 10, Sort.by("id").descending())
);
```

Counting uses the configured primary key attribute to execute `countDistinct`, so duplicated one-to-many rows produced by joins do not inflate the total.

`findSlice` does not execute a count; instead it fetches one extra record to determine whether a next page exists:

```java
Slice<Order> slice = executor.findSlice(spec, PageRequest.of(0, 10));
boolean hasNext = slice.hasNext();
```

## Fetch Join Pagination

Using a fetch join directly on a paginated query makes JPA perform pagination in memory. When `fetchAttributes` is provided, the executor switches to a three-step query:

1. Count the total with `countDistinct`
2. Query only the primary keys of the current page
3. Load the entities by primary key with the fetch join

```java
Page<Order> page = executor.findPage(
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        PageRequest.of(0, 10, Sort.by("id")),
        List.of("orderProducts")
);
```

The three-step logic is also exposed as static methods for reuse in custom queries:

```java
Page<Order> page = FetchJoinPaging.findPage(
        entityManager,
        Order.class,
        "id",
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        List.of("orderProducts"),
        PageRequest.of(0, 10, Sort.by("id"))
);
```

## Single Record and Deletion

| Method | Description |
| --- | --- |
| `T findById(Object id)` | Find by primary key, applying the same logical deletion filter; returns `null` when not found |
| `int deleteById(Object id)` | Logical deletion takes precedence; physical deletion when the entity is not annotated with `@LogicDelete` |
| `int deleteAllById(Collection<?> ids)` | Delete in batch by primary key |
| `int deleteAll(Collection<T> entities)` | Delete in batch by entity |

```java
Order order = executor.findById(1L);
int affected = executor.deleteById(1L);
```

The behavior of the delete methods depends on whether the entity is annotated with `@LogicDelete`; see [Logical Deletion](/en/core/logic-delete) for the details.

## Relationship with Repository

The executor uses `EntityManager` directly and does not require the `Repository` to extend `JpaSpecificationExecutor`. Both approaches can coexist:

- The repository already has `JpaSpecificationExecutor` and fetch join pagination is not needed: use `findAll(spec, pageable)` directly.
- `Slice`, `countDistinct` or fetch join pagination is needed: use the executor.
