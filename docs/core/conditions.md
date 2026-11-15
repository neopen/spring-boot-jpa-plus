# 注解式查询条件

`@QueryCondition` 把查询参数对象的字段声明为查询条件，一次声明即可在 `SpecWrapper`、`HqlWrapper`、`SqlWrapper` 中复用。

## 注解属性

```java
@QueryCondition(
        value = "totalAmount",
        operator = ConditionOperator.BETWEEN,
        endField = "maxTotalAmount",
        ignoreBlank = true,
        group = "amount"
)
private BigDecimal minTotalAmount;
```

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `value` | `String` | `""` | 目标字段名或列名，空表示使用被标注字段名 |
| `operator` | `ConditionOperator` | `EQ` | 条件运算符 |
| `ignoreBlank` | `boolean` | `true` | 是否忽略空白字符串 |
| `endField` | `String` | `""` | 区间结束值所在字段名，仅 `BETWEEN` 使用 |
| `group` | `String` | `""` | 条件分组名，同一分组内按 `OR` 组合 |

## 运算符

| 运算符 | 参数个数 | 说明 |
| --- | --- | --- |
| `EQ` | 1 | 等于 |
| `NE` | 1 | 不等于 |
| `GT` / `GE` / `LT` / `LE` | 1 | 大于 / 大于等于 / 小于 / 小于等于 |
| `LIKE` | 1 | 两侧模糊匹配 |
| `LIKE_LEFT` | 1 | 左模糊，值前补 `%` |
| `LIKE_RIGHT` | 1 | 右模糊，值后补 `%` |
| `NOT_LIKE` | 1 | 两侧不匹配 |
| `IN` / `NOT_IN` | 1 | 集合匹配，值可以是 `Collection` 或数组 |
| `BETWEEN` | 2 | 闭区间，结束值取自 `endField` |
| `IS_NULL` | 0 | 字段为空，注解字段作为布尔开关 |
| `IS_NOT_NULL` | 0 | 字段不为空，注解字段作为布尔开关 |

## 基础用法

```java
public class OrderQueryParam {

    @QueryCondition(operator = ConditionOperator.LIKE)
    private String orderNo;

    @QueryCondition(operator = ConditionOperator.BETWEEN, endField = "maxTotalAmount")
    private BigDecimal minTotalAmount;

    private BigDecimal maxTotalAmount;
}

Specification<Order> spec = SpecWrapper.create(Order.class)
        .apply(param)
        .toSpecification();
```

`HqlWrapper` 与 `SqlWrapper` 提供同名 `apply(Object param)` 方法：

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class).apply(param);

SqlWrapper sqlWrapper = SqlWrapper.create("t_order", "o").apply(param);
```

SQL 构建器会把驼峰字段名转换为下划线列名，可用 `value` 覆盖。

## 自动跳过的值

以下值不会参与条件拼接：

- `null`
- 空白字符串（`ignoreBlank = true` 时）
- 空集合、空数组

因此查询参数对象可以直接透传，不需要在 Service 中写一堆 `if (StrUtil.isNotBlank(...))`。

## 条件分组

同一 `group` 内的条件按 `OR` 组合，不同分组之间以及与未分组条件之间按 `AND` 组合：

```java
public class OrderQueryParam {

    @QueryCondition
    private String status;

    @QueryCondition(group = "keyword", operator = ConditionOperator.LIKE)
    private String orderNo;

    @QueryCondition(group = "keyword", operator = ConditionOperator.LIKE)
    private String receiverName;
}

// status = ? and (order_no like ? or receiver_name like ?)
```

## 嵌套参数对象

字段本身没有 `@QueryCondition` 时，会递归扫描该对象内部的条件，最多三层，条件直接平铺，不加字段前缀：

```java
public class OrderQueryParam {

    @QueryCondition
    private String status;

    private KeywordFilter keyword;   // 内部字段继续按注解解析
}
```

## 布尔开关语义

`IS_NULL` / `IS_NOT_NULL` 不读取字段值本身，而是把字段当作开关：

```java
public class OrderQueryParam {

    @QueryCondition(value = "paymentTime", operator = ConditionOperator.IS_NOT_NULL)
    private Boolean paidOnly;    // true 时追加 paymentTime is not null
}
```

## 适用边界

- 条件值通过命名参数或 Criteria 绑定，不参与字符串拼接。
- 分组只支持一层 `OR`；多层嵌套请改用构建器的 `and` / `or` / `anyOf`。
- 条件默认都是实体属性名（SQL 构建器除外），不支持在 `value` 中写函数或表达式。
