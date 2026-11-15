# Annotation-Based Query Conditions

`@QueryCondition` declares fields of a query parameter object as query conditions; a single declaration can be reused in `SpecWrapper`, `HqlWrapper` and `SqlWrapper`.

## Annotation Attributes

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

| Attribute | Type | Default Value | Description |
| --- | --- | --- | --- |
| `value` | `String` | `""` | Target field name or column name; empty means the annotated field name is used |
| `operator` | `ConditionOperator` | `EQ` | Condition operator |
| `ignoreBlank` | `boolean` | `true` | Whether to ignore blank strings |
| `endField` | `String` | `""` | Field name holding the interval end value, used only by `BETWEEN` |
| `group` | `String` | `""` | Condition group name; conditions in the same group are combined with `OR` |

## Operators

| Operator | Argument Count | Description |
| --- | --- | --- |
| `EQ` | 1 | Equal to |
| `NE` | 1 | Not equal to |
| `GT` / `GE` / `LT` / `LE` | 1 | Greater than / greater than or equal to / less than / less than or equal to |
| `LIKE` | 1 | Fuzzy matching on both sides |
| `LIKE_LEFT` | 1 | Left fuzzy matching, a `%` is prepended to the value |
| `LIKE_RIGHT` | 1 | Right fuzzy matching, a `%` is appended to the value |
| `NOT_LIKE` | 1 | No match on either side |
| `IN` / `NOT_IN` | 1 | Collection matching; the value can be a `Collection` or an array |
| `BETWEEN` | 2 | Closed interval; the end value comes from `endField` |
| `IS_NULL` | 0 | The field is null; the annotated field acts as a boolean switch |
| `IS_NOT_NULL` | 0 | The field is not null; the annotated field acts as a boolean switch |

## Basic Usage

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

`HqlWrapper` and `SqlWrapper` provide the same `apply(Object param)` method:

```java
HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class).apply(param);

SqlWrapper sqlWrapper = SqlWrapper.create("t_order", "o").apply(param);
```

The SQL builder converts camelCase field names into underscore column names, which can be overridden with `value`.

## Automatically Skipped Values

The following values do not participate in condition composition:

- `null`
- Blank strings (when `ignoreBlank = true`)
- Empty collections and empty arrays

Therefore query parameter objects can be passed straight through, without writing a pile of `if (StrUtil.isNotBlank(...))` in services.

## Condition Groups

Conditions in the same `group` are combined with `OR`; different groups, as well as groups and ungrouped conditions, are combined with `AND`:

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

## Nested Parameter Objects

When a field itself has no `@QueryCondition`, the conditions inside that object are scanned recursively, up to three levels; conditions are flattened directly without a field prefix:

```java
public class OrderQueryParam {

    @QueryCondition
    private String status;

    private KeywordFilter keyword;   // 内部字段继续按注解解析
}
```

## Boolean Switch Semantics

`IS_NULL` / `IS_NOT_NULL` do not read the field value itself; the field is treated as a switch:

```java
public class OrderQueryParam {

    @QueryCondition(value = "paymentTime", operator = ConditionOperator.IS_NOT_NULL)
    private Boolean paidOnly;    // true 时追加 paymentTime is not null
}
```

## Applicable Boundaries

- Condition values are bound through named parameters or Criteria and do not participate in string concatenation.
- Groups support only one level of `OR`; for multi-level nesting use the builder's `and` / `or` / `anyOf`.
- Conditions default to entity property names (except for the SQL builder), and functions or expressions are not supported in `value`.
