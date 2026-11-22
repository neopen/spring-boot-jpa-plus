 <h1 align="center">Spring Boot JPA Plus</h1>

<p align="center">  <strong>A Powerful JPA Extension Toolkit for Spring Boot</strong>  <br/>  Simplify data access, eliminate boilerplate code, and boost development productivity.</p>

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
    <a href="https://github.com/neopen/spring-boot-jpa-plus/blob/main/README.zh-CN.md" target="_blank">简体中文</a> •
    <a href="https://jpa.helpenx.com/" target="_blank"><b>Website</b></a> •
    <a href="https://jpa.helpenx.com/guide/getting-started.html" target="_blank">Quick Start</a> •
    <a href="https://jpa.helpenx.com/version.html" target="_blank">Releases</a> •
    <a href="https://github.com/neopen/spring-boot-jpa-plus/issues" target="_blank">Issues</a> 
</p>



## Overview

**Spring Boot JPA Plus** is a modular enhancement toolkit built on top of Spring Data JPA.

It provides convenient abstractions for common database operations, dynamic queries, batch processing, Entity-VO conversion, and pagination.

Instead of replacing JPA, it extends the existing development experience with reusable components and flexible APIs.

### Features

- **CRUD Operations** - Simplified common database operations.
- **Batch Processing** - Convenient batch save, update, and delete operations, upsert by business key, optional Hibernate JDBC batching, and batch-wise stream consumption with `forEachBatch`.
- **Dynamic Queries** - Flexible query conditions powered by JPA Criteria API.
- **Query Utilities** - Annotation-driven conditions, safe sorting, logical deletion, a specification executor, and DTO projection.
- **Auditing** - `@CreatedAt` / `@UpdatedAt` / `@CreatedBy` / `@UpdatedBy` fields maintained automatically, with an `AuditorProvider` SPI for the current user.
- **Change History** - `@Audited` captures field-level before/after snapshots and publishes create, update, and delete records through `AuditLogPublisher`.
- **Multi-Tenancy** - `@TenantId` fills the tenant column on write and filters automatically on query, with fixed tenants per scope.
- **Row-Level Data Permission** - `DataPermissionProvider` and `DataRule` append `field IN (...)` conditions to queries, with `OR` rules and association paths.
- **Filter Skipping** - `@IgnoreTenant` / `@IgnoreDataScope` skip tenant and permission filters per method, with nested restoration.
- **Dynamic Data Sources** - `@DataSource` and `DataSourceContext` support read/write splitting and per-business database switching.
- **Enum Conversion** - Persist enums as explicit database values instead of constant names.
- **Field Encryption** - Transparent AES-GCM encryption for annotated string fields, with key rotation and a deterministic mode for equality search.
- **Multi-datasource Scopes** - Isolate logic-delete, auditing, encryption, and batch switches by a thread-bound scope.
- **Tree & IDs** - Build trees from flat lists and generate snowflake identifiers.
- **Data Desensitization** - Mask names, phones, ID cards, bank cards, emails, and addresses for fastjson2 and Jackson.
- **Reliability Aspects** - Optimistic-lock retry, slow-method logging, and slow SQL logging.
- **Stream Queries** - Stream large result sets from the Specification, HQL, and native SQL executors.
- **Metrics** - Optional Micrometer counters for data source routes and slow SQL, plus an asynchronous audit publisher.
- **Context Propagation** - `JpaPlusContext` and a `TaskDecorator` carry the scope and filter switches into async tasks and restore them afterwards.
- **Entity-VO Conversion** - Reusable object conversion and mapping.
- **Pagination** - Simplified paginated queries, cursor (keyset) pagination, validated request paging parameters, and response handling.
- **HQL & SQL** - Chained query builders with grouping, parameterized subqueries, union, pagination with automatic count, batch updates, and native SQL access.
- **Auto Configuration** - Seamless Spring Boot integration.
- **Modular Architecture** - Independent modules and optional extensions.

## Architecture

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

| Module          | Description                                    |
| --------------- | ---------------------------------------------- |
| `core`          | Core JPA data access and CRUD operations       |
| `convert`       | Entity-VO conversion and dynamic query support |
| `autoconfigure` | Spring Boot auto-configuration                 |
| `starter`       | Unified dependency entry point                 |
| `extension`     | Optional extension components                  |
| `demo`          | Sample application and usage examples          |

## Requirements

- Java 17+
- Spring Boot 3.x
- Maven 3.x

