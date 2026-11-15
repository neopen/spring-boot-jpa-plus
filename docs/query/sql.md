# 原生 SQL 查询

`SqlWrapper` 构造原生 SQL，`JpaPlusSqlExecutor` 负责执行、分页与结果映射。适合需要数据库特性语法、复杂连表或手写优化语句的场景。

## 能力范围

- 链式构造 `where`、`group by`、`order by`
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
| `notExists(String subQuery)` | `not exists (subQuery)` |

片段原样拼入语句，不参与参数绑定，只能传入开发者编写的静态 SQL，禁止拼接用户输入。

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
- 包含 `group by` 或 `select distinct` 时，必须显式提供 count 语句
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
