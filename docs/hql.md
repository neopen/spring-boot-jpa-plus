# HQL 使用文档

本文档介绍 Spring Boot JPA Plus 的 HQL 数据访问能力，包括链式条件构造、分页、统计、批量查询和自定义运算符。

## 1. 能力范围

`JpaPlusHqlExecutor` 面向实体查询，提供以下能力：

- 基于 JPA 命名参数执行 HQL，参数值不参与语句拼接。
- 链式构造器 `HqlWrapper`，减少手写 HQL 字符串。
- 分页查询直接返回 Spring Data `Page<T>`，自动统计总数。
- 支持投影、更新、删除等原生 HQL 用法。

适用场景：

- 条件较多但结构相对固定的列表查询。
- 需要 join、子查询、聚合等 Criteria API 表达繁琐的查询。
- 按主键集合批量回填关联数据。

数据库特性语法（如窗口函数、厂商函数）建议使用 `JpaPlusSqlExecutor` 或原生 SQL。

## 2. 快速开始

引入 starter 后，执行器由自动配置注册，直接注入即可：

```java
@Service
public class OrderQueryService {

    private final JpaPlusHqlExecutor hqlExecutor;

    public OrderQueryService(JpaPlusHqlExecutor hqlExecutor) {
        this.hqlExecutor = hqlExecutor;
    }
}
```

相关配置：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.enabled` | `true` | 关闭后整个 JPA Plus 自动配置不生效 |
| `neopen.jpa.hql-enabled` | `true` | 为 `false` 时不注册 HQL 执行器 |

自动配置注册的 Bean 名称为 `jpaPlusHqlExecutor`。如果业务自行定义 `JpaPlusHqlExecutor`，默认实现不会覆盖。

## 3. 基础查询

### 3.1 列表查询

```java
List<Order> orders = hqlExecutor.queryForList(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class
);
```

无参数时第二个参数传 `null`。

### 3.2 单条查询

```java
Order order = hqlExecutor.queryForObject(
        "select o from Order o where o.orderNo = :orderNo",
        Map.of("orderNo", "DS202501010001"),
        Order.class
);
```

`queryForObject` 最多读取一条记录，无结果时返回 `null`，不会因为结果不唯一而抛异常。

### 3.3 原生分页

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

## 4. 链式查询构造器

`HqlWrapper` 用于替代手写 where 条件。默认根实体别名为 `o`。

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .eq("status", "PAID")
        .like("orderNo", "DS")
        .in("userId", userIds)
        .between("createTime", start, end)
        .orderByDesc("createTime");

List<Order> orders = hqlExecutor.queryForList(wrapper);
```

生成的条件：

```hql
select o from Order o
where o.status = :p1
  and o.orderNo like :p2
  and o.userId in (:p3)
  and o.createTime between :p4 and :p5
order by o.createTime desc
```

### 4.1 运算符

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

`like` 系列不会转义 `%` 和 `_`。如果查询值来自用户输入且需要按字面量匹配，应先自行转义。

### 4.2 字段路径

字段名是相对根实体的属性路径，支持嵌套路径：

```java
HqlWrapper<OrderProduct> wrapper = HqlWrapper.create(OrderProduct.class)
        .in("order.id", orderIds);
```

也可以直接传入带别名的完整路径，执行器不会重复添加别名。

字段路径不会做实体元模型校验，错误字段会在 HQL 执行阶段暴露。

### 4.3 嵌套条件

条件默认使用 `AND` 连接。`or(...)` 会把分组与前面的条件用 `OR` 连接：

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .eq("status", "PAID")
        .or(w -> w.isNull("paymentTime").gt("totalAmount", 100));
```

生成：

```hql
o.status = :p1 or (o.paymentTime is null and o.totalAmount > :p2)
```

分组内部同样默认 `AND`。`and(...)` 用于在 `OR` 分组内继续嵌套：

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class)
        .or(w -> w
                .eq("status", "PAID")
                .or(n -> n.eq("status", "SHIPPED").eq("userId", 1L)));
```

