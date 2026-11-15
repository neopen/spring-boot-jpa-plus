# Native SQL Queries

`SqlWrapper` builds native SQL, and `JpaPlusSqlExecutor` handles execution, pagination and result mapping. It fits scenarios that need database-specific syntax, complex joins or hand-written optimized statements.

## Capabilities

- Chained construction of `where`, `group by`, `having`, `union` and `order by`
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
| `having(String fragment)` | Group filter spliced as is; static strings only |
| `union / unionAll(SqlWrapper...)` | Merges query parts with the same shape |
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
| `exists(String subQuery, Map<String, Object> params)` | `exists (subQuery)` with bound named parameters |
| `notExists(String subQuery)` | `not exists (subQuery)` |
| `notExists(String subQuery, Map<String, Object> params)` | `not exists (subQuery)` with bound named parameters |

Named parameters in the subquery are replaced with framework-generated names and bound:

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .exists(
                "select 1 from t_order_item i"
                        + " where i.order_id = o.id and i.sku = :sku",
                Map.of("sku", "A-1001")
        );
```

::: warning Security note
The statement structure passed to `raw`, `exists` and `notExists` is spliced into the SQL as is; only static strings written by developers may be passed in. Pass search values through the parameter map.
:::

## Grouping and Aggregation

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .select("status", "count(*) as total")
        .groupBy("status")
        .having("count(*) > 10");

Page<Object[]> page = sqlExecutor.queryForPage(wrapper, PageRequest.of(0, 10));
```

| Method | Description |
| --- | --- |
| `select(String...)` | Appends select columns or expressions as is |
| `groupBy(String...)` | Appends grouping columns; simple names get the table alias |
| `having(String fragment)` | Appends a group filter as is; static strings only |

When grouping is used, `getCountSql()` wraps the whole query as a subquery, so the total matches the grouped result.

## union

`union` and `unionAll` merge tables with the same shape, such as a business table and an archive table:

```java
SqlWrapper archived = SqlWrapper.create("t_order_archive", "a")
        .select("a.id", "a.order_no", "a.status")
        .eq("a.status", "CLOSED");

SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .select("o.id", "o.order_no", "o.status")
        .eq("status", "PAID")
        .union(archived)
        .orderByDesc("id");

Page<Object[]> page = sqlExecutor.queryForPage(wrapper, PageRequest.of(0, 10));
```

Generated SQL:

```sql
select o.id, o.order_no, o.status from t_order o where o.status = :p1
union
select a.id, a.order_no, a.status from t_order_archive a where a.status = :p2
order by id desc
```

| Method | Description |
| --- | --- |
| `union(SqlWrapper...)` | Merges and removes duplicates |
| `unionAll(SqlWrapper...)` | Merges and keeps duplicates |
| `hasUnion()` | Whether the builder contains a union part |
| `getSortAlias()` | Returns `null` for unions so sorting uses output column names |

Rules:

- Every part must select the same number of columns in the same order.
- A union part must not contain `order by`; it throws `IllegalArgumentException`. Put sorting on the outer builder so it applies to the whole result.
- Nested unions are not supported; pass all parts in one call instead.
- Parameters are renumbered while merging, so names never collide.
- Pagination counts the whole union result through a subquery, so totals stay correct.

With `queryForPage(wrapper, pageable)`, a sorted `Pageable` is appended against output column names without a table alias.

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
- The count statement is always wrapped as a subquery, so `group by`, `having` and aggregate columns are counted correctly
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
