# Param → Entity mapping

`ParamMapper` maps a request parameter object onto an entity for creation and partial updates. It is a separate API from Entity → VO conversion, with a different direction and purpose.

## Methods

| Method | Description |
| --- | --- |
| `static <P, E> E toEntity(P param, Class<E> entityType)` | Creates an entity and populates it |
| `static <P, E> E toEntity(P param, Class<E> entityType, EntityManager em)` | Creates an entity and supports `@ParamReference` |
| `static <P, E> E toEntity(P param, Supplier<E> supplier)` | Provides the entity instance via `supplier` |
| `static <P, E> E toEntity(P param, Supplier<E> supplier, EntityManager em)` | Uses `supplier` and supports references |
| `static <P, E> void apply(P param, E entity)` | Writes parameters into an existing entity, suitable for updates |
| `static <P, E> void apply(P param, E entity, EntityManager em)` | Updates an entity and supports references |
| `static <P, E> List<E> toEntities(Collection<P> params, Class<E> entityType)` | Batch creation |
| `static <P, E> List<E> toEntities(Collection<P> params, Supplier<E> supplier)` | Batch creation, with instances provided by `supplier` |
| `static <P, E> ParamMapping<P, E> mapper(Class<P>, Class<E>)` | Creates a configurable mapping |

## Mapping rules

- Field-name matching ignores case and underscores: `orderNo`, `order_no`, and `ORDER_NO` all match.
- JPA column aliases are also matched: if entity field `number` is annotated with `@Column(name = "order_no")`, a parameter field `orderNo` matches it.
- By default only **non-null** parameter fields are overwritten; fields that are `null` in the parameter keep the entity's original value, so this can be used directly for partial updates.
- Parameter fields that do not exist on the entity are ignored by default; strict mode reports them as errors.
- Common types are converted automatically: strings, numbers, booleans, characters, enums, UUIDs, temporal types, collection/array elements, and JPA `AttributeConverter` declared by `@Convert`.
- Temporal fields can use Spring's `@DateTimeFormat(pattern = "...")`; without it, ISO formats are used, and `LocalDateTime` also accepts `yyyy-MM-dd HH:mm:ss`.

## Annotations

| Annotation | Description |
| --- | --- |
| `@ParamField("orderNo")` | Specifies the target field when names differ; dotted paths such as `address.city` are supported |
| `@ParamIgnore` | Ignores this parameter field |
| `@ParamReference("customer")` | Converts a foreign-key ID into an entity reference; requires an `EntityManager` |

```java
public class OrderParam {

    @ParamField("orderNo")
    private String number;

    private BigDecimal totalAmount;

    @ParamField("address.city")
    private String city;

    @ParamReference("customer")
    private Long customerId;

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

To map associations, pass an `EntityManager`:

```java
Order order = ParamMapper.toEntity(param, Order.class, entityManager);
```

`@ParamReference` uses `EntityManager#getReference`, creating a proxy instead of loading the full row just to set a foreign key.

## Updating an entity

```java
Order existing = orderRepository.findById(id).orElseThrow();
ParamMapper.apply(param, existing);
orderRepository.save(existing);
```

`apply` only writes non-null fields from the parameter; fields not passed keep their original values.

## Configurable mapping

Use `ParamMapping` for finer control:

```java
MappingResult<Order> result = ParamMapper
        .mapper(OrderParam.class, Order.class)
        .ignore("token")
        .rename("note", "remark")
        .convert("status", (value, context) -> Status.valueOf(value.toString()))
        .nullPolicy(NullPolicy.SET_NULL)
        .ignoreJpaManaged(true)
        .strict(true)
        .map(param, existingOrder);
```

| Method | Description |
| --- | --- |
| `ignore(String...)` | Ignores the given parameter fields |
| `rename(String, String)` | Maps a parameter field to another entity field; dotted paths are supported |
| `convert(String, PropertyConverter)` | Uses a custom converter for one field |
| `nullPolicy(NullPolicy)` | `IGNORE` keeps the original value (default); `SET_NULL` clears it |
| `ignoreJpaManaged(boolean)` | Skips `@Id`, `@Version`, generated values, and audit fields |
| `strict(boolean)` | Fails when a parameter field cannot be matched |
| `map(P, E)` / `apply(P, E)` | Runs the mapping and returns a `MappingResult` |

`MappingResult` contains:

| Property | Description |
| --- | --- |
| `entity` | The mapped entity |
| `unknownFields` | Parameter fields that did not match an entity field |
| `convertedFields` | Parameter fields that were converted or resolved as references |

`NullPolicy.SET_NULL` never writes `null` into primitive fields; primitives keep their original values.

## Custom converters

Register a global converter when the built-in conversions are not enough. It applies to every `ParamMapper` and `ResultMapper`:

```java
Converters.register(new PropertyConverter() {

    @Override
    public boolean supports(ConversionContext context, Object value) {
        return context.targetType() == Money.class;
    }

    @Override
    public Object convert(Object value, ConversionContext context) {
        return Money.of(value.toString());
    }
});
```

Use the builder when only one field should be affected:

```java
ParamMapper.mapper(OrderParam.class, Order.class)
        .convert("amount", (value, context) -> new BigDecimal(value.toString()))
        .apply(param, order);
```

Converters registered later take precedence over built-in conversions, and `@Convert` on the target field takes precedence over normal enum conversion. The `ConversionContext` exposes the target field, so converters can read annotations such as `@DateTimeFormat`.

## Differences from Entity → VO

| Item | Entity → VO | Param → Entity |
| --- | --- | --- |
| Direction | Entity output as a view | Request parameter written into an entity |
| Implementation | Explicit conversion function (`baseVo`) | Field/column matching + annotations |
| Null handling | Determined by the conversion function | Non-null overwrite by default; `NullPolicy` configurable |
| Typical use | Returning query results | Creation, partial updates |
