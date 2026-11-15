# Getting Started

::: warning Version notice
The documentation on this site corresponds to **version 0.5** and describes the latest behaviour of the `main` branch.
The current officially released Maven version is **0.4.x** and does not include the capabilities added in 0.5.
When the dependency version does not match the documentation, refer to [Releases](/en/version).
:::

## Requirements

| Item | Requirement |
| --- | --- |
| JDK | 17 or later |
| Spring Boot | 3.x |
| Maven | 3.x |

The versions actually supported are defined by the dependency declarations of the corresponding release.

## Adding the dependency

Only the single entry point is required:

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>${version}</version>
</dependency>
```

The `starter` transitively brings in the `core`, `convert` and `autoconfigure` modules. The extension components (idempotency, rate limiting, unified pagination response) are **not** part of `starter` and have to be declared separately:

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-extension</artifactId>
    <version>${version}</version>
</dependency>
```

## Auto-configuration

`JpaPlusAutoConfiguration` takes effect when the following conditions are met:

- The JPA API and Spring Data JPA are present on the classpath
- An `EntityManagerFactory` exists
- `neopen.jpa.enabled` is `true` (default)

Once active, two beans are registered:

| Bean | Condition |
| --- | --- |
| `JpaPlusHqlExecutor` | `neopen.jpa.hql-enabled=true` (default) |
| `JpaPlusSqlExecutor` | `neopen.jpa.sql-enabled=true` (default) |

They can be injected directly:

```java
@Service
public class OrderQueryService {

    private final JpaPlusHqlExecutor hqlExecutor;

    public OrderQueryService(JpaPlusHqlExecutor hqlExecutor) {
        this.hqlExecutor = hqlExecutor;
    }
}
```

## Defining an entity

```java
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private String status;

    private BigDecimal totalAmount;

    @LogicDelete
    private Boolean deleted = Boolean.FALSE;

    // getter / setter
}
```

## Defining a repository

Keep using the Spring Data JPA interfaces; no replacement is needed:

```java
@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long>,
                JpaSpecificationExecutor<Order> {
}
```

## Your first query

`SpecWrapper` builds a `Specification` through a fluent API; conditions are combined with `AND` by default:

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .filterDeleted()
        .toSpecification();

Page<Order> page = orderRepository.findAll(
        spec,
        PageRequest.of(0, 10, Sorts.parse("id,desc", Order.class))
);
```

For joined queries, add `join`; `distinct` is enabled automatically, so pagination and counting do not produce duplicates because of one-to-many associations:

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .join("orderProducts")
        .like("orderProducts.productName", "机械")
        .toSpecification();
```

## Using the base service

Extend `AbstractBaseService` to reuse the conversion and condition building capabilities; the generic order is `<VO, Entity, Param>`:

```java
@Service
public class OrderServiceImpl
        extends AbstractBaseService<OrderVo, Order, OrderQueryParam>
        implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

The conversion method requires overriding `baseVo`; its purpose is to convert an entity into a VO:

```java
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
        return (S) vo;
    };
}
```

## Minimal configuration

```yaml
neopen:
  jpa:
    enabled: true
    batch-size: 500
    hql-enabled: true
    sql-enabled: true
    logic-delete:
      enabled: true
    audit:
      enabled: true
      zone: Asia/Shanghai
```

For the full list of properties, see [Configuration](/en/reference/configuration).

## Next steps

- [DAO and CRUD](/en/core/dao)
- [Specification builder](/en/core/specification)
- [HQL queries](/en/query/hql)
- [Example project](/en/examples)