### 4.4 排序

```java
wrapper.orderByAsc("status", "orderNo");
wrapper.orderByDesc("createTime");
wrapper.orderBy(Sort.by(Sort.Order.desc("createTime")));
```

分页查询时，`Pageable` 携带的排序优先于构造器中的排序；只有 `Pageable` 未排序时才使用构造器的排序。

### 4.5 自定义运算符

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

## 5. 分页查询

### 5.1 使用构造器

```java
Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

Page<Order> page = hqlExecutor.queryForPage(wrapper, pageable);

page.getContent();
page.getTotalElements();
page.getTotalPages();
```

总数通过构造器生成的 count 语句统计。

### 5.2 使用原生 HQL

```java
Page<Order> page = hqlExecutor.queryForPage(
        "select o from Order o where o.status = :status",
        Map.of("status", "PAID"),
        Order.class,
        pageable
);
```

该重载会自动派生 count 语句：

- 去掉 `order by`；
- 将 `join fetch` 降级为普通 `join`；
- 在 from 子句前拼接 `select count(*)`。

以下情况需要显式传入 count 语句：

```java
Page<Tuple> page = hqlExecutor.queryForPage(
        "select o.status, count(o) from Order o group by o.status",
        "select count(distinct o.status) from Order o",
        null,
        Tuple.class,
        PageRequest.of(0, 10)
);
```

聚合查询请使用未排序的分页参数，否则执行器会把 `order by` 追加到分组语句之后。

自动派生不适用于：

- 包含 `group by` 的查询；
- `select distinct` 查询；
- 多根实体或集合关联，可能出现行数放大；
- 复杂子查询。

### 5.3 统计总数

```java
long total = hqlExecutor.queryForCount(wrapper);
```

## 6. 批量查询

按主键集合一次查询关联数据，避免循环查询：

```java
HqlWrapper<OrderProduct> wrapper = HqlWrapper.create(OrderProduct.class)
        .in("order.id", orderIds);

List<OrderProduct> products = hqlExecutor.queryForList(wrapper);
```

`orderIds` 为空时不会生成 `in ()`，而是生成恒假条件，查询返回空列表。

## 7. 更新与删除

```java
int updated = hqlExecutor.executeUpdate(
        "update Order o set o.status = :status where o.id in :ids",
        Map.of("status", "CLOSED", "ids", orderIds)
);
```

```java
int deleted = hqlExecutor.executeUpdate(
        "delete from Order o where o.status = :status",
        Map.of("status", "CANCELLED")
);
```

`executeUpdate` 方法是事务方法，调用方已有事务时会加入当前事务。批量更新和删除会绕过持久化上下文，同一事务内已经加载的实体不会自动同步；如有需要，请手动 `clear` 或 `refresh`。

## 8. 投影查询

构造器固定查询根实体。需要投影时使用原生 HQL：

```java
List<OrderSummary> summaries = hqlExecutor.queryForList(
        "select new com.example.OrderSummary(o.status, count(o)) "
                + "from Order o group by o.status",
        null,
        OrderSummary.class
);
```

`OrderSummary` 需要提供参数顺序匹配的构造方法。

## 9. 限制与建议

- `HqlWrapper` 是可变的链式构造器，不是线程安全对象；不要在多个请求间共享同一个实例。
- 字段路径不做元模型校验，建议通过常量或枚举集中管理字段名。
- `like` 不转义通配符，用户输入包含 `%` 或 `_` 时会参与模糊匹配。
- `in` 集合来自外部输入时，应限制集合大小，避免超过数据库参数上限。
- 自动 count 适用于单根实体的简单查询；复杂查询请显式提供 count 语句。
- 分页查询中 `Pageable` 的排序优先于 `HqlWrapper` 的排序。

## 10. 示例工程

完整用法见：

- `examples/spring-boot-jpa-plus-demo` 的 `OrderServiceImpl`
- `examples/spring-boot-jpa-plus-demo` 的 `OrderProductServiceImpl`
