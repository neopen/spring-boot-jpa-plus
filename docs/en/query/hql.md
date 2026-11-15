# HQL Queries

`HqlWrapper` builds conditions and sorting, and `JpaPlusHqlExecutor` handles execution, pagination and counting. All parameters are bound through JPA named parameters and never take part in statement concatenation.

## Capabilities

- Chained construction of `where` and `order by`, which removes most hand-written HQL strings
- Pagination returns Spring Data `Page<T>` directly, with the total counted automatically
- Supports projection, batch update and delete
- Supports custom operators and nested condition groups

Use cases: list queries with many conditions but a fixed structure, and queries such as join / subquery / aggregation that are cumbersome to express with the Criteria API. For database-vendor-specific syntax, prefer [native SQL queries](/en/query/sql).

## Executor

After the `starter` is added, the executor is registered by auto-configuration with the bean name `jpaPlusHqlExecutor`:

```java
@Service
public class OrderQueryService {

    private final JpaPlusHqlExecutor hqlExecutor;

    public OrderQueryService(JpaPlusHqlExecutor hqlExecutor) {
        this.hqlExecutor = hqlExecutor;
    }
}
```

| Property | Default Value | Description |
| --- | --- | --- |
| `neopen.jpa.enabled` | `true` | When disabled, the whole auto-configuration takes no effect |
| `neopen.jpa.hql-enabled` | `true` | When `false`, the HQL executor is not registered |

If the application defines its own `JpaPlusHqlExecutor`, auto-configuration does not override it.

## Basic Queries

```java
List<Order> orders = hqlExecutor.queryForList(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class
);
```

When there are no parameters, pass `null` as the second argument.

```java
Order order = hqlExecutor.queryForObject(
        "select o from Order o where o.orderNo = :orderNo",
        Map.of("orderNo", "DS202501010001"),
        Order.class
);
```

`queryForObject` reads at most one record and returns `null` when there is no result; it does not throw an exception when the result is not unique.

## Chainable Builder

The default alias of the root entity is `o`, which can also be specified with `HqlWrapper.create(Order.class, "ord")`.

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .eq("status", "PAID")
        .like("orderNo", "DS")
        .in("userId", userIds)
        .between("createTime", start, end)
        .orderByDesc("createTime");

List<Order> orders = hqlExecutor.queryForList(wrapper);
```

Generated:

```sql
select o from Order o
where o.status = :p1
  and o.orderNo like :p2
  and o.userId in (:p3)
  and o.createTime between :p4 and :p5
order by o.createTime desc
```

### Operators

| Method | Generated Fragment | Description |
| --- | --- | --- |
| `eq(field, value)` | `field = :p` | Generates `is null` when the value is `null` |
| `ne(field, value)` | `field <> :p` | Generates `is not null` when the value is `null` |
| `gt / ge / lt / le` | `field > / >= / < / <= :p` | Comparison operations |
| `like(field, value)` | `field like :p` | Automatically wraps the value with `%value%` |
| `likeLeft(field, value)` | `field like :p` | Automatically prepends `%` to the value |
| `likeRight(field, value)` | `field like :p` | Automatically appends `%` to the value |
| `notLike(field, value)` | `field not like :p` | Automatically wraps the value with `%value%` |
| `in(field, values)` | `field in (:p)` | An empty collection generates the always-false condition `1 = 0` |
| `notIn(field, values)` | `field not in (:p)` | An empty collection is ignored |
| `between(field, start, end)` | `field between :p1 and :p2` | Closed interval |
| `isNull(field)` | `field is null` | Null check |
| `isNotNull(field)` | `field is not null` | Not-null check |

The `like` family does not escape `%` and `_`; when the query value comes from user input and must be matched literally, escape it beforehand.

### Field Paths

Field names are property paths relative to the root entity and support nesting:

```java
HqlWrapper<OrderProduct> wrapper = HqlWrapper.create(OrderProduct.class)
        .in("order.id", orderIds);
```

Field paths are not validated against the metamodel; a wrong field is exposed at execution time.

### Nested Conditions

Conditions are combined with `AND` by default. `or(...)` combines the group with the preceding conditions through `OR`:

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .eq("status", "PAID")
        .or(w -> w.isNull("paymentTime").gt("totalAmount", 100));
```

```sql
o.status = :p1 or (o.paymentTime is null and o.totalAmount > :p2)
```

| Method | Semantics |
| --- | --- |
| `and(Consumer<HqlWrapper<T>>)` | `AND` inside the group; the group is combined with the preceding conditions through `AND` |
| `or(Consumer<HqlWrapper<T>>)` | The group is combined with the preceding conditions through `OR`, with `AND` inside |
| `anyOf(Consumer<HqlWrapper<T>>)` | `OR` inside the group; the group is combined with the preceding conditions through `AND` |

### Sorting

```java
wrapper.orderByAsc("status", "orderNo");
wrapper.orderByDesc("createTime");
wrapper.orderBy(Sort.by(Sort.Order.desc("createTime")));
```

In paginated queries, the sort carried by `Pageable` takes precedence over the builder sort; the builder sort is used only when `Pageable` is not sorted.

### Custom Operators

Implement `HqlOperator` to extend the condition syntax:

```java
HqlOperator lowerEq = new HqlOperator() {

    @Override
    public int parameterCount() {
        return 1;
    }

    @Override
    public String render(String path, List<String> parameterNames) {
        return "lower(" + path + ") = :" + parameterNames.get(0);
    }
};

HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .add("orderNo", lowerEq, "ds202501010001");
```

