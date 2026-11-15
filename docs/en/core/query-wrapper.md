# Dynamic Condition Queries

`QueryWrapper` combines queries by runtime conditions without writing Criteria API boilerplate. It is consumed by `JpaPlusDao.findList` / `findPage`.

## Available Methods

| Method | Description |
| --- | --- |
| `eq(String field, Object value)` | Equal to |
| `ne(String field, Object value)` | Not equal to |
| `gt / ge / lt / le` | Greater than / greater than or equal to / less than / less than or equal to |
| `like / likeLeft / likeRight` | Fuzzy matching |
| `in(String field, Collection<?> values)` | In a collection |
| `notIn(String field, Collection<?> values)` | Not in a collection |
| `between(String field, Object start, Object end)` | Closed interval |
| `isNull(String field)` / `isNotNull(String field)` | Null check |
| `add(String field, QueryOperator operator, Object... values)` | Custom operator |
| `isEmpty()` | Whether there are no conditions |
| `getConditions()` | Get the list of added conditions |
| `clear()` | Clear conditions |

All condition methods return `this`, so calls can be chained.

## Example

```java
QueryWrapper<Order> wrapper = new QueryWrapper<Order>()
        .eq("status", "PAID")
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .in("receiverCity", List.of("上海", "杭州"))
        .isNotNull("paymentTime");

List<Order> orders = orderDao.findList(wrapper);
```

A `null` `wrapper` is equivalent to querying all records, so optional parameters can be passed straight through:

```java
Page<Order> page = orderDao.findPage(wrapper, PageRequest.of(0, 20));
```

## Condition Composition Rules

`QueryWrapper` internally combines conditions only with `AND`. If `OR` semantics are needed, there are two options:

1. Use the `group` of [Annotation-Based Query Conditions](/en/core/conditions); conditions in the same group are combined with `OR`.
2. Use the `and` / `or` / `anyOf` nested grouping of the [Specification Builder](/en/core/specification).

## Generation Process

`QueryWrapper` itself does not depend on JPA; at query time its conditions are converted into Criteria `Predicate` by `JpaPlusQueryBuilder.buildPredicate`:

- An empty condition returns `conjunction()` and does not affect the result set.
- Condition field names are entity property names, not database column names.

## Applicable Scenarios

- List and paginated queries with simple conditions, all combined with `AND`.
- Dynamic queries that should be completed with minimal dependencies (only `core`).

Not applicable: scenarios requiring joins, `OR` grouping, subqueries or fetch joins; use `SpecWrapper` instead.
