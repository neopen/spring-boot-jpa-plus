# Native SQL Queries

`SqlWrapper` builds native SQL, and `JpaPlusSqlExecutor` handles execution, pagination and result mapping. It fits scenarios that need database-specific syntax, complex joins or hand-written optimized statements.

## Capabilities

- Chained construction of `where`, `group by` and `order by`
- Column names are converted from camel case to underscores automatically
- Pagination returns `Page<T>` with the count derived automatically
- Results can be mapped to entities, `Object[]`, `Map` or DTOs

Not supported: `SqlWrapper` has no `join` method. Write the join in the `select` statement yourself, or switch to HQL / Criteria.

## Executor

Registered by auto-configuration with the bean name `jpaPlusSqlExecutor`:

```java
@Service
public class ReportQueryService {

    private final JpaPlusSqlExecutor sqlExecutor;

    public ReportQueryService(JpaPlusSqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }
}
```

| Property | Default Value | Description |
| --- | --- | --- |
| `neopen.jpa.sql-enabled` | `true` | When `false`, the SQL executor is not registered |

You can also use `new JpaPlusSqlExecutor(entityManager)` directly.

## Building SQL

```java
SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .select("o.id", "o.order_no", "o.total_amount")
        .eq("status", "PAID")
        .like("orderNo", "DS")
        .between("totalAmount", start, end)
        .groupBy("status")
        .orderByDesc("totalAmount");

SqlQuery query = wrapper.build();
query.getSql();
query.getCountSql();
query.getParameters();
```

| Method | Description |
| --- | --- |
| `static SqlWrapper create(String table)` | Table name |
| `static SqlWrapper create(String table, String alias)` | Table name + alias |
| `select(String... columns)` | Specifies the columns; defaults to `*` |
| `eq / ne / gt / ge / lt / le` | Comparison operations |
| `like / likeLeft / likeRight / notLike` | Fuzzy matching |
| `in / notIn` | Collection matching |
| `between` | Closed interval |
| `isNull / isNotNull` | Null check |
| `add(String column, SqlOperator operator, Object... values)` | Custom operator |
| `apply(Object param)` | Resolves `@QueryCondition` |
| `and / or / anyOf(Consumer<SqlWrapper>)` | Nested condition groups |
| `groupBy(String... columns)` | Grouping |
| `orderByAsc / orderByDesc(String... columns)` | Sorting |
| `orderBy(Sort sort)` | Sorts by Spring `Sort` |
| `build()` | Generates an immutable `SqlQuery` |

## Column Name Conversion

The builder converts camel-case column names to the underscore form: `orderNo` → `order_no`.

The same applies to annotated conditions; when the real column name must be specified, override it with `@QueryCondition(value = "order_no")`.

Column names already written with underscores are kept as is.

## Conditions and Groups

```java
SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .eq("status", "PAID")
        .anyOf(w -> w
                .like("orderNo", "DS")
                .like("receiverName", "张"));
```

The semantics are the same as the [Specification builder](/en/core/specification):

| Method | Semantics |
| --- | --- |
| `and(...)` | `AND` inside the group; the group is combined with the preceding conditions through `AND` |
| `or(...)` | The group is combined with the preceding conditions through `OR`, with `AND` inside |
| `anyOf(...)` | `OR` inside the group; the group is combined with the preceding conditions through `AND` |

## Raw Fragments and Subqueries

```java
SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .raw("1 = 1")
        .exists("select 1 from t_order_item i where i.order_id = o.id")
        .notExists("select 1 from t_payment p where p.order_id = o.id");
```

| Method | Generated Fragment |
| --- | --- |
| `raw(String fragment)` | `fragment` |
| `exists(String subQuery)` | `exists (subQuery)` |
| `notExists(String subQuery)` | `not exists (subQuery)` |

Fragments are spliced into the statement as is and do not take part in parameter binding; only static SQL written by developers may be passed in, and concatenating user input is forbidden.

Use `alias` when annotated conditions need to map a parameter name to a column name. Field names are first replaced according to the mapping, and then converted from camel case to underscores:

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .alias("keyword", "order_no")
        .apply(queryParam);
```

## Execution

```java
List<Object[]> rows = sqlExecutor.queryForList(query);
```

Map results by Java type:

```java
List<BigDecimal> amounts = sqlExecutor.queryForList(
        "select total_amount from t_order where status = :status",
        Map.of("status", "PAID"),
        BigDecimal.class
);
```

## Pagination

```java
Page<OrderSummary> page = sqlExecutor.queryForDtoPage(
        "select o.id, o.order_no as orderNo, o.total_amount as totalAmount"
                + " from t_order o where o.status = :status",
        Map.of("status", "PAID"),
        OrderSummary.class,
        PageRequest.of(0, 10)
);
```

You can also execute the paginated statement generated by the builder:

```java
Page<Object[]> page = sqlExecutor.queryForPage(wrapper, PageRequest.of(0, 10));
```

Pagination rules:

- The page number starts at 0
- The total comes from `SqlQuery.getCountSql()`
- When `group by` or `select distinct` is present, the count statement must be provided explicitly
- The `Pageable` sort takes precedence over the `SqlWrapper` sort

Counting:

```java
long total = sqlExecutor.queryForCount(wrapper);
```

## Update and Delete

```java
int updated = sqlExecutor.executeUpdate(
        "update t_order set status = :status where id in (:ids)",
        Map.of("status", "CLOSED", "ids", ids)
);
```

`executeUpdate` is a transactional method; when the caller already has a transaction, it joins the current transaction.

## Differences from HQL

| Item | HQL | Native SQL |
| --- | --- | --- |
| Query target | Entities and property names | Tables and column names |
| Column name conversion | No conversion | Camel case to underscores |
| join | Can be written in the statement; the builder has no join | Can only be written in the statement |
| Result mapping | Entity / constructor projection / DTO | Entity / DTO / `Object[]` / `Map` |
| Logic delete | Not appended automatically | Not appended automatically |
| Database portability | Handled by the JPA dialect | Depends on the specific database |

Both executors bind parameters through named parameters; splicing user input into SQL text is forbidden.
