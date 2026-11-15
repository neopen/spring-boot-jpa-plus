# HQL 查询

`HqlWrapper` 负责构造条件与排序，`JpaPlusHqlExecutor` 负责执行、分页与统计。所有参数都通过 JPA 命名参数绑定，不参与语句拼接。

## 能力范围

- 链式构造 `where` 与 `order by`，减少手写 HQL 字符串
- 分页直接返回 Spring Data `Page<T>`，自动统计总数
- 支持投影、批量更新与删除
- 支持自定义运算符与嵌套条件分组

适用场景：条件较多但结构固定的列表查询、join / 子查询 / 聚合等 Criteria API 表达繁琐的查询。数据库厂商特性语法建议使用 [原生 SQL 查询](/query/sql)。

## 执行器

引入 `starter` 后，执行器由自动配置注册，Bean 名称为 `jpaPlusHqlExecutor`：

```java
@Service
public class OrderQueryService {

    private final JpaPlusHqlExecutor hqlExecutor;

    public OrderQueryService(JpaPlusHqlExecutor hqlExecutor) {
        this.hqlExecutor = hqlExecutor;
    }
}
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.enabled` | `true` | 关闭后整个自动配置不生效 |
| `neopen.jpa.hql-enabled` | `true` | 为 `false` 时不注册 HQL 执行器 |

业务自行定义 `JpaPlusHqlExecutor` 时，自动配置不会覆盖。

## 基础查询

```java
List<Order> orders = hqlExecutor.queryForList(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class
);
```

无参数时第二个参数传 `null`。

```java
Order order = hqlExecutor.queryForObject(
        "select o from Order o where o.orderNo = :orderNo",
        Map.of("orderNo", "DS202501010001"),
        Order.class
);
```

`queryForObject` 最多读取一条记录，无结果返回 `null`，不会因结果不唯一抛异常。

## 链式构造器

默认根实体别名为 `o`，也可以用 `HqlWrapper.create(Order.class, "ord")` 指定。

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .eq("status", "PAID")
        .like("orderNo", "DS")
        .in("userId", userIds)
        .between("createTime", start, end)
        .orderByDesc("createTime");

List<Order> orders = hqlExecutor.queryForList(wrapper);
```

生成：

```sql
select o from Order o
where o.status = :p1
  and o.orderNo like :p2
  and o.userId in (:p3)
  and o.createTime between :p4 and :p5
order by o.createTime desc
```

### 运算符

| 方法 | 生成片段 | 说明 |
| --- | --- | --- |
| `eq(field, value)` | `field = :p` | 值为 `null` 时生成 `is null` |
| `ne(field, value)` | `field <> :p` | 值为 `null` 时生成 `is not null` |
| `gt / ge / lt / le` | `field > / >= / < / <= :p` | 比较运算 |
| `like(field, value)` | `field like :p` | 自动补 `%value%` |
| `likeLeft(field, value)` | `field like :p` | 自动补 `%value` |
| `likeRight(field, value)` | `field like :p` | 自动补 `value%` |
| `notLike(field, value)` | `field not like :p` | 自动补 `%value%` |
| `in(field, values)` | `field in (:p)` | 空集合生成恒假条件 `1 = 0` |
| `notIn(field, values)` | `field not in (:p)` | 空集合直接忽略 |
| `between(field, start, end)` | `field between :p1 and :p2` | 闭区间 |
| `isNull(field)` | `field is null` | 空值判断 |
| `isNotNull(field)` | `field is not null` | 非空判断 |

`like` 系列不会转义 `%` 与 `_`；查询值来自用户输入且需要按字面量匹配时，应先自行转义。

### 字段路径

字段名是相对根实体的属性路径，支持嵌套：

```java
HqlWrapper<OrderProduct> wrapper = HqlWrapper.create(OrderProduct.class)
        .in("order.id", orderIds);
```

字段路径不做元模型校验，错误字段会在执行阶段暴露。

### 嵌套条件

条件默认 `AND` 连接。`or(...)` 把分组与前序条件用 `OR` 连接：

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .eq("status", "PAID")
        .or(w -> w.isNull("paymentTime").gt("totalAmount", 100));
```

```sql
o.status = :p1 or (o.paymentTime is null and o.totalAmount > :p2)
```

| 方法 | 语义 |
| --- | --- |
| `and(Consumer<HqlWrapper<T>>)` | 分组内部 `AND`，整体与前序条件 `AND` 连接 |
| `or(Consumer<HqlWrapper<T>>)` | 分组与前序条件 `OR` 连接，内部 `AND` |
| `anyOf(Consumer<HqlWrapper<T>>)` | 分组内部 `OR`，整体与前序条件 `AND` 连接 |

### 排序

```java
wrapper.orderByAsc("status", "orderNo");
wrapper.orderByDesc("createTime");
wrapper.orderBy(Sort.by(Sort.Order.desc("createTime")));
```

