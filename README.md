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

## Example Project

Explore the demo project for practical integration and usage examples.

**[spring-boot-jpa-plus-demo](https://github.com/neopen/spring-boot-jpa-plus/tree/main/examples/spring-boot-jpa-plus-demo)**

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