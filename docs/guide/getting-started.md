# 快速开始

## 环境要求

| 项 | 要求 |
| --- | --- |
| JDK | 17 及以上 |
| Spring Boot | 3.x |
| Maven | 3.x |

实际支持的版本以对应发布版本的依赖声明为准。

## 引入依赖

只需要统一入口：

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>${version}</version>
</dependency>
```

`starter` 会传递引入 `core`、`convert`、`autoconfigure` 三个模块。扩展组件（幂等、限流、统一分页响应）**不在** `starter` 内，需要单独声明：

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-extension</artifactId>
    <version>${version}</version>
</dependency>
```

## 自动配置

满足以下条件时 `JpaPlusAutoConfiguration` 生效：

- classpath 中存在 JPA API 与 Spring Data JPA
- 存在 `EntityManagerFactory`
- `neopen.jpa.enabled` 为 `true`（默认）

生效后会注册两个 Bean：

| Bean | 条件 |
| --- | --- |
| `JpaPlusHqlExecutor` | `neopen.jpa.hql-enabled=true`（默认） |
| `JpaPlusSqlExecutor` | `neopen.jpa.sql-enabled=true`（默认） |

可以直接注入使用：

```java
@Service
public class OrderQueryService {

    private final JpaPlusHqlExecutor hqlExecutor;

    public OrderQueryService(JpaPlusHqlExecutor hqlExecutor) {
        this.hqlExecutor = hqlExecutor;
    }
}
```

## 定义实体

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

## 定义 Repository

继续使用 Spring Data JPA 的接口，不需要替换：

```java
@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long>,
                JpaSpecificationExecutor<Order> {
}
```

## 第一个查询

`SpecWrapper` 用链式调用构造 `Specification`，条件默认按 `AND` 组合：

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

连表查询时追加 `join`，`distinct` 会自动开启，分页与计数不会因一对多关联产生重复：

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .join("orderProducts")
        .like("orderProducts.productName", "机械")
        .toSpecification();
```

## 使用基础服务

继承 `AbstractBaseService` 复用转换与条件构建能力，泛型顺序为 `<VO, Entity, Param>`：

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

转换方法需要重写 `baseVo`，转换函数的作用是把实体转换为 VO：

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

## 最小配置

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

完整配置项见 [配置项](/reference/configuration)。

## 下一步

- [DAO 与 CRUD](/core/dao)
- [Specification 构建器](/core/specification)
- [HQL 查询](/query/hql)
- [示例项目](/examples)
