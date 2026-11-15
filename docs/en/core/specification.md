# Specification Builder

`SpecWrapper` builds Spring Data JPA `Specification` through chained calls, with conditions combined by `AND` by default. It supports both field-name strings and JPA Metamodel attributes.

## Creation and Output

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .toSpecification();

Page<Order> page = orderRepository.findAll(spec, PageRequest.of(0, 10));
```

| Method | Description |
| --- | --- |
| `static <T> SpecWrapper<T> create(Class<T> entityType)` | Create a builder |
| `Class<T> getEntityType()` | Get the entity type |
| `Specification<T> toSpecification()` | Generate a `Specification` |
| `Predicate toPredicate(...)` | Participate directly in Criteria building; `SpecWrapper` itself implements `Specification` |

## Condition Methods

| Method | Description |
| --- | --- |
| `eq / ne / gt / ge / lt / le(String field, Object value)` | Comparison operations |
| `like / likeLeft / likeRight / notLike(String field, String value)` | Fuzzy matching |
| `in / notIn(String field, Collection<?> values)` | Collection matching |
| `between(String field, Object start, Object end)` | Closed interval |
| `isNull / isNotNull(String field)` | Null check |
| `add(String field, SpecOperator operator, Object... values)` | Custom operator |
| `apply(Object param)` | Append conditions in bulk from an annotated parameter object |
| `raw(Specification<T> specification)` | Append a native `Specification` (subqueries, comparisons between fields, etc.) |

## Type-Safe Style

After generating the static Metamodel, property objects can replace strings to avoid mistyped field names:

```java
SpecWrapper.create(Order.class)
        .eq(Order_.status, "PAID")
        .isNotNull(Order_.paymentTime)
        .toSpecification();
```

The Metamodel overloads accept parameters of type `SingularAttribute<? super T, ?>` and cover `eq`, `ne`, `gt`, `ge`, `lt`, `le`, `like`, `likeLeft`, `likeRight`, `notLike`, `in`, `notIn`, `between`, `isNull`, `isNotNull`.

## Joins

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .join("orderProducts")
        .like("orderProducts.productName", "机械")
        .toSpecification();
```

| Method | Description |
| --- | --- |
| `join(String attribute)` | Inner join, with the alias equal to the attribute name |
| `join(String attribute, JoinType joinType)` | Specify the join type |
| `join(String attribute, String alias)` | Specify the alias, inner join |
| `join(String attribute, String alias, JoinType joinType)` | Specify the alias and join type |

Joining automatically enables `distinct` to avoid duplicated paged results and inflated counts caused by one-to-many associations. It can also be controlled explicitly:

```java
SpecWrapper.create(Order.class)
        .join("orderProducts", "op", JoinType.LEFT)
        .distinct(true);
```

## Fetch Join

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .fetch("orderProducts")
        .toSpecification();
```

`fetch(String attribute)` uses `LEFT` by default, and it can also be specified via `fetch(String attribute, JoinType joinType)`. Fetch joins are skipped in count queries.

Note: when fetch join is combined with pagination, JPA generally falls back to in-memory pagination. For database-side pagination, use `findPage(spec, pageable, fetchAttributes)` of the [Specification Executor](/en/core/executor).

## Nested Condition Groups

```java
SpecWrapper.create(Order.class)
        .eq("status", "PAID")
        .anyOf(w -> w
                .like("orderNo", "DS")
                .like("receiverName", "张"))
        .toSpecification();

// status = ? and (order_no like ? or receiver_name like ?)
```

| Method | Semantics |
| --- | --- |
| `and(Consumer<SpecWrapper<T>> nested)` | `AND` inside the group; the group as a whole is joined with `AND` |
| `or(Consumer<SpecWrapper<T>> nested)` | The group is joined with the preceding conditions by `OR`, with `AND` inside the group |
| `anyOf(Consumer<SpecWrapper<T>> nested)` | `OR` inside the group; the group as a whole is joined with the preceding conditions by `AND` |

## Annotation Conditions and Logical Deletion

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .apply(queryParam)      // 解析 @QueryCondition
        .filterDeleted()        // 追加 @LogicDelete 过滤
        .toSpecification();
```

`filterDeleted()` only takes effect for entities annotated with `@LogicDelete`; otherwise it returns unchanged. See [Annotation-Based Query Conditions](/en/core/conditions) and [Logical Deletion](/en/core/logic-delete).

## Native Specification

When subqueries, comparisons between fields, or other cases cannot be expressed with chained methods, append them with `raw`:

```java
SpecWrapper.create(Product.class)
        .raw((root, query, cb) -> cb.lessThanOrEqualTo(
                root.<Integer> get("stockQuantity"),
                root.<Integer> get("safetyStock")))
        .toSpecification();
```