> Compatibility depends on the published release. Check the project dependencies for the exact supported versions.

## Installation

Add the starter dependency to your `pom.xml`:

```
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>${version}</version>
</dependency>
```

Replace `${version}` with the desired release version.

> **Version notice**: this documentation describes version **0.8**.
> The current Maven release is **0.4.x**, which does not include the 0.5–0.8 features
> (enum conversion, field encryption, tree utilities, snowflake IDs, desensitization,
> optimistic-lock retry, slow logging, query builder enhancements, multi-tenancy,
> data permission, change history, dynamic data sources, logical-delete restoration,
> batch upsert, filter skipping, stream queries, JDBC batching, metrics,
> cursor pagination, batch streaming and context propagation).
> See the [release page](https://jpa.helpenx.com/version) for details.

## Quick Start

### 1. Define an Entity

```
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNo;

    private String status;

    private BigDecimal totalAmount;

    // Getters and setters
}
```

### 2. Define a Repository

```
@Repository
public interface OrderRepository
        extends JpaRepository<Order, Long>,
                JpaSpecificationExecutor<Order> {
}
```

### 3. Build Dynamic Queries

Implement custom query conditions without repeating Criteria API boilerplate:

```
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

### 4. Use the Base Service

Extend the provided base service to reuse common data access and conversion capabilities:

```
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

## Documentation

Full documentation, including usage guides and API reference, is published at
[docs site](https://jpa.helpenx.com/).

- [HQL Usage Guide](https://jpa.helpenx.com/query/hql.html) - Chained conditions, pagination, counting, batch queries, and custom operators.

## JPA Specification Builder

`SpecWrapper` builds Spring Data JPA `Specification` with the same chained style as the HQL and SQL builders. Conditions default to `AND`; use `and` / `or` for nested groups.

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .like("orderNo", "DS")
        .ge("totalAmount", new BigDecimal("100"))
        .join("orderProducts")
        .like("orderProducts.productName", "机械")
        .toSpecification();

Page<Order> page = orderRepo.findAll(spec, PageRequest.of(0, 10));
```

Joins enable `distinct` automatically, so pagination and count queries are not duplicated by one-to-many associations. Fetch joins are skipped for count queries:

```java
SpecWrapper.create(Order.class)
        .fetch("orderProducts")
        .toSpecification();
```

For subqueries or field-to-field comparisons, append a raw specification:

```java
SpecWrapper.create(Product.class)
        .raw((root, query, cb) -> cb.lessThanOrEqualTo(
                root.<Integer>get("stockQuantity"),
                root.<Integer>get("safetyStock")))
        .toSpecification();
```

### Annotation-driven Conditions

Declare query semantics on the parameter object once, then reuse it across all three builders:

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

`null` values, blank strings, empty collections, and empty arrays are skipped automatically. `HqlWrapper` and `SqlWrapper` provide the same `apply(param)` method; the SQL builder converts camelCase field names to snake_case columns unless `value` overrides the name. `IS_NULL` / `IS_NOT_NULL` treat the annotated field as a boolean switch.

Conditions that share a `group` are combined with `OR`, while different groups and ungrouped conditions are joined with `AND`:

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

Nested parameter objects are scanned recursively (up to three levels) as long as the field itself has no `@QueryCondition`; conditions are flattened without a field prefix:

```java
public class OrderQueryParam {

    @QueryCondition
    private String status;

    private KeywordFilter keyword;   // fields inside are scanned recursively
}
```

### Type-safe Attributes

Criteria metamodel attributes can replace string field names when the static metamodel is generated:

```java
SpecWrapper.create(Order.class)
        .eq(Order_.status, "PAID")
        .isNotNull(Order_.paymentTime)
        .toSpecification();
```

### Safe Sorting

`Sorts` validates sort expressions against entity fields or an explicit allow-list before building a `Sort`, so external input cannot reference arbitrary properties:

```java
Sort sort = Sorts.parse("createTime,desc;id,asc", Order.class);
Sort allowed = Sorts.parse("id,desc", Set.of("id", "totalAmount"));
```

Unknown fields and invalid directions throw `IllegalArgumentException`; blank expressions return `Sort.unsorted()`.

Nested property paths are validated segment by segment (up to three levels), and alias maps translate external field names to entity properties:

```java
Sort sort = Sorts.parse("customer.name,asc", Order.class);
Sort aliased = Sorts.parse(
        "name,desc",
        Order.class,
        Map.of("name", "customer.name")
);
```

Alias targets are validated as well; an unknown alias target is rejected.

### Logical Deletion

Annotate a boolean field with `@LogicDelete` and the builder appends `field = false` automatically. The annotation is inherited from parent classes and cached per entity type:

```java
public class Order {

    @LogicDelete
    private Boolean deleted = Boolean.FALSE;
}

Specification<Order> spec = SpecWrapper.create(Order.class)
        .filterDeleted()
        .like("orderNo", "DS")
        .toSpecification();

// or combine with an existing specification
Specification<Order> filtered = LogicDeletes.filter(Order.class, spec);
```

Only JPA Criteria queries are filtered. Native SQL statements must add the condition manually. The write side is available as well:

```java
// flag rows instead of removing them
int affected = LogicDeletes.deleteById(entityManager, Order.class, 1L);
int marked = LogicDeletes.deleteAll(entityManager, Order.class, orders);
```

`deleteById` and `deleteAllById` execute a JPQL update and therefore skip entity lifecycle callbacks; `deleteAll` flips the field on managed entities. Entities without `@LogicDelete` are rejected with `IllegalArgumentException`.

Filtering can be disabled globally through `LogicDeletes.setEnabled(false)` or `neopen.jpa.logic-delete.enabled=false`, which is useful for maintenance jobs that must see deleted rows. Write-side deletion is not affected by the switch.

### Specification Executor

`JpaPlusSpecExecutor` centralizes paging, counting, slicing, and fetch pagination for `Specification` queries:

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

Count queries use `countDistinct` on the configured identifier to avoid duplicate rows from joins. `findSlice` fetches `pageSize + 1` rows instead of counting, and logical deletion filtering is enabled by default (`executor.logicDelete(false)` disables it). When fetch attributes are provided, the executor delegates to `FetchJoinPaging`.

`findById` applies the same logical deletion filter, and `deleteById` / `deleteAllById` delegate to `LogicDeletes` when the entity is annotated:

```java
Order order = executor.findById(1L);
executor.deleteById(1L);
```

### Fetch Join Pagination

Fetching a collection together with pagination normally falls back to in-memory paging. `FetchJoinPaging` keeps pagination on the database side by querying IDs first and then loading the page with fetch joins:

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

## SQL Query Builder

`SqlWrapper` mirrors the HQL builder for native SQL: it generates named parameters, a count query, and optional ordering.

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

Pagination is available for both projections and entities, with automatic or explicit count SQL:

```java
Page<Product> page = sqlExecutor.queryForPage(
        SqlWrapper.create("t_product").eq("product_type", "输入设备"),
        Product.class,
        PageRequest.of(0, 10, Sort.by("id").descending())
);
```

## DTO Projection

`JpaPlusSqlExecutor` and `JpaPlusHqlExecutor` map native SQL or HQL results to DTOs. Column aliases are matched to DTO properties after ignoring case and underscores, so `order_no`, `orderNo`, and `ORDER_NO` all target `orderNo`. The DTO only needs a no-arg constructor.

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

Values are converted to the target field type for numbers, strings, booleans, and enums, and unknown columns are ignored. `queryForDtoObject` returns the first row or `null`, while `queryForDtoPage` derives the count statement automatically (pass an explicit count query for `group by` or `distinct`).

## Batch Operations

`BatchOperation` flushes and clears the persistence context in batches:

```java
BatchOperation.persistAll(entityManager, orders, 500);
BatchOperation.removeAll(entityManager, orders, 500);

BatchOperation.forEachBatch(ids, 500, batch ->
        repository.deleteAllByIdInBatch(batch));
```

Null entity managers, non-positive batch sizes, and null callbacks are rejected before any database work starts. The default used by the overloads without an explicit batch size can be changed with `BatchOperation.setDefaultBatchSize(200)` or `neopen.jpa.batch-size`.

## Auditing

Add `@EntityListeners(AuditListener.class)` to the entity and annotate the audit fields:

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

Time fields support `Instant`, `LocalDateTime`, `LocalDate`, `OffsetDateTime`, `ZonedDateTime`, `java.util.Date`, and `long` epoch milliseconds. Configure the auditor and clock once at startup:

```java
Audits.setAuditorSupplier(() -> currentUserId());
Audits.setClock(Clock.systemDefaultZone());
```

`@CreatedAt` and `@CreatedBy` are written on insert only, while `@UpdatedAt` and `@UpdatedBy` are refreshed on insert and update. Auditor fields are left untouched when the supplier returns `null`, and unsupported field types fail fast at the first scan.

The starter can configure the same values: `neopen.jpa.audit.enabled` toggles filling, `neopen.jpa.audit.zone` selects the clock zone (default `UTC`), and `neopen.jpa.audit.auditor` provides a fixed operator name. Programmatic settings remain in effect when a property is not configured.

## Enum Conversion

`@EnumValue` declares the database value of an enum constant. A converter subclass resolves the enum type from its generic parameter:

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

Constants without the annotation fall back to `name()`. `EnumValues.fromValue` matches declared values first, then names ignoring case. Duplicate values, conflicting declarations, and unknown values fail with `JpaPlusException`.

## Field Encryption

Annotate string fields and let `EncryptedFieldConverter` encrypt with AES/GCM/NoPadding:

```java
@EncryptedField
@Convert(converter = EncryptedFieldConverter.class)
private String idCard;
```

Configure the key once:

```java
EncryptedFields.setKey("change-me-in-production");   // SHA-256 derived
```

Or through properties: `neopen.jpa.encryption.key` and `neopen.jpa.encryption.enabled`. Each encryption uses a random 12-byte IV and stores `Base64(IV + ciphertext + tag)`; tampered data fails authentication. `null` and empty strings pass through, and a missing key raises `JpaPlusException` instead of silently writing plaintext.

## Tree Building

`TreeUtils.build` converts a flat list into a tree while preserving input order:

```java
List<Category> tree = TreeUtils.build(
        categories,
        Category::getId,
        Category::getParentId,
        Category::setChildren
);
```

Missing parents become roots. Null or duplicated IDs, self-references, and cycles are rejected with `IllegalArgumentException`. Existing children collections are reset before attaching.

## Snowflake IDs

```java
SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
long id = generator.nextId();
```

IDs use 41 timestamp bits, 5 data-center bits, 5 worker bits, and 12 sequence bits, with a default epoch of `2020-01-01T00:00:00Z`. `nextId()` is synchronized, waits for the next millisecond when the sequence overflows, and throws `IllegalStateException` when the clock moves backwards.

## Parameter Mapping

`ParamMapper` copies request parameters onto entities. Only non-null parameter fields are written, so one mapper serves both creates and partial updates. Names are matched after ignoring case and underscores, and setters take precedence over direct field access.

```java
Order order = ParamMapper.toEntity(orderParam, Order.class);
ParamMapper.apply(orderParam, existingOrder);   // null parameter fields are skipped
List<Order> orders = ParamMapper.toEntities(params, Order::new);
```

Use `@ParamField("orderNo")` to rename a parameter field and `@ParamIgnore` to exclude one:

```java
public class OrderParam {

    @ParamField("orderNo")
    private String number;

    @ParamIgnore
    private String checkToken;
}
```

Numbers, strings, booleans, chars, and enums are converted automatically, and a missing no-arg constructor fails fast with `JpaPlusException`.

## Idempotency

`@Idempotent` protects methods from duplicate submissions. Two modes are available:

```java
// business key mode: SpEL key, de-duplicated for ttl seconds
@Idempotent(required = true, key = "#orderNo", ttl = 30)
public void submit(String orderNo) { }

// one-time token mode: reads checkToken from the method arguments
@Idempotent(required = true)
public void confirm(SubmitRequest request) { }
```

Business keys are scoped per method and cached locally; duplicates raise `InvalidObjectException`. Token mode requires an HTTP request context and is skipped when the Servlet API is absent. The aspect is registered by the extension auto-configuration and can be disabled with `neopen.extension.idempotent.enabled=false`.

## Rate Limiting

`@RateLimit` limits a method with a local Guava `RateLimiter`:

```java
@RateLimit(permits = 10, interval = 1, unit = TimeUnit.SECONDS)
public void submit() { }

@RateLimit(key = "#userId", permits = 1, interval = 1)
public void send(String userId) { }
```

The `key` expression uses SpEL and falls back to `Class#method` when empty. Rejected calls throw `RateLimitException` with the configured message. Rate limiters are local to a single JVM, so multi-instance deployments should use a distributed limiter. Disable the aspect with `neopen.extension.rate-limit.enabled=false`.

## Data Desensitization

`@Desensitize` masks sensitive string fields either in place or during JSON serialization:

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

Built-in types cover `NAME`, `PHONE`, `ID_CARD`, `BANK_CARD`, `EMAIL`, `ADDRESS`, and `PASSWORD`. Inherited fields are included, and field metadata is cached per type.

## Optimistic Lock Retry

```java
@RetryOnOptimisticLock(maxAttempts = 3, delay = 100)
@Transactional
public void updateStock(Long id, int quantity) {
    // ...
}
```

The aspect runs with the highest precedence so every retry starts a new transaction. Only `OptimisticLockingFailureException` and `jakarta.persistence.OptimisticLockException` (including nested causes) are retried; other exceptions propagate immediately. Disable it with `neopen.extension.retry.enabled=false`.

## Slow Method Logging

```java
@SlowLog(threshold = 200)
public List<Order> loadOrders(Long userId) {
    // ...
}
```

Methods exceeding the threshold (default 500 ms) log a warning under the `SlowLogAspect` logger. Failures are still timed and rethrown unchanged. Disable it with `neopen.extension.slow-log.enabled=false`.

## Query Builder Extras

Both query builders can map parameter fields and append developer-authored fragments:

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

`raw`, `exists`, and `notExists` splice static fragments without parameter binding, so never pass user input to them. `JpaPlusSpecExecutor` also provides `exists(spec)` for existence checks.

## Example Project

The [spring-boot-jpa-plus-demo](examples/spring-boot-jpa-plus-demo) module is a runnable sample covering:

- **Order aggregate** - JPA CRUD, order item batch loading (N+1 avoidance), and the same queries implemented with HQL and native SQL.
- **JPA joins and filters** - `OrderJpaServiceImpl.buildPredicates` uses `SpecWrapper` to join `orderProducts` for item name and quantity filters, applies automatic `distinct` for correct pagination counts, and supports sender phone and order amount ranges.
- **Product inventory** - JPA `SpecWrapper` queries versus HQL and native SQL, native field comparison, bulk restock, and `SqlWrapper`-based aggregation.
- **Same interface, three implementations** - `OrderService` and `ProductService` each have JPA, HQL, and native SQL implementations. Default endpoints use JPA, `/hql` endpoints use HQL, and `/sql` endpoints use `SqlWrapper` with `JpaPlusSqlExecutor`.
- **HTTP API** - REST endpoints under `/order` and `/product`, backed by an H2 in-memory database.

Key endpoints:

| Method | Path | Implementation |
| --- | --- | --- |
| POST | `/order` | JPA |
| GET | `/order/page`, `/order/list`, `/order/{orderNo}` | JPA |
| GET | `/order/hql/page`, `/order/hql/list`, `/order/hql/{orderNo}` | HQL |
| GET | `/order/sql/page`, `/order/sql/list`, `/order/sql/{orderNo}` | Native SQL |
| POST | `/product` | JPA |
| GET | `/product/page`, `/product/list`, `/product/{id}` | JPA |
| GET | `/product/low-stock`, `/product/stock-summary` | JPA |
| POST | `/product/restock` | JPA entity update |
| GET | `/product/hql/page`, `/product/hql/list`, `/product/hql/{id}` | HQL |
| GET | `/product/hql/low-stock`, `/product/hql/stock-summary` | HQL + native SQL |
| POST | `/product/hql/restock` | HQL bulk update |
| GET | `/product/sql/page`, `/product/sql/list`, `/product/sql/{id}` | Native SQL |
| GET | `/product/sql/low-stock`, `/product/sql/stock-summary` | Native SQL |
| POST | `/product/sql/restock` | Native SQL bulk update |

Tests are split into three routes by JUnit `@Tag`:

```bash
cd examples/spring-boot-jpa-plus-demo

mvn test                       # all routes
mvn test -Punit-test           # plain unit tests, no Spring context
mvn test -Pintegration-test    # Spring + H2 service tests for all implementations
mvn test -Papi-test            # MockMvc HTTP tests
```

## Build from Source

```
git clone https://github.com/neopen/spring-boot-jpa-plus.git

cd spring-boot-jpa-plus

mvn clean install
```

## Contributing

Contributions are welcome!

- Report bugs through [Issues](https://github.com/neopen/spring-boot-jpa-plus/issues).
- Suggest features and improvements.
- Submit Pull Requests.

## License

Spring Boot JPA Plus is licensed under the [MIT License](https://github.com/neopen/spring-boot-jpa-plus/blob/main/LICENSE).

<p align="center">  If you find this project useful, please consider giving it a ⭐</p>