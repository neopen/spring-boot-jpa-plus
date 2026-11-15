# Specification 构建器

`SpecWrapper` 用链式调用构造 Spring Data JPA 的 `Specification`，条件默认按 `AND` 组合。它同时支持字段名字符串与 JPA Metamodel 属性两种写法。

## 创建与输出

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .toSpecification();

Page<Order> page = orderRepository.findAll(spec, PageRequest.of(0, 10));
```

| 方法 | 说明 |
| --- | --- |
| `static <T> SpecWrapper<T> create(Class<T> entityType)` | 创建构造器 |
| `Class<T> getEntityType()` | 取得实体类型 |
| `Specification<T> toSpecification()` | 生成 `Specification` |
| `Predicate toPredicate(...)` | 直接参与 Criteria 构建，`SpecWrapper` 本身实现了 `Specification` |

## 条件方法

| 方法 | 说明 |
| --- | --- |
| `eq / ne / gt / ge / lt / le(String field, Object value)` | 比较运算 |
| `like / likeLeft / likeRight / notLike(String field, String value)` | 模糊匹配 |
| `in / notIn(String field, Collection<?> values)` | 集合匹配 |
| `between(String field, Object start, Object end)` | 闭区间 |
| `isNull / isNotNull(String field)` | 空值判断 |
| `add(String field, SpecOperator operator, Object... values)` | 自定义运算符 |
| `apply(Object param)` | 从注解参数对象批量追加条件 |
| `raw(Specification<T> specification)` | 追加原生 `Specification`（子查询、字段间比较等） |

## 类型安全写法

生成静态 Metamodel 后，可以用属性对象替代字符串，避免字段名写错：

```java
SpecWrapper.create(Order.class)
        .eq(Order_.status, "PAID")
        .isNotNull(Order_.paymentTime)
        .toSpecification();
```

Metamodel 重载接受的参数类型为 `SingularAttribute<? super T, ?>`，覆盖 `eq`、`ne`、`gt`、`ge`、`lt`、`le`、`like`、`likeLeft`、`likeRight`、`notLike`、`in`、`notIn`、`between`、`isNull`、`isNotNull`。

## 连表

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .join("orderProducts")
        .like("orderProducts.productName", "机械")
        .toSpecification();
```

| 方法 | 说明 |
| --- | --- |
| `join(String attribute)` | 内连接，别名与属性名相同 |
| `join(String attribute, JoinType joinType)` | 指定连接类型 |
| `join(String attribute, String alias)` | 指定别名，内连接 |
| `join(String attribute, String alias, JoinType joinType)` | 指定别名与连接类型 |

连表后会自动开启 `distinct`，避免一对多关联导致分页结果重复、计数偏大。也可以显式控制：

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

`fetch(String attribute)` 默认使用 `LEFT`，也可通过 `fetch(String attribute, JoinType joinType)` 指定。fetch join 在 count 查询中会被跳过。

注意：fetch join 配合分页时，JPA 一般会退化为内存分页。需要数据库侧分页请使用 [Specification 执行器](/core/executor) 的 `findPage(spec, pageable, fetchAttributes)`。

## 嵌套条件分组

```java
SpecWrapper.create(Order.class)
        .eq("status", "PAID")
        .anyOf(w -> w
                .like("orderNo", "DS")
                .like("receiverName", "张"))
        .toSpecification();

// status = ? and (order_no like ? or receiver_name like ?)
```

| 方法 | 语义 |
| --- | --- |
| `and(Consumer<SpecWrapper<T>> nested)` | 分组内部 `AND`，分组整体按 `AND` 连接 |
| `or(Consumer<SpecWrapper<T>> nested)` | 分组与前序条件按 `OR` 连接，分组内部 `AND` |
| `anyOf(Consumer<SpecWrapper<T>> nested)` | 分组内部 `OR`，分组整体与前序条件按 `AND` 连接 |

## 注解条件与逻辑删除

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .apply(queryParam)      // 解析 @QueryCondition
        .filterDeleted()        // 追加 @LogicDelete 过滤
        .toSpecification();
```

`filterDeleted()` 只对标注了 `@LogicDelete` 的实体生效，否则原样返回。详见 [注解式查询条件](/core/conditions) 与 [逻辑删除](/core/logic-delete)。

## 原生 Specification

子查询、字段间比较等无法用链式方法表达时，用 `raw` 追加：

```java
SpecWrapper.create(Product.class)
        .raw((root, query, cb) -> cb.lessThanOrEqualTo(
                root.<Integer> get("stockQuantity"),
                root.<Integer> get("safetyStock")))
        .toSpecification();
```
