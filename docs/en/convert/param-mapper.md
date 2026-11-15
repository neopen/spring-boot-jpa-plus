# Param → Entity mapping

`ParamMapper` maps a request parameter object onto an entity for creation and partial updates. It is a separate API from Entity → VO conversion, with a different direction and purpose.

## Methods

| Method | Description |
| --- | --- |
| `static <P, E> E toEntity(P param, Class<E> entityType)` | Creates an entity and populates it |
| `static <P, E> E toEntity(P param, Supplier<E> supplier)` | Provides the entity instance via `supplier` |
| `static <P, E> void apply(P param, E entity)` | Writes parameters into an existing entity, suitable for updates |
| `static <P, E> List<E> toEntities(Collection<P> params, Class<E> entityType)` | Batch creation |
| `static <P, E> List<E> toEntities(Collection<P> params, Supplier<E> supplier)` | Batch creation, with instances provided by `supplier` |

## Mapping rules

- Field-name matching ignores case and underscores: `orderNo`, `order_no`, and `ORDER_NO` all match.
- Only **non-null** parameter fields are overwritten; fields that are `null` in the parameter keep the entity's original value, so this can be used directly for partial updates.
- Parameter fields that do not exist on the entity are ignored.
- When types are inconsistent, the conversion process determines the result; confirm the parameter types before assigning across types.

## Annotations

| Annotation | Description |
| --- | --- |
| `@ParamField("orderNo")` | Specifies the target field when the parameter field name differs from the entity field name |
| `@ParamIgnore` | Ignores this parameter field so it does not participate in mapping |

```java
public class OrderParam {

    @ParamField("orderNo")
    private String number;

    private BigDecimal totalAmount;

    @ParamIgnore
    private String clientVersion;
}
```

## Creating an entity

```java
Order order = ParamMapper.toEntity(param, Order.class);
orderRepository.save(order);
```

When default values need to be preset or a parameterized constructor is required, use a `Supplier`:

```java
Order order = ParamMapper.toEntity(param, () -> {
    Order created = new Order();
    created.setStatus("CREATED");
    return created;
});
```

## Updating an entity

```java
Order existing = orderRepository.findById(id).orElseThrow();
ParamMapper.apply(param, existing);
orderRepository.save(existing);
```

`apply` only writes non-null fields from the parameter; fields not passed in the parameter keep their original values. If the business needs "passing empty clears the value" semantics, use a fixed value or a separate marker field in the parameter object rather than relying on `null`.

## Differences from Entity → VO

| Item | Entity → VO | Param → Entity |
| --- | --- | --- |
| Direction | Entity output as a view | Request parameter written into an entity |
| Implementation | Explicit conversion function (`baseVo`) | Field-name matching + annotations |
| Null handling | Determined by the conversion function | Non-null fields overwrite |
| Typical use | Returning query results | Creation, partial updates |
