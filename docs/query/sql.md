# 原生 SQL 查询

`SqlWrapper` 构造原生 SQL，`JpaPlusSqlExecutor` 负责执行、分页与结果映射。适合需要数据库特性语法、复杂连表或手写优化语句的场景。

## 能力范围

- 链式构造 `where`、`group by`、`having`、`union`、`order by`
- 列名自动驼峰转下划线
- 分页返回 `Page<T>`，自动派生 count
- 结果可映射为实体、`Object[]`、`Map` 或 DTO

不支持：`SqlWrapper` 没有 `join` 方法。需要在 `select` 中自行书写连表语句，或改用 HQL / Criteria。

## 执行器

由自动配置注册，Bean 名称为 `jpaPlusSqlExecutor`：

```java
@Service
public class ReportQueryService {

    private final JpaPlusSqlExecutor sqlExecutor;

    public ReportQueryService(JpaPlusSqlExecutor sqlExecutor) {
        this.sqlExecutor = sqlExecutor;
    }
}
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.sql-enabled` | `true` | 为 `false` 时不注册 SQL 执行器 |

也可以直接 `new JpaPlusSqlExecutor(entityManager)`。

## 构造 SQL

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

| 方法 | 说明 |
| --- | --- |
| `static SqlWrapper create(String table)` | 表名 |
| `static SqlWrapper create(String table, String alias)` | 表名 + 别名 |
| `select(String... columns)` | 指定列，缺省为 `*` |
| `eq / ne / gt / ge / lt / le` | 比较运算 |
| `like / likeLeft / likeRight / notLike` | 模糊匹配 |
| `in / notIn` | 集合匹配 |
| `between` | 闭区间 |
| `isNull / isNotNull` | 空值判断 |
| `add(String column, SqlOperator operator, Object... values)` | 自定义运算符 |
| `apply(Object param)` | 解析 `@QueryCondition` |
| `and / or / anyOf(Consumer<SqlWrapper>)` | 嵌套条件分组 |
| `groupBy(String... columns)` | 分组 |
| `having(String fragment)` | 分组过滤，原样拼接，仅接受静态字符串 |
| `union / unionAll(SqlWrapper...)` | 合并结构一致的多段查询 |
| `orderByAsc / orderByDesc(String... columns)` | 排序 |
| `orderBy(Sort sort)` | 按 Spring `Sort` 排序 |
| `build()` | 生成不可变 `SqlQuery` |

## 列名转换

构建器会把驼峰列名转换为下划线形式：`orderNo` → `order_no`。

注解条件同样适用，需要指定真实列名时用 `@QueryCondition(value = "order_no")` 覆盖。

已经写成下划线的列名会原样保留。

## 条件与分组

```java
SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .eq("status", "PAID")
        .anyOf(w -> w
                .like("orderNo", "DS")
                .like("receiverName", "张"));
```

语义与 [Specification 构建器](/core/specification) 一致：

| 方法 | 语义 |
| --- | --- |
| `and(...)` | 分组内部 `AND`，整体与前序条件 `AND` |
| `or(...)` | 分组与前序条件 `OR`，内部 `AND` |
| `anyOf(...)` | 分组内部 `OR`，整体与前序条件 `AND` |

## 原生片段与子查询

```java
SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .raw("1 = 1")
        .exists("select 1 from t_order_item i where i.order_id = o.id")
        .notExists("select 1 from t_payment p where p.order_id = o.id");
```

| 方法 | 生成片段 |
| --- | --- |
| `raw(String fragment)` | `fragment` |
| `exists(String subQuery)` | `exists (subQuery)` |
| `exists(String subQuery, Map<String, Object> params)` | `exists (subQuery)`，绑定命名参数 |
| `notExists(String subQuery)` | `not exists (subQuery)` |
| `notExists(String subQuery, Map<String, Object> params)` | `not exists (subQuery)`，绑定命名参数 |

子查询中的 `:参数名` 会替换为框架生成的参数名并完成绑定：

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .exists(
                "select 1 from t_order_item i"
                        + " where i.order_id = o.id and i.sku = :sku",
                Map.of("sku", "A-1001")
        );
