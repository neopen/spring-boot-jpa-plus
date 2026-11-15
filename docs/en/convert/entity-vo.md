# Entity → VO conversion

Conversion in the `convert` module is **explicit mapping**: the business implements the conversion function, and the framework handles batch and grouped traversal. It does not rely on reflection, so field renaming, encryption/decryption, and enum translation can all be done inside the same function.

## Service interface

`BaseVoService<T, V>`: `T` is the entity type, `V` is the VO type.

| Method | Description |
| --- | --- |
| `<S extends V> BaseVoCommom<T, S> baseVo(T entity)` | Returns the conversion function; the default implementation returns `(t) -> null`, **usually needs to be overridden** |
| `<S extends T> V parseVo(S entity)` | Converts a single record, internally calls `baseVo(entity).setCommonVo(entity)` |
| `<S extends T> V parseVo(S entity, Map<Object, ?> map)` | Carries map-style extended parameters, ignored by default |
| `<S extends T> V parseVo(S entity, Object... params)` | Carries variable extended parameters, ignored by default |

`BaseVoCommom<T, V>` is a functional interface whose only method is `V setCommonVo(T entity)`.

## Implementation

```java
@Service
public class OrderVoService implements BaseVoService<Order, OrderVo> {

    @Override
    @SuppressWarnings("unchecked")
    public <S extends OrderVo> BaseVoCommom<Order, S> baseVo(Order entity) {
        return t -> {
            if (t == null) {
                return null;
            }

            OrderVo vo = new OrderVo();
            vo.setId(t.getId());
            vo.setOrderNo(t.getOrderNo());
            vo.setTotalAmount(t.getTotalAmount());
            return (S) vo;
        };
    }
}
```

Because the return type of `baseVo` is `BaseVoCommom<T, S>` while the implementation constructs a concrete VO, an unchecked cast is required, and the caller must guarantee generic consistency.

Usage:

```java
OrderVo vo = service.parseVo(order);

// 携带扩展参数，例如当前登录用户、字典缓存
OrderVo withExt = service.parseVo(order, Map.of("lang", "zh-CN"));
```

## Base service

`AbstractBaseService<V, T, P>` implements both `BaseVoService<T, V>` and `BasePredicateService<T, P>`, with the generic order `<VO, Entity, Param>`:

```java
@Service
public class OrderServiceImpl
        extends AbstractBaseService<OrderVo, Order, OrderQueryParam>
        implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends OrderVo> BaseVoCommom<Order, S> baseVo(Order entity) {
        return t -> (S) toVo(t);
    }

    private OrderVo toVo(Order order) {
        OrderVo vo = new OrderVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        return vo;
    }
}
```

### Batch and grouping

| Method | Description |
| --- | --- |
| `protected final List<V> batchParseVo(Collection<? extends T> entities)` | Batch conversion; returns an empty list for `null` or empty collections |
| `protected final List<V> batchParseVo(Collection<? extends T> entities, Object... params)` | Batch conversion and passes through extended parameters |
| `protected final Map<Object, List<V>> batchParseMapsVo(Map<Object, ? extends List<T>> groups, boolean sort)` | Grouped conversion preserving keys; empty groups are not written to the result; when `sort=true`, keys are sorted naturally |
| `protected final Map<Object, List<V>> batchParseMapsVo(Map<Object, ? extends List<T>> groups, Comparator<Object> comparator)` | Sorts by a custom comparator; when `comparator` is `null`, the input order is preserved |

### Query predicate extension point

`BasePredicateService<T, P>` provides `getPredicates(...)` and `buildPredicates(...)`, which return an empty list by default. The business can override `buildPredicates` to translate parameters into `Predicate`:

```java
@Override
public List<Predicate> buildPredicates(Root<Order> root,
                                       CriteriaQuery<?> query,
                                       CriteriaBuilder cb,
                                       OrderQueryParam param) {

    List<Predicate> predicates = new ArrayList<>();
    if (param != null && param.getStatus() != null) {
        predicates.add(cb.equal(root.get("status"), param.getStatus()));
    }
    return predicates;
}
```

Combined with `SpecWrapper`:

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .raw((root, query, cb) -> cb.and(
                service.getPredicates(root, query, cb, param)
                        .toArray(new Predicate[0])))
        .toSpecification();
```

When conditions are simple, prefer [annotation-based query conditions](/en/core/conditions) to avoid writing Criteria by hand.

## VO type constraints

View objects are not required to extend any base class; any POJO can be used as `V`. Primary keys, serialization interfaces, and so on can be declared as the business requires:

```java
@Data
public class OrderVo implements Serializable {

    private Long id;
    private String orderNo;
}
```

`V` in `BaseVoService` and `BaseVoCommom` is an unconstrained type parameter, so DTOs, `record`s, or third-party return models can all reuse the same conversion conventions directly.
