# DTO Projection

`ResultMapper` maps columns of query results to DTOs, for list and pagination scenarios that return only part of the fields. Column aliases and DTO property names are matched case-insensitively and ignoring underscores.

## Alias Matching Rules

`order_no`, `orderNo`, and `ORDER_NO` all match the `orderNo` property.

```java
public class OrderSummary {

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;

    // 无参构造器 + getter / setter
}
```

## ResultMapper

| Method | Description |
| --- | --- |
| `static <T> ResultMapper<T> of(Class<T> targetType)` | Creates a mapper |
| `T map(Object row)` | Maps a single row (`Object[]` or `Map`) |
| `T map(Object[] values, String[] aliases)` | Maps with explicit aliases |
| `List<T> mapAll(Collection<?> rows)` | Maps in batch |
| `static <T> T mapRow(Object row, Class<T> targetType)` | Static single-row mapping |
| `static <T> List<T> mapRows(Collection<?> rows, Class<T> targetType)` | Static batch mapping |

## SQL Projection

```java
List<OrderSummary> list = new JpaPlusSqlExecutor(entityManager).queryForDtoList(
        "select id, order_no as orderNo, total_amount as totalAmount"
                + " from t_order where order_no like :prefix",
        Map.of("prefix", "DS-%"),
        OrderSummary.class
);
```

## HQL Projection

```java
Page<OrderSummary> page = new JpaPlusHqlExecutor(entityManager).queryForDtoPage(
        "select o.orderNo as orderNo, o.totalAmount as totalAmount"
                + " from Order o where o.orderNo like :prefix",
        Map.of("prefix", "DS-%"),
        OrderSummary.class,
        PageRequest.of(0, 10)
);
```

## Projection Methods Provided by Executors

| Method | Description |
| --- | --- |
| `queryForDtoList(sqlOrHql, params, Class<T>)` | List |
| `queryForDtoObject(sqlOrHql, params, Class<T>)` | First row, returns `null` when there is no result |
| `queryForDtoPage(sqlOrHql, params, Class<T>, Pageable)` | Pagination, count is derived automatically |
| `queryForDtoPage(..., String countSqlOrHql, ...)` | Explicit count statement |

## Type Conversion

Automatic conversion is supported for numbers, strings, booleans, characters, and enums:

- Numeric columns to `Integer`, `Long`, `BigDecimal`, `BigInteger`, etc.
- String columns to enums by matching with `valueOf`
- Boolean columns to `boolean` / `Boolean`

Columns not declared in the DTO are ignored without raising an error.

## Applicable Boundaries

- The DTO must provide a no-arg constructor.
- Nested objects and collection properties are not mapped; only flat fields are handled.
- Pagination projection derives the count statement automatically by default; for `group by` and `distinct` scenarios the count statement must be passed explicitly, otherwise the count result will differ from expectations.
