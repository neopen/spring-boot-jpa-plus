# DTO 投影

`ResultMapper` 把查询结果的列映射到 DTO，用于只返回部分字段的列表与分页场景。列别名与 DTO 属性名按忽略大小写、忽略下划线的规则匹配。

## 别名匹配规则

`order_no`、`orderNo`、`ORDER_NO` 都会命中 `orderNo` 属性。

```java
public class OrderSummary {

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;

    // 无参构造器 + getter / setter
}
```

## ResultMapper

| 方法 | 说明 |
| --- | --- |
| `static <T> ResultMapper<T> of(Class<T> targetType)` | 创建映射器 |
| `T map(Object row)` | 映射单行（`Object[]` 或 `Map`） |
| `T map(Object[] values, String[] aliases)` | 按显式别名映射 |
| `List<T> mapAll(Collection<?> rows)` | 批量映射 |
| `static <T> T mapRow(Object row, Class<T> targetType)` | 静态单行映射 |
| `static <T> List<T> mapRows(Collection<?> rows, Class<T> targetType)` | 静态批量映射 |

## SQL 投影

```java
List<OrderSummary> list = new JpaPlusSqlExecutor(entityManager).queryForDtoList(
        "select id, order_no as orderNo, total_amount as totalAmount"
                + " from t_order where order_no like :prefix",
        Map.of("prefix", "DS-%"),
        OrderSummary.class
);
```

## HQL 投影

```java
Page<OrderSummary> page = new JpaPlusHqlExecutor(entityManager).queryForDtoPage(
        "select o.orderNo as orderNo, o.totalAmount as totalAmount"
                + " from Order o where o.orderNo like :prefix",
        Map.of("prefix", "DS-%"),
        OrderSummary.class,
        PageRequest.of(0, 10)
);
```

## 执行器提供的投影方法

| 方法 | 说明 |
| --- | --- |
| `queryForDtoList(sqlOrHql, params, Class<T>)` | 列表 |
| `queryForDtoObject(sqlOrHql, params, Class<T>)` | 首行，无结果返回 `null` |
| `queryForDtoPage(sqlOrHql, params, Class<T>, Pageable)` | 分页，自动派生 count |
| `queryForDtoPage(..., String countSqlOrHql, ...)` | 显式 count 语句 |

## 类型转换

支持数字、字符串、布尔、字符与枚举的自动转换：

- 数值列到 `Integer`、`Long`、`BigDecimal`、`BigInteger` 等
- 字符串列到枚举按 `valueOf` 匹配
- 布尔列到 `boolean` / `Boolean`

未在 DTO 中声明的列会被忽略，不会报错。

## 适用边界

- DTO 必须提供无参构造器。
- 嵌套对象、集合属性不做映射，只处理扁平字段。
- 分页投影默认自动派生 count 语句；`group by`、`distinct` 场景必须显式传入 count 语句，否则计数结果与预期不一致。
