# 参数 → 实体映射

`ParamMapper` 把请求参数对象映射到实体，用于创建与部分更新。它与 Entity → VO 的转换是两套独立 API，方向与用途都不同。

## 方法

| 方法 | 说明 |
| --- | --- |
| `static <P, E> E toEntity(P param, Class<E> entityType)` | 创建实体并填充 |
| `static <P, E> E toEntity(P param, Supplier<E> supplier)` | 由 `supplier` 提供实体实例 |
| `static <P, E> void apply(P param, E entity)` | 把参数写入已有实体，适合更新 |
| `static <P, E> List<E> toEntities(Collection<P> params, Class<E> entityType)` | 批量创建 |
| `static <P, E> List<E> toEntities(Collection<P> params, Supplier<E> supplier)` | 批量创建，实例由 `supplier` 提供 |

## 映射规则

- 字段名匹配忽略大小写与下划线：`orderNo`、`order_no`、`ORDER_NO` 都能匹配。
- 只覆盖**非空**的参数字段，参数中为 `null` 的字段保留实体原值，因此可以直接用于部分更新。
- 参数字段在实体中不存在时忽略。
- 类型不一致时由转换过程决定结果，跨类型赋值前请先确认参数类型。

## 注解

| 注解 | 说明 |
| --- | --- |
| `@ParamField("orderNo")` | 参数字段与实体字段名不一致时指定目标字段 |
| `@ParamIgnore` | 忽略该参数字段，不参与映射 |

```java
public class OrderParam {

    @ParamField("orderNo")
    private String number;

    private BigDecimal totalAmount;

    @ParamIgnore
    private String clientVersion;
}
```

## 创建实体

```java
Order order = ParamMapper.toEntity(param, Order.class);
orderRepository.save(order);
```

需要预设默认值或使用带参构造器时，用 `Supplier`：

```java
Order order = ParamMapper.toEntity(param, () -> {
    Order created = new Order();
    created.setStatus("CREATED");
    return created;
});
```

## 更新实体

```java
Order existing = orderRepository.findById(id).orElseThrow();
ParamMapper.apply(param, existing);
orderRepository.save(existing);
```

`apply` 只写入参数中非空的字段，参数未传的字段保持原值。如果业务需要"传空即清空"的语义，应在参数对象中使用固定值或单独的标记字段，而不是依赖 `null`。

## 与 Entity → VO 的区别

| 项 | Entity → VO | Param → Entity |
| --- | --- | --- |
| 方向 | 实体输出为视图 | 请求参数写入实体 |
| 实现方式 | 显式转换函数（`baseVo`） | 字段名匹配 + 注解 |
| 空值处理 | 由转换函数决定 | 非空字段覆盖 |
| 典型用途 | 查询结果返回 | 新增、部分更新 |
