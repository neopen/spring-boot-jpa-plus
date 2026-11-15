# Safe Sorting

`Sorts` parses externally supplied sort expressions into Spring Data `Sort` and validates fields and directions, preventing the frontend from passing arbitrary property names.

## Methods

| Method | Description |
| --- | --- |
| `static Sort parse(String expression, Class<?> entityType)` | Validate against entity fields |
| `static Sort parse(String expression, Class<?> entityType, Map<String, String> aliases)` | Support alias mapping from external field names to entity properties |
| `static Sort parse(String expression, Set<String> allowedFields)` | Validate against an explicit allowlist |

## Expression Format

```java
Sort sort = Sorts.parse("createTime,desc;id,asc", Order.class);
Sort allowed = Sorts.parse("id,desc", Set.of("id", "totalAmount"));
```

- Multiple sort fields are separated by `;`
- The field name and direction of the same field are separated by `,`
- Only `asc` / `desc` are accepted as directions
- An empty expression returns `Sort.unsorted()`

## Validation Rules

- Non-existent field or invalid direction: throws `IllegalArgumentException`
- Nested paths are validated segment by segment, up to three levels:

```java
Sort sort = Sorts.parse("customer.name,asc", Order.class);
```

- The target field of an alias mapping is validated as well; an alias pointing to a non-existent property is rejected too:

```java
Sort sort = Sorts.parse(
        "name,desc",
        Order.class,
        Map.of("name", "customer.name")
);
```

## Where to Use It

```java
Page<Order> page = orderRepository.findAll(
        spec,
        PageRequest.of(0, 10, Sorts.parse(param.getSort(), Order.class))
);
```

The HQL / SQL builders also accept a `Sort` directly:

```java
HqlWrapper.create(Order.class)
        .orderBy(Sorts.parse("createTime,desc", Order.class));

SqlWrapper.create("t_order", "o")
        .orderBy(Sorts.parse("create_time,desc", Set.of("create_time")));
```

Do not pass `Sort.by(request.getParameter("sort"))` to a query directly: `Sort` performs no field validation and cannot restrict external input.