分页查询时，`Pageable` 携带的排序优先于构造器排序；只有 `Pageable` 未排序时才使用构造器排序。

### 自定义运算符

实现 `HqlOperator` 即可扩展条件语法：

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

内置运算符枚举为 `StandardHqlOperator`，与 [ConditionOperator](/core/conditions) 一一映射。

### 注解条件

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class).apply(queryParam);
```

查询参数字段名与属性路径不一致时，用 `alias` 建立映射。映射只影响 `apply` 解析出的条件：

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .alias("keyword", "order.status")
        .apply(queryParam);
```

### 原生片段与子查询

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .raw("1 = 1")
        .exists("select 1 from OrderItem i where i.order.id = o.id")
        .notExists("select 1 from Payment p where p.order.id = o.id");
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
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .exists(
                "select 1 from OrderItem i"
                        + " where i.order.id = o.id and i.sku = :sku",
                Map.of("sku", "A-1001")
        );
```

::: warning 安全提示
`raw`、`exists`、`notExists` 的语句结构会原样拼入 HQL，只能传入开发者编写的静态字符串，禁止拼接用户输入。查询值请通过参数 Map 传入。
:::

### 分组与聚合

```java
List<Tuple> rows = hqlExecutor.queryForList(
        HqlWrapper.create(Order.class)
                .select("status", "count(id)")
                .groupBy("status")
                .having("count(o.id) > 10")
);
```

| 方法 | 说明 |
| --- | --- |
| `select(String...)` | 追加 select 表达式，简单属性名自动补全别名 |
| `groupBy(String...)` | 追加分组表达式，简单属性名自动补全别名 |
| `having(String fragment)` | 追加分组过滤片段，原样拼接，仅接受静态字符串 |

未调用 `select` 时默认查询根实体。`having` 只拼入主查询，不进入自动派生的 count 语句；结构固定的报表查询建议显式传入 count 语句，或直接用 `SqlWrapper`。

## 分页

### 使用构造器

```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

Page<Order> page = hqlExecutor.queryForPage(wrapper, pageable);

page.getContent();
page.getTotalElements();
page.getTotalPages();
```

### 使用原生 HQL

```java
Page<Order> page = hqlExecutor.queryForPage(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class,
        pageable
);
```

自动派生 count 语句的规则：

- 去掉 `order by`
- 把 `join fetch` 降级为普通 `join`
- 在 `from` 子句前拼接 `select count(*)`

以下情况**必须**显式传入 count 语句：

```java
Page<Tuple> page = hqlExecutor.queryForPage(
        "select o.status, count(o) from Order o group by o.status",
        "select count(distinct o.status) from Order o",
        null,
        Tuple.class,
        PageRequest.of(0, 10)
);
```

自动派生不适用于：

- 包含 `group by` 的查询
- `select distinct` 查询
- 多根实体或集合关联导致行数放大
- 复杂子查询

聚合查询请使用未排序的分页参数，否则 `order by` 会被追加到分组语句之后。

### 只取列表

```java
List<Order> orders = hqlExecutor.queryForPage(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class,
        0,
        10
);
```

该重载返回当前页列表，不统计总数，页码从 0 开始。

### 统计总数

```java
long total = hqlExecutor.queryForCount(wrapper);
```

## 批量查询

```java
HqlWrapper<OrderProduct> wrapper = HqlWrapper.create(OrderProduct.class)
        .in("order.id", orderIds);

List<OrderProduct> products = hqlExecutor.queryForList(wrapper);
```

`orderIds` 为空时生成恒假条件，查询返回空列表，不会出现 `in ()`。

## 更新与删除

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

`executeUpdate` 是事务方法，调用方已有事务时加入当前事务。批量更新与删除绕过持久化上下文，同一事务内已加载的实体不会自动同步，需要时手动 `clear` 或 `refresh`。

## 投影

默认查询根实体；也可以先用 `select` 指定列或聚合表达式，再按 `Tuple` 或 DTO 接收：

```java
List<OrderSummary> summaries = hqlExecutor.queryForList(
        "select new com.example.OrderSummary(o.status, count(o)) "
                + "from Order o group by o.status",
        null,
        OrderSummary.class
);
```

`OrderSummary` 需要提供参数顺序匹配的构造方法。也可使用 `queryForDtoList` / `queryForDtoPage` 按别名映射，见 [DTO 投影](/core/projection)。

## 限制与建议

- `HqlWrapper` 是可变构造器，不是线程安全对象，不要跨请求共享。
- 字段路径不做元模型校验，建议用常量集中管理字段名。
- `like` 不转义通配符，用户输入包含 `%` / `_` 时参与模糊匹配。
- `in` 集合来自外部输入时应限制大小，避免超过数据库参数上限。
- 自动 count 只适合单根实体的简单查询，复杂查询显式提供 count 语句。
- 分页查询中 `Pageable` 的排序优先于 `HqlWrapper` 的排序。
