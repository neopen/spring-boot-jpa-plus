 <h1 align="center">Spring Boot JPA Plus</h1>

<p align="center">  <strong>面向 Spring Boot 的 JPA 增强工具包</strong>  <br/>  简化数据访问、消除样板代码、提升开发效率。</p>

<p align="center">
  <a href="https://github.com/neopen/spring-boot-jpa-plus">
    <img src="https://img.shields.io/github/stars/neopen/spring-boot-jpa-plus?style=flat-square" alt="GitHub Stars"/>
  </a>
  <a href="https://github.com/neopen/spring-boot-jpa-plus/network/members">
    <img src="https://img.shields.io/github/forks/neopen/spring-boot-jpa-plus?style=flat-square" alt="GitHub Forks"/>
  </a>
  <a href="https://central.sonatype.com/artifact/io.github.neopen/spring-boot-jpa-plus">
    <img src="https://img.shields.io/maven-central/v/io.github.neopen/spring-boot-jpa-plus?style=flat-square" alt="Maven Central Version"/>
  </a>
  <img src="https://img.shields.io/badge/JDK-17-orange?style=flat-square" alt="JDK 17"/>
  <a href="https://github.com/neopen/spring-boot-jpa-plus/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/neopen/spring-boot-jpa-plus?style=flat-square" alt="License"/>
  </a>
</p>

<p align="center">
    <a href="https://github.com/neopen/spring-boot-jpa-plus/blob/main/README.md" target="_blank">English</a> •
    <a href="https://jpa.helpenx.com/" target="_blank"><b>官网</b></a> •
    <a href="https://jpa.helpenx.com/guide/getting-started.html" target="_blank">快速开始</a> •
    <a href="https://jpa.helpenx.com/version.html" target="_blank">版本发布</a> •
    <a href="https://github.com/neopen/spring-boot-jpa-plus/issues" target="_blank">（问题反馈 & 需求采集）</a> 
</p>

## 项目简介

**Spring Boot JPA Plus** 是基于 Spring Data JPA 构建的模块化增强工具包。

它为常见的数据库操作、动态查询、批量处理、Entity-VO 转换和分页提供了便捷封装。

项目不替代 JPA，而是在保留原有开发方式的基础上，通过可复用组件和灵活的 API 扩展开发体验。

### 核心特性

- **CRUD 操作** - 简化常见数据库操作。
- **批量处理** - 便捷的批量新增、更新与删除，支持按业务键 upsert、Hibernate JDBC 批量与 `forEachBatch` 流式分批消费。
- **动态查询** - 基于 JPA Criteria API 的灵活查询条件。
- **查询工具** - 注解式条件、安全排序、逻辑删除、Specification 执行器与 DTO 投影。
- **审计字段** - 自动维护 `@CreatedAt` / `@UpdatedAt` / `@CreatedBy` / `@UpdatedBy`，并通过 `AuditorProvider` SPI 获取当前操作人。
- **变更历史** - `@Audited` 记录字段级前后快照，通过 `AuditLogPublisher` 输出新增、修改与删除记录。
- **多租户** - `@TenantId` 写入自动填充、查询自动过滤，支持按作用域固定租户。
- **行级数据权限** - 通过 `DataPermissionProvider` 与 `DataRule` 为查询追加 `字段 IN (...)` 条件，支持 `OR` 规则与关联路径。
- **过滤跳过** - `@IgnoreTenant` / `@IgnoreDataScope` 按方法临时跳过租户与权限条件，支持嵌套恢复。
- **动态数据源** - `@DataSource` 注解与 `DataSourceContext` 支持读写分离与按业务切换数据源。
- **枚举转换** - 枚举以显式业务值持久化，而不是常量名。
- **字段加密** - 为标注字段提供透明 AES-GCM 加解密，支持密钥轮换与确定性模式（可等值查询）。
- **多数据源作用域** - 按线程绑定作用域，隔离逻辑删除、审计、加密与批量大小开关。
- **树与 ID** - 把扁平列表构建为树，以及生成雪花 ID。
- **数据脱敏** - 姓名、手机号、身份证、银行卡、邮箱、地址脱敏，支持 fastjson2 与 Jackson。
- **可靠性切面** - 乐观锁重试、慢方法日志与慢 SQL 日志。
- **流式查询** - Specification、HQL 与原生 SQL 执行器支持逐条消费大结果集。
- **指标监控** - 可选 Micrometer 统计数据源路由与慢 SQL，内置异步审计发布者。
- **上下文传播** - `JpaPlusContext` 配合 `TaskDecorator`，把作用域与过滤开关带入异步任务并在结束后恢复。
- **Entity-VO 转换** - 可复用的对象转换与映射。
- **分页** - 简化分页查询、游标（keyset）分页、请求分页参数校验与响应处理。
- **HQL 与 SQL** - 链式查询构建器，支持分组、参数化子查询、union、自动 count 分页、批量更新与原生 SQL。
- **自动配置** - 与 Spring Boot 无缝集成。
- **模块化架构** - 模块独立，扩展能力按需引入。