```

::: warning 安全提示
`raw`、`exists`、`notExists` 的语句结构会原样拼入 SQL，只能传入开发者编写的静态字符串，禁止拼接用户输入。查询值请通过参数 Map 传入。
:::

## 分组与聚合

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .select("status", "count(*) as total")
        .groupBy("status")
        .having("count(*) > 10");

Page<Object[]> page = sqlExecutor.queryForPage(wrapper, PageRequest.of(0, 10));
```

| 方法 | 说明 |
| --- | --- |
| `select(String...)` | 追加 select 列或表达式，原样使用 |
| `groupBy(String...)` | 追加分组列，简单列名自动补全别名 |
| `having(String fragment)` | 追加分组过滤片段，原样拼接，仅接受静态字符串 |

包含分组和 `having` 时，`getCountSql()` 会把整条查询包装为子查询统计，因此分页总数与分组结果一致。

## union 合并

`union` 与 `unionAll` 用于合并结构一致的多张表，例如业务表与归档表：

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

生成：

```sql
select o.id, o.order_no, o.status from t_order o where o.status = :p1
union
select a.id, a.order_no, a.status from t_order_archive a where a.status = :p2
order by id desc
```

| 方法 | 说明 |
| --- | --- |
| `union(SqlWrapper...)` | 合并并去重 |
| `unionAll(SqlWrapper...)` | 合并并保留重复行 |
| `hasUnion()` | 是否包含 union 子查询 |
| `getSortAlias()` | union 查询返回 `null`，表示排序不带表别名 |

规则：

- 各段 `select` 的列数量和顺序必须一致。
- 子查询自身不能包含 `order by`，会直接抛出 `IllegalArgumentException`；排序统一写在最外层构造器上，作用整个结果集。
- 不支持嵌套 union，需要对子查询再 union 时请拆成多段一次传入。
- 参数在合并时统一重新编号，不会出现命名冲突。
- 分页统计会把整个 union 结果包装为子查询，总数正确。

`queryForPage(wrapper, pageable)` 传入 `Pageable` 排序时会按输出列名追加，不会补全表别名。

注解条件需要把参数名映射到列名时使用 `alias`。字段名先按映射替换，再执行驼峰转下划线：

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .alias("keyword", "order_no")
        .apply(queryParam);
```

## 执行

```java
List<Object[]> rows = sqlExecutor.queryForList(query);
```

按 Java 类型映射结果：

```java
List<BigDecimal> amounts = sqlExecutor.queryForList(
        "select total_amount from t_order where status = :status",
        Map.of("status", "PAID"),
        BigDecimal.class
);
```

## 分页

```java
Page<OrderSummary> page = sqlExecutor.queryForDtoPage(
        "select o.id, o.order_no as orderNo, o.total_amount as totalAmount"
                + " from t_order o where o.status = :status",
        Map.of("status", "PAID"),
        OrderSummary.class,
        PageRequest.of(0, 10)
);
```

也可以执行构造器生成的分页语句：

```java
Page<Object[]> page = sqlExecutor.queryForPage(wrapper, PageRequest.of(0, 10));
```

分页规则：

- 页码从 0 开始
- 总数来自 `SqlQuery.getCountSql()`
- 统计语句始终按子查询包装，包含 `group by`、`having` 或聚合列时结果同样正确
- `Pageable` 的排序优先于 `SqlWrapper` 的排序

统计：

```java
long total = sqlExecutor.queryForCount(wrapper);
```

## 更新与删除

```java
int updated = sqlExecutor.executeUpdate(
        "update t_order set status = :status where id in (:ids)",
        Map.of("status", "CLOSED", "ids", ids)
);
```

`executeUpdate` 是事务方法，调用方已有事务时加入当前事务。

## 与 HQL 的差异

| 项 | HQL | 原生 SQL |
| --- | --- | --- |
| 查询对象 | 实体与属性名 | 表与列名 |
| 列名转换 | 不转换 | 驼峰转下划线 |
| join | 可在语句中书写，构造器无 join | 只能写在语句中 |
| 结果映射 | 实体 / 构造器投影 / DTO | 实体 / DTO / `Object[]` / `Map` |
| 逻辑删除 | 不自动追加 | 不自动追加 |
| 数据库可移植性 | 由 JPA 方言处理 | 依赖具体数据库 |

两种执行器都通过命名参数绑定，禁止把用户输入拼进 SQL 文本。
