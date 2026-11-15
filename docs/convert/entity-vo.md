# Entity → VO 转换

`convert` 模块的转换是**显式映射**：由业务实现转换函数，框架负责批量与分组遍历。它不依赖反射，因此字段改名、加解密、枚举翻译都可以在同一个函数里完成。

## 服务接口

`BaseVoService<T, V>`：`T` 为实体类型，`V` 为 VO 类型。

| 方法 | 说明 |
| --- | --- |
| `<S extends V> BaseVoCommom<T, S> baseVo(T entity)` | 返回转换函数，默认实现返回 `(t) -> null`，**通常需要重写** |
| `<S extends T> V parseVo(S entity)` | 单条转换，内部调用 `baseVo(entity).setCommonVo(entity)` |
| `<S extends T> V parseVo(S entity, Map<Object, ?> map)` | 携带映射形式扩展参数，默认忽略 |
| `<S extends T> V parseVo(S entity, Object... params)` | 携带可变扩展参数，默认忽略 |

`BaseVoCommom<T, V>` 是函数式接口，唯一方法为 `V setCommonVo(T entity)`。

## 实现

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

由于 `baseVo` 的返回类型是 `BaseVoCommom<T, S>`，而实现中构造的是具体 VO，需要一次未检查转换，由调用方保证泛型一致。

使用：

```java
OrderVo vo = service.parseVo(order);

// 携带扩展参数，例如当前登录用户、字典缓存
OrderVo withExt = service.parseVo(order, Map.of("lang", "zh-CN"));
```

## 基础服务

`AbstractBaseService<V, T, P>` 同时实现 `BaseVoService<T, V>` 与 `BasePredicateService<T, P>`，泛型顺序为 `<VO, Entity, Param>`：

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

### 批量与分组

| 方法 | 说明 |
| --- | --- |
| `protected final List<V> batchParseVo(Collection<? extends T> entities)` | 批量转换，`null` 或空集合返回空列表 |
| `protected final List<V> batchParseVo(Collection<? extends T> entities, Object... params)` | 批量转换并透传扩展参数 |
| `protected final Map<Object, List<V>> batchParseMapsVo(Map<Object, ? extends List<T>> groups, boolean sort)` | 保留 Key 的分组转换，空分组不写入结果；`sort=true` 时按 Key 自然排序 |
| `protected final Map<Object, List<V>> batchParseMapsVo(Map<Object, ? extends List<T>> groups, Comparator<Object> comparator)` | 按自定义比较器排序，`comparator` 为 `null` 时保留输入顺序 |

### 查询条件扩展点

`BasePredicateService<T, P>` 提供 `getPredicates(...)` 与 `buildPredicates(...)`，默认返回空列表。业务可重写 `buildPredicates` 把参数翻译为 `Predicate`：

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

与 `SpecWrapper` 组合：

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .raw((root, query, cb) -> cb.and(
                service.getPredicates(root, query, cb, param)
                        .toArray(new Predicate[0])))
        .toSpecification();
```

条件简单时优先用 [注解式查询条件](/core/conditions)，避免手写 Criteria。

## VO 类型约束

视图对象不要求继承任何基类，任意 POJO 都可以作为 `V`。主键、序列化接口等按业务需要自行声明：

```java
@Data
public class OrderVo implements Serializable {

    private Long id;
    private String orderNo;
}
```

`BaseVoService` 与 `BaseVoCommom` 的 `V` 均为无约束类型参数，因此 DTO、`record` 或第三方返回模型都可以直接复用同一套转换约定。
