# Spring Boot JPA Plus

<p align="center">  <strong>A Powerful JPA Extension Toolkit for Spring Boot</strong>  <br/>  Simplify data access, eliminate boilerplate code, and boost development productivity.</p>

<p align="center">  <a href="https://github.com/neopen/spring-boot-jpa-plus">    <img src="https://img.shields.io/github/stars/neopen/spring-boot-jpa-plus?style=flat-square" alt="GitHub Stars"/>  </a>  <a href="https://github.com/neopen/spring-boot-jpa-plus/network/members">    <img src="https://img.shields.io/github/forks/neopen/spring-boot-jpa-plus?style=flat-square" alt="GitHub Forks"/>  </a>  <a href="https://github.com/neopen/spring-boot-jpa-plus/issues">    <img src="https://img.shields.io/github/issues/neopen/spring-boot-jpa-plus?style=flat-square" alt="GitHub Issues"/>  </a>  <a href="https://github.com/neopen/spring-boot-jpa-plus/blob/main/LICENSE">    <img src="https://img.shields.io/github/license/neopen/spring-boot-jpa-plus?style=flat-square" alt="License"/>  </a></p>

## Overview

**Spring Boot JPA Plus** is a modular enhancement toolkit built on top of Spring Data JPA.

It provides convenient abstractions for common database operations, dynamic queries, batch processing, Entity-VO conversion, and pagination.

Instead of replacing JPA, it extends the existing development experience with reusable components and flexible APIs.

### Features

- **CRUD Operations** - Simplified common database operations.
- **Batch Processing** - Convenient batch save, update, and delete operations.
- **Dynamic Queries** - Flexible query conditions powered by JPA Criteria API.
- **Entity-VO Conversion** - Reusable object conversion and mapping.
- **Pagination** - Simplified paginated queries and response handling.
- **HQL & SQL** - Extended data access capabilities.
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

- [HQL Usage Guide](docs/hql.md) - Chained conditions, pagination, counting, batch queries, and custom operators.

## Example Project

The [spring-boot-jpa-plus-demo](examples/spring-boot-jpa-plus-demo) module is a runnable sample covering:

- **Order aggregate** - JPA CRUD, order item batch loading (N+1 avoidance), and the same queries implemented with HQL.
- **Product inventory** - JPA `Specification` queries versus HQL chained queries, native HQL field comparison, bulk restock, and native SQL aggregation.
- **Same interface, two implementations** - `OrderService` and `ProductService` each have a JPA implementation and an HQL implementation. Default endpoints use JPA; `/hql` endpoints use HQL.
- **HTTP API** - REST endpoints under `/order` and `/product`, backed by an H2 in-memory database.

Key endpoints:

| Method | Path | Implementation |
| --- | --- | --- |
| POST | `/order` | JPA |
| GET | `/order/page`, `/order/list`, `/order/{orderNo}` | JPA |
| GET | `/order/hql/page`, `/order/hql/list`, `/order/hql/{orderNo}` | HQL |
| POST | `/product` | JPA |
| GET | `/product/page`, `/product/list`, `/product/{id}` | JPA |
| GET | `/product/low-stock`, `/product/stock-summary` | JPA |
| POST | `/product/restock` | JPA entity update |
| GET | `/product/hql/page`, `/product/hql/list`, `/product/hql/{id}` | HQL |
| GET | `/product/hql/low-stock`, `/product/hql/stock-summary` | HQL + native SQL |
| POST | `/product/hql/restock` | HQL bulk update |

Tests are split into three routes by JUnit `@Tag`:

```bash
cd examples/spring-boot-jpa-plus-demo

mvn test                       # all routes
mvn test -Punit-test           # plain unit tests, no Spring context
mvn test -Pintegration-test    # Spring + H2 service tests for both implementations
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

Spring Boot JPA Plus is licensed under the [MIT License](https://chatgpt.com/c/LICENSE).

<p align="center">  If you find this project useful, please consider giving it a ⭐</p>