The built-in operator enum is `StandardHqlOperator`, which maps one to one to [ConditionOperator](/en/core/conditions).

### Annotated Conditions

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class).apply(queryParam);
```

When a query parameter field name differs from the property path, use `alias` to build a mapping. The mapping only affects the conditions resolved by `apply`:

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .alias("keyword", "order.status")
        .apply(queryParam);
```

### Raw Fragments and Subqueries

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .raw("1 = 1")
        .exists("select 1 from OrderItem i where i.order.id = o.id")
        .notExists("select 1 from Payment p where p.order.id = o.id");
```

| Method | Generated Fragment |
| --- | --- |
| `raw(String fragment)` | `fragment` |
| `exists(String subQuery)` | `exists (subQuery)` |
| `exists(String subQuery, Map<String, Object> params)` | `exists (subQuery)` with bound named parameters |
| `notExists(String subQuery)` | `not exists (subQuery)` |
| `notExists(String subQuery, Map<String, Object> params)` | `not exists (subQuery)` with bound named parameters |

Named parameters in the subquery are replaced with framework-generated names and bound:

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .exists(
                "select 1 from OrderItem i"
                        + " where i.order.id = o.id and i.sku = :sku",
                Map.of("sku", "A-1001")
        );
```

::: warning Security note
The statement structure passed to `raw`, `exists` and `notExists` is spliced into the HQL as is; only static strings written by developers may be passed in. Pass search values through the parameter map.
:::

### Grouping and Aggregation

```java
List<Tuple> rows = hqlExecutor.queryForList(
        HqlWrapper.create(Order.class)
                .select("status", "count(id)")
                .groupBy("status")
                .having("count(o.id) > 10")
);
```

| Method | Description |
| --- | --- |
| `select(String...)` | Appends select expressions; simple property names get the root alias |
| `groupBy(String...)` | Appends grouping expressions; simple property names get the root alias |
| `having(String fragment)` | Appends a group filter fragment as is; static strings only |

Without `select` the root entity is selected. `having` only goes into the main query, not the derived count statement; for fixed reporting queries pass an explicit count statement or use `SqlWrapper`.

## Pagination

### With the Builder

```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

Page<Order> page = hqlExecutor.queryForPage(wrapper, pageable);

page.getContent();
page.getTotalElements();
page.getTotalPages();
```

### With Native HQL

```java
Page<Order> page = hqlExecutor.queryForPage(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class,
        pageable
);
```

Rules for automatically deriving the count statement:

- Remove `order by`
- Downgrade `join fetch` to a plain `join`
- Splice `select count(*)` before the `from` clause

In the following cases the count statement **must** be passed explicitly:

```java
Page<Tuple> page = hqlExecutor.queryForPage(
        "select o.status, count(o) from Order o group by o.status",
        "select count(distinct o.status) from Order o",
        null,
        Tuple.class,
        PageRequest.of(0, 10)
);
```

Automatic derivation does not apply to:

- Queries containing `group by`
- `select distinct` queries
- Multiple root entities or collection joins that inflate the row count
- Complex subqueries

For aggregate queries, use pageable parameters without sorting, otherwise `order by` is appended after the grouping clause.

### List Only

```java
List<Order> orders = hqlExecutor.queryForPage(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class,
        0,
        10
);
```

This overload returns the list of the current page without counting the total; the page number starts at 0.

### Counting the Total

```java
long total = hqlExecutor.queryForCount(wrapper);
```

## Batch Queries

```java
HqlWrapper<OrderProduct> wrapper = HqlWrapper.create(OrderProduct.class)
        .in("order.id", orderIds);

List<OrderProduct> products = hqlExecutor.queryForList(wrapper);
```

When `orderIds` is empty, an always-false condition is generated, the query returns an empty list, and `in ()` never appears.

## Update and Delete

```java
int updated = hqlExecutor.executeUpdate(
        "update Order o set o.status = :status where o.id in :ids",
        Map.of("status", "CLOSED", "ids", orderIds)
);

int deleted = hqlExecutor.executeUpdate(
        "delete from Order o where o.status = :status",
        Map.of("status", "CANCELLED")
);
```

`executeUpdate` is a transactional method; when the caller already has a transaction, it joins the current transaction. Batch update and delete bypass the persistence context, so entities already loaded in the same transaction are not synchronized automatically; call `clear` or `refresh` manually when needed.

## Projection

The builder queries the root entity by default; you can also use `select` to pick columns or aggregate expressions and map them to `Tuple` or a DTO:

```java
List<OrderSummary> summaries = hqlExecutor.queryForList(
        "select new com.example.OrderSummary(o.status, count(o)) "
                + "from Order o group by o.status",
        null,
        OrderSummary.class
);
```

`OrderSummary` must provide a constructor whose parameter order matches. You can also use `queryForDtoList` / `queryForDtoPage` to map by alias, see [DTO projection](/en/core/projection).

## Limitations and Recommendations

- `HqlWrapper` is a mutable builder, not a thread-safe object; do not share it across requests.
- Field paths are not validated against the metamodel; it is recommended to manage field names centrally with constants.
- `like` does not escape wildcards, so `%` / `_` in user input take part in fuzzy matching.
- When the `in` collection comes from external input, limit its size to avoid exceeding the database parameter limit.
- Automatic count only suits simple queries on a single root entity; for complex queries, provide the count statement explicitly.
- In paginated queries, the `Pageable` sort takes precedence over the `HqlWrapper` sort.
