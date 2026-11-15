# 动态条件查询

`QueryWrapper` 用于在不写 Criteria API 样板代码的前提下，按运行时条件组合查询。它由 `JpaPlusDao.findList` / `findPage` 消费。

## 可用方法

| 方法 | 说明 |
| --- | --- |
| `eq(String field, Object value)` | 等于 |
| `ne(String field, Object value)` | 不等于 |
| `gt / ge / lt / le` | 大于 / 大于等于 / 小于 / 小于等于 |
| `like / likeLeft / likeRight` | 模糊匹配 |
| `in(String field, Collection<?> values)` | 属于集合 |
| `notIn(String field, Collection<?> values)` | 不属于集合 |
| `between(String field, Object start, Object end)` | 闭区间 |
| `isNull(String field)` / `isNotNull(String field)` | 空值判断 |
| `add(String field, QueryOperator operator, Object... values)` | 自定义运算符 |
| `isEmpty()` | 是否没有任何条件 |
| `getConditions()` | 取得已添加的条件列表 |
| `clear()` | 清空条件 |

所有条件方法返回 `this`，支持链式调用。

## 示例

```java
QueryWrapper<Order> wrapper = new QueryWrapper<Order>()
        .eq("status", "PAID")
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .in("receiverCity", List.of("上海", "杭州"))
        .isNotNull("paymentTime");

List<Order> orders = orderDao.findList(wrapper);
```

`wrapper` 为 `null` 时等价于查询全部，因此可以直接把可选参数透传进来：

```java
Page<Order> page = orderDao.findPage(wrapper, PageRequest.of(0, 20));
```

## 条件组合规则

`QueryWrapper` 内部只按 `AND` 组合条件。如果需要 `OR` 语义，有两个选择：

1. 使用 [注解式查询条件](/core/conditions) 的 `group`，同一分组内按 `OR` 组合。
2. 使用 [Specification 构建器](/core/specification) 的 `and` / `or` / `anyOf` 嵌套分组。

## 生成过程

`QueryWrapper` 本身不依赖 JPA，条件在查询时由 `JpaPlusQueryBuilder.buildPredicate` 转换为 Criteria `Predicate`：

- 空条件返回 `conjunction()`，不影响结果集。
- 条件字段名是实体属性名，不是数据库列名。

## 适用场景

- 条件组合简单、全部为 `AND` 的列表与分页查询。
- 需要以最小依赖（只有 `core`）完成动态查询。

不适用：需要连表、`OR` 分组、子查询或 fetch join 的场景，请使用 `SpecWrapper`。