## 模块结构

```
spring-boot-jpa-plus
│
├── spring-boot-jpa-plus-core
├── spring-boot-jpa-plus-convert
├── spring-boot-jpa-plus-autoconfigure
├── spring-boot-jpa-plus-starter
├── spring-boot-jpa-plus-extension
│
└── examples
    └── spring-boot-jpa-plus-demo
```

| 模块            | 说明                             |
| --------------- | -------------------------------- |
| `core`          | JPA 核心数据访问与 CRUD 操作     |
| `convert`       | Entity-VO 转换与动态查询支持     |
| `autoconfigure` | Spring Boot 自动配置             |
| `starter`       | 统一依赖入口                     |
| `extension`     | 可选的扩展组件                   |
| `demo`          | 示例应用与用法演示               |

## 环境要求

- Java 17+
- Spring Boot 3.x
- Maven 3.x

> 兼容范围取决于已发布的版本，请以项目依赖中的实际版本为准。

## 安装

在 `pom.xml` 中引入 starter 依赖：

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>${version}</version>
</dependency>
```

将 `${version}` 替换为需要使用的发布版本。

> **版本说明**：本文档描述 **0.8** 版本能力。
> 当前 Maven 正式发布版本为 **0.4.x**，不包含 0.5 – 0.8 新增功能
> （枚举转换、字段加密、树工具、雪花 ID、数据脱敏、乐观锁重试、
> 日志增强、查询构建器增强，多租户、数据权限、变更历史、
> 动态数据源、逻辑删除恢复、批量 upsert、过滤跳过、
> 流式查询、JDBC 批量、指标监控、游标分页、流式分批与上下文传播）。
> 详情见[版本发布页](https://jpa.helpenx.com/version)。

## 快速开始

### 1. 定义实体

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

    // Getter / Setter
}
```

### 2. 定义 Repository

```java
@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long>,
                JpaSpecificationExecutor<Order> {
}
```

### 3. 构建动态查询

无需重复编写 Criteria API 样板代码：

```java
@Override
public List<Predicate> buildPredicates(
        Root<Order> root,
        CriteriaQuery<?> query,
        CriteriaBuilder cb,
        OrderQueryParam param) {

    List<Predicate> predicates = new ArrayList<>();

    if (param == null) {
        return predicates;
    }

    if (StrUtil.isNotBlank(param.getOrderNo())) {
        predicates.add(cb.like(
                root.get("orderNo"),
                "%" + param.getOrderNo() + "%"
        ));
    }

    if (StrUtil.isNotBlank(param.getStatus())) {
        predicates.add(cb.equal(
                root.get("status"),
                param.getStatus()
        ));
    }

    return predicates;
}
```

### 4. 使用基础服务

继承提供的基础服务，复用数据访问与对象转换能力：

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

## 文档

完整的使用指南与 API 参考发布于[文档站](https://jpa.helpenx.com/)。

- [HQL 使用指南](https://jpa.helpenx.com/query/hql.html) - 链式条件、分页、计数、批量查询与自定义运算符。

## Specification 构建器

`SpecWrapper` 以与 HQL、SQL 构建器一致的链式风格构造 Spring Data JPA `Specification`。条件默认按 `AND` 组合，可通过 `and` / `or` 构建嵌套分组。

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .join("orderProducts")
        .like("orderProducts.productName", "机械")
        .toSpecification();

Page<Order> page = orderRepo.findAll(spec, PageRequest.of(0, 10));
```

连表会自动开启 `distinct`，避免一对多关联导致分页与计数出现重复行。count 查询会自动跳过 fetch join：

```java
SpecWrapper.create(Order.class)
        .fetch("orderProducts")
        .toSpecification();
```

需要子查询或字段间比较时，追加原生 Specification：

```java
SpecWrapper.create(Product.class)
        .raw((root, query, cb) -> cb.lessThanOrEqualTo(
                root.<Integer>get("stockQuantity"),
                root.<Integer>get("safetyStock")))
        .toSpecification();
```

### 注解式条件

在参数对象上声明一次查询语义，即可在三种构建器中复用：

```java
public class OrderQueryParam {

    @QueryCondition(operator = ConditionOperator.LIKE)
    private String orderNo;

    @QueryCondition(value = "totalAmount",
            operator = ConditionOperator.BETWEEN,
            endField = "maxTotalAmount")
    private BigDecimal minTotalAmount;

    private BigDecimal maxTotalAmount;
}

Specification<Order> spec = SpecWrapper.create(Order.class)
        .apply(param)
        .toSpecification();
```

`null` 值、空白字符串、空集合和空数组会被自动忽略。`HqlWrapper` 与 `SqlWrapper` 提供相同的 `apply(param)`；SQL 构建器默认把驼峰字段名转换为下划线列名，可用 `value` 覆盖。`IS_NULL` / `IS_NOT_NULL` 把标注字段当作布尔开关使用。

相同 `group` 的条件按 `OR` 组合，不同分组与未分组条件之间按 `AND` 组合：

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

嵌套参数对象会被递归扫描（最多三层），前提是该字段本身没有 `@QueryCondition`；内部条件不带字段前缀，直接平铺：

```java
public class OrderQueryParam {

    @QueryCondition
    private String status;

    private KeywordFilter keyword;   // 内部字段会被递归扫描
}
```

### 类型安全属性

生成静态元模型后，可以用元模型属性代替字符串字段名：

```java
SpecWrapper.create(Order.class)
        .eq(Order_.status, "PAID")
        .isNotNull(Order_.paymentTime)
        .toSpecification();
```

### 安全排序

`Sorts` 在构建 `Sort` 前按实体字段或白名单校验排序表达式，避免外部输入引用任意属性：

```java
Sort sort = Sorts.parse("createTime,desc;id,asc", Order.class);
Sort allowed = Sorts.parse("id,desc", Set.of("id", "totalAmount"));
```

未知字段和非法方向会抛 `IllegalArgumentException`；空白表达式返回 `Sort.unsorted()`。

嵌套路径按段校验（最多三层），别名映射可把外部字段名转换为实体属性：

```java
Sort sort = Sorts.parse("customer.name,asc", Order.class);
Sort aliased = Sorts.parse(
        "name,desc",
        Order.class,
        Map.of("name", "customer.name")
);
```

别名目标同样会被校验，指向不存在的属性会被拒绝。

### 请求分页参数

`PageParams` 在 `Sorts` 之上封装页码、每页数量与排序校验，适合直接绑定前端参数：

```java
Pageable pageable = PageParams.builder()
        .page(1)
        .size(20)
        .sort("createTime,desc")
        .entityType(Order.class)
        .build()
        .toPageable();
```

页码从 1 开始，每页上限默认 500；未配置实体类型或白名单时忽略排序，避免未校验字段进入 SQL。

### 逻辑删除

在布尔字段上标注 `@LogicDelete`，构建器会自动追加 `field = false`。注解支持父类继承，并按实体类型缓存：

```java
public class Order {

    @LogicDelete
    private Boolean deleted = Boolean.FALSE;
}

Specification<Order> spec = SpecWrapper.create(Order.class)
        .filterDeleted()
        .like("orderNo", "DS")
        .toSpecification();

// 或与已有条件合并
Specification<Order> filtered = LogicDeletes.filter(Order.class, spec);
```

过滤只作用于 JPA Criteria 查询，原生 SQL 需要自行追加条件。写侧删除同样可用：

```java
// 标记而不是物理删除
int affected = LogicDeletes.deleteById(entityManager, Order.class, 1L);
int marked = LogicDeletes.deleteAll(entityManager, Order.class, orders);
```

`deleteById` 与 `deleteAllById` 执行 JPQL 更新语句，不触发实体生命周期回调；`deleteAll` 直接修改托管实体的字段。实体未标注 `@LogicDelete` 时抛 `IllegalArgumentException`。

可通过 `LogicDeletes.setEnabled(false)` 或 `neopen.jpa.logic-delete.enabled=false` 全局关闭过滤，适用于必须读取已删除数据的维护任务。写侧删除不受该开关影响。

### Specification 执行器

`JpaPlusSpecExecutor` 集中处理 `Specification` 查询的分页、计数、切片与 fetch 分页：

```java
JpaPlusSpecExecutor<Order> executor =
        JpaPlusSpecExecutor.of(entityManager, Order.class, "id");

Page<Order> page = executor.findPage(spec, PageRequest.of(0, 10));
Slice<Order> slice = executor.findSlice(spec, PageRequest.of(0, 10));
Page<Order> fetched = executor.findPage(
        spec,
        PageRequest.of(0, 10),
        List.of("orderProducts")
);
```

计数使用配置的主键属性执行 `countDistinct`，避免连表产生重复行。`findSlice` 多取一条记录判断是否有下一页，不执行 count。逻辑删除过滤默认开启，可用 `executor.logicDelete(false)` 关闭。传入 fetch 属性时委托给 `FetchJoinPaging`。

`findById` 同样应用逻辑删除过滤，`deleteById` / `deleteAllById` 在实体标注注解时委托给 `LogicDeletes`：

```java
Order order = executor.findById(1L);
executor.deleteById(1L);
```

### Fetch Join 分页

集合 fetch join 与分页一起使用时，JPA 默认会退化为内存分页。`FetchJoinPaging` 先查主键、再按主键加载，把分页保留在数据库侧：

```java
Page<Order> page = FetchJoinPaging.findPage(
        entityManager,
        Order.class,
        "id",
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        List.of("orderProducts"),
        PageRequest.of(0, 10, Sort.by("id"))
);
```

## SQL 查询构建器

`SqlWrapper` 与 HQL 构建器风格一致，用于原生 SQL：生成命名参数、count 查询与可选排序。

```java
SqlQuery query = SqlWrapper.create("t_product", "p")
        .select("p.stock_status", "count(*) as total")
        .eq("is_active", true)
        .groupBy("stock_status")
        .orderByAsc("stock_status")
        .build();

List<?> rows = sqlExecutor.queryForList(
        query.getSql(),
        query.getParameters()
);
```

分页同时支持投影与实体，count 语句可自动派生或显式传入：

```java
Page<Product> page = sqlExecutor.queryForPage(
        SqlWrapper.create("t_product").eq("product_type", "输入设备"),
        Product.class,
        PageRequest.of(0, 10, Sort.by("id").descending())
);
```

分组与 `having`：

```java
SqlWrapper wrapper = SqlWrapper.create("t_order")
        .select("status", "count(*) as total")
        .groupBy("status")
        .having("count(*) > 10");
```

`union` / `unionAll` 用于合并结构一致的多段查询，参数统一重编号，分页统计按子查询包装：

```java
SqlWrapper archived = SqlWrapper.create("t_order_archive", "a")
        .select("a.id", "a.order_no as orderNo", "a.status")
        .eq("a.status", "CLOSED");

SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
        .select("o.id", "o.order_no as orderNo", "o.status")
        .eq("status", "PAID")
        .union(archived)
        .orderByDesc("id");
```

子查询自身不能包含 `order by`，排序统一写在外层；不支持嵌套 union。

## DTO 投影

`JpaPlusSqlExecutor` 与 `JpaPlusHqlExecutor` 可以把原生 SQL 或 HQL 结果映射为 DTO。列别名在忽略大小写与下划线后与 DTO 属性匹配，`order_no`、`orderNo`、`ORDER_NO` 都能映射到 `orderNo`。DTO 只需要无参构造器。

```java
List<OrderSummary> list = new JpaPlusSqlExecutor(entityManager).queryForDtoList(
        "select id, order_no as orderNo, total_amount as totalAmount"
                + " from t_order where order_no like :prefix",
        Map.of("prefix", "DS-%"),
        OrderSummary.class
);

Page<OrderSummary> page = new JpaPlusHqlExecutor(entityManager).queryForDtoPage(
        "select o.orderNo as orderNo, o.totalAmount as totalAmount"
                + " from Order o where o.orderNo like :prefix",
        Map.of("prefix", "DS-%"),
        OrderSummary.class,
        PageRequest.of(0, 10)
);
```

数字、字符串、布尔与枚举会按目标字段类型转换，未知列会被忽略。`queryForDtoObject` 返回第一行或 `null`；`queryForDtoPage` 自动派生 count 语句（`group by` 或 `distinct` 场景建议显式传入 count）。

## 批量操作

`BatchOperation` 按批次 flush 并清空持久化上下文：

```java
BatchOperation.persistAll(entityManager, orders, 500);
BatchOperation.removeAll(entityManager, orders, 500);

BatchOperation.forEachBatch(ids, 500, batch ->
        repository.deleteAllByIdInBatch(batch));
```

实体管理器为空、批次大小非正数、回调为空都会在任何数据库操作前直接报错。未显式传入批次大小的重载使用默认值，可通过 `BatchOperation.setDefaultBatchSize(200)` 或 `neopen.jpa.batch-size` 调整。

## 审计

在实体上添加 `@EntityListeners(AuditListener.class)` 并标注审计字段：

```java
@Entity
@EntityListeners(AuditListener.class)
public class Order {

    @CreatedAt
    private Instant createdAt;

    @UpdatedAt
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @UpdatedBy
    private String updatedBy;
}
```

时间字段支持 `Instant`、`LocalDateTime`、`LocalDate`、`OffsetDateTime`、`ZonedDateTime`、`java.util.Date` 以及 `long` 毫秒时间戳。启动时配置一次操作人与时钟：

```java
Audits.setAuditorSupplier(() -> currentUserId());
Audits.setClock(Clock.systemDefaultZone());
```

`@CreatedAt` 与 `@CreatedBy` 只在插入时写入，`@UpdatedAt` 与 `@UpdatedBy` 在插入和更新时刷新。提供者返回 `null` 时保留原值；不支持的字段类型会在首次扫描时直接失败。

Spring Boot 中推荐注册 `AuditorProvider` Bean，优先级高于固定操作人配置：

```java
@Bean
public AuditorProvider auditorProvider() {
    return () -> SecurityContextHolder.currentUsername();
}
```

配置项：`neopen.jpa.audit.enabled` 控制是否填充，`neopen.jpa.audit.zone` 设置时钟时区（默认 `UTC`），`neopen.jpa.audit.auditor` 提供固定的操作人名称。未配置的属性不会覆盖代码中的设置。

## 枚举转换

`@EnumValue` 声明枚举常量对应的数据库值，转换器子类从泛型参数解析枚举类型：

```java
public enum OrderStatus {

    @EnumValue("PAID")
    PAID,

    @EnumValue("CANCELLED")
    CANCELLED;
}

public class OrderStatusConverter extends EnumValueConverter<OrderStatus> {
}

@Convert(converter = OrderStatusConverter.class)
private OrderStatus status;
```

未标注的常量回退到 `name()`。`EnumValues.fromValue` 先匹配声明值，再忽略大小写匹配名称。重复值、冲突声明与未知值都会抛 `JpaPlusException`。

## 字段加密

标注字符串字段，由 `EncryptedFieldConverter` 使用 AES/GCM/NoPadding 加解密：

```java
@EncryptedField
@Convert(converter = EncryptedFieldConverter.class)
private String idCard;
```

启动时配置一次密钥：

```java
EncryptedFields.setKey("change-me-in-production");   // 经 SHA-256 派生
```

也可以通过配置项：`neopen.jpa.encryption.key` 与 `neopen.jpa.encryption.enabled`。每次加密使用 12 字节随机 IV，存储格式为 `Base64(IV + 密文 + 认证标签)`；数据被篡改会导致认证失败。`null` 与空串原样返回，未配置密钥时抛 `JpaPlusException`，避免明文入库。

密钥轮换与确定性加密：

```yaml
neopen:
  jpa:
    encryption:
      keys:
        v1: old-secret
        v2: new-secret
      active-key-id: v2
```

```java
@EncryptedField(mode = EncryptedField.Mode.DETERMINISTIC)
@Convert(converter = DeterministicEncryptedFieldConverter.class)
private String phone;

// 相同明文密文一致，可用密文等值查询
wrapper.eq("phone", EncryptedFields.encryptDeterministic(phone));
```

> 注意：JPQL / Criteria 查询会再次应用字段转换器，传入密文会被二次加密。Spring Data 派生查询请直接传明文，或改用原生 SQL 比较密文列。

## 多数据源作用域

逻辑删除、审计、加密与批量大小的开关默认对整个 JVM 生效。多数据源应用可以按作用域隔离：

```yaml
neopen:
  jpa:
    scopes:
      archive-view:
        logic-delete-enabled: false
        audit-enabled: false
        encryption-key: secondary-secret
```

```java
JpaPlusScope.run("archive-view", () -> {
    // 这里不过滤已删除数据，审计与加密也使用该作用域的配置
    archiveRepository.findAll(spec);
});
```

作用域绑定在线程上，执行结束后自动恢复外层作用域；未配置的项沿用全局设置。

## 树构建

`TreeUtils.build` 把扁平列表转换为树，并保持输入顺序：

```java
List<Category> tree = TreeUtils.build(
        categories,
        Category::getId,
        Category::getParentId,
        Category::setChildren
);
```

找不到父节点的元素作为根节点。空 ID、重复 ID、自引用与环都会抛 `IllegalArgumentException`；挂载前会清空已有的子节点集合。

## 雪花 ID

```java
SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
long id = generator.nextId();
```

ID 由 41 位时间戳、5 位数据中心、5 位机器与 12 位序列组成，默认起始时间为 `2020-01-01T00:00:00Z`。`nextId()` 为同步方法，序列溢出时等待下一毫秒，时钟回拨时抛 `IllegalStateException`。

## 参数映射

`ParamMapper` 把请求参数复制到实体，只写入非空参数字段，因此同一套映射可以同时用于新增与局部更新。字段名在忽略大小写与下划线后匹配，setter 优先于直接字段访问。

```java
Order order = ParamMapper.toEntity(orderParam, Order.class);
ParamMapper.apply(orderParam, existingOrder);   // 参数为 null 的字段会跳过
List<Order> orders = ParamMapper.toEntities(params, Order::new);
```

使用 `@ParamField("orderNo")` 重命名字段，使用 `@ParamIgnore` 排除字段：

```java
public class OrderParam {

    @ParamField("orderNo")
    private String number;

    @ParamIgnore
    private String checkToken;
}
```

数字、字符串、布尔、字符与枚举会自动转换；缺少无参构造器会在首次使用时抛 `JpaPlusException`。

## 幂等控制

`@Idempotent` 防止方法被重复提交，支持两种模式：

```java
// 业务键模式：SpEL 键，ttl 秒内去重
@Idempotent(required = true, key = "#orderNo", ttl = 30)
public void submit(String orderNo) { }

// 一次性令牌模式：从方法参数读取 checkToken
@Idempotent(required = true)
public void confirm(SubmitRequest request) { }
```

业务键按方法隔离并缓存在本地，重复提交抛 `InvalidObjectException`。令牌模式依赖 HTTP 请求上下文，缺少 Servlet API 时自动跳过。切面由扩展自动配置注册，可通过 `neopen.extension.idempotent.enabled=false` 关闭。

## 接口限流

`@RateLimit` 使用本地 Guava `RateLimiter` 限制方法调用：

```java
@RateLimit(permits = 10, interval = 1, unit = TimeUnit.SECONDS)
public void submit() { }

@RateLimit(key = "#userId", permits = 1, interval = 1)
public void send(String userId) { }
```

`key` 表达式使用 SpEL，为空时回退为 `Class#method`。被拒绝的调用抛 `RateLimitException`，消息可配置。限流器只在单个 JVM 内生效，多实例部署请使用分布式限流。可通过 `neopen.extension.rate-limit.enabled=false` 关闭切面。

## 数据脱敏

`@Desensitize` 可以在对象上就地脱敏，也可以在 JSON 序列化时脱敏：

```java
public class UserView {

    @Desensitize(type = DesensitizeType.NAME)
    private String name;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;
}

UserView masked = Desensitizes.apply(userView);
String json = JSON.toJSONString(userView, new DesensitizeValueFilter());
```

内置类型覆盖 `NAME`、`PHONE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`ADDRESS`、`PASSWORD`。父类字段同样生效，字段元数据按类型缓存。引入 Jackson 时自动注册 `DesensitizeModule`，`ObjectMapper` 序列化同样脱敏。

## 乐观锁重试

```java
@RetryOnOptimisticLock(maxAttempts = 3, delay = 100)
@Transactional
public void updateStock(Long id, int quantity) {
    // ...
}
```

切面以最高优先级执行，确保每次重试都开启新事务。只重试 `OptimisticLockingFailureException` 与 `jakarta.persistence.OptimisticLockException`（包括嵌套原因），其他异常立即向外传播。可通过 `neopen.extension.retry.enabled=false` 关闭。

## 慢日志

慢方法日志：

```java
@SlowLog(threshold = 200)
public List<Order> loadOrders(Long userId) {
    // ...
}
```

超过阈值（默认 500ms）的方法会在 `SlowLogAspect` 日志器下输出 WARN。异常仍会记录耗时并按原样抛出。可通过 `neopen.extension.slow-log.enabled=false` 关闭。

慢 SQL 日志基于 Hibernate 内置阈值：

```yaml
neopen:
  jpa:
    slow-sql:
      enabled: true
      threshold-ms: 500
logging:
  level:
    org.hibernate.SQL_SLOW: warn
```

## 查询构建器补充

两个查询构建器都支持参数字段映射与开发者编写的静态片段：

```java
HqlWrapper<Order> hql = HqlWrapper.create(Order.class)
        .alias("keyword", "customer.name")
        .apply(param)
        .exists("select 1 from OrderItem i where i.order.id = o.id");

SqlWrapper sql = SqlWrapper.create("t_order")
        .alias("keyword", "order_no")
        .apply(param)
        .notExists("select 1 from t_payment p where p.order_id = o.id");
```

`raw`、`exists`、`notExists` 会原样拼接静态片段，禁止传入用户输入；需要绑定的查询值请使用 `exists(subQuery, params)` 重载。`JpaPlusSpecExecutor` 另外提供 `exists(spec)` 用于存在性判断。

## 示例项目

[spring-boot-jpa-plus-demo](examples/spring-boot-jpa-plus-demo) 是一个可运行的示例应用，覆盖：

- **订单聚合** - JPA CRUD、订单项批量加载（避免 N+1），以及同一查询的 HQL 与原生 SQL 实现。
- **JPA 连表与过滤** - `OrderJpaServiceImpl.buildPredicates` 使用 `SpecWrapper` 连 `orderProducts`，按明细名称与数量过滤，自动 `distinct` 保证分页计数正确，并支持发件人手机号与订单金额区间。
- **商品库存** - JPA `SpecWrapper` 查询与 HQL、原生 SQL 对照，原生字段比较、批量补货与基于 `SqlWrapper` 的聚合统计。
- **同一接口三种实现** - `OrderService` 与 `ProductService` 各有 JPA、HQL、原生 SQL 三套实现。
- **0.5 新能力** - `/feature` 端点演示 `PageParams`、union、数据源作用域、字段加密与 `AuditorProvider`。

主要端点：

| 方法 | 路径 | 实现 |
| --- | --- | --- |
| POST | `/order` | JPA |
| GET | `/order/page`、`/order/list`、`/order/{orderNo}` | JPA |
| GET | `/order/hql/page`、`/order/hql/list`、`/order/hql/{orderNo}` | HQL |
| GET | `/order/sql/page`、`/order/sql/list`、`/order/sql/{orderNo}` | 原生 SQL |
| POST | `/product` | JPA |
| GET | `/product/page`、`/product/list`、`/product/{id}` | JPA |
| GET | `/product/low-stock`、`/product/stock-summary` | JPA |
| POST | `/product/restock` | JPA 实体更新 |
| GET | `/product/hql/page`、`/product/hql/list`、`/product/hql/{id}` | HQL |
| GET | `/product/hql/low-stock`、`/product/hql/stock-summary` | HQL + 原生 SQL |
| POST | `/product/hql/restock` | HQL 批量更新 |
| GET | `/product/sql/page`、`/product/sql/list`、`/product/sql/{id}` | 原生 SQL |
| GET | `/product/sql/low-stock`、`/product/sql/stock-summary` | 原生 SQL |
| POST | `/product/sql/restock` | 原生 SQL 批量更新 |
| GET | `/feature/page` | `PageParams` 分页 |
| GET | `/feature/union` | union 合并查询 |
| GET | `/feature/scope` | 数据源作用域查询 |
| POST / GET | `/feature/secret` | 加密字段写入与确定性查询 |

测试按 JUnit `@Tag` 分为三条路线：

```bash
cd examples/spring-boot-jpa-plus-demo

mvn test                       # 全部
mvn test -Punit-test           # 纯单元测试，不启动 Spring 上下文
mvn test -Pintegration-test    # Spring + H2 服务测试，覆盖三套实现
mvn test -Papi-test            # MockMvc HTTP 测试
```

> 示例模块不在根 reactor 中，首次运行前需要先在项目根目录执行
> `mvn install -DskipTests` 安装本地快照。

## 源码构建

```
git clone https://github.com/neopen/spring-boot-jpa-plus.git

cd spring-boot-jpa-plus

mvn clean install
```

## 参与贡献

欢迎参与贡献！

- 通过 [Issues](https://github.com/neopen/spring-boot-jpa-plus/issues) 反馈问题。
- 提出新功能与改进建议。
- 提交 Pull Request。

## 许可证

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

本项目基于 **Apache License 2.0** 开源。完整协议文本见 [LICENSE](LICENSE)，第三方组件声明见 [NOTICE](NOTICE)。

版权所有 2026 <你的名字或组织名>。

### 这对你意味着什么

- 你可以将 Spring Boot JPA Plus 用于**商业项目和闭源项目**，无需公开你自己的源代码。
- 你需要保留版权声明和许可证声明。
- 本协议包含贡献者授予的**明确专利许可**。
- Hibernate ORM（LGPL-2.1）作为标准 JPA 依赖被调用，本项目**未修改、未重新分发**其源码，因此其 copyleft 条款不会影响你的应用。详见 [NOTICE](NOTICE)。

### 商标

"Spring Boot JPA Plus" 及项目 Logo 为项目所有者的商标，**不在** Apache License 2.0 的授权范围内。


<p align="center">如果这个项目对你有帮助，欢迎点一个 ⭐</p>
