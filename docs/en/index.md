---
layout: home

hero:
  name: Spring Boot JPA Plus
  text: A JPA enhancement toolkit for Spring Boot
  tagline: It does not replace JPA; it only fills in the repetitive hand-written code around dynamic queries, HQL / native SQL building, entity conversion, batch operations and pagination.
  actions:
    - theme: brand
      text: Getting Started
      link: /en/guide/getting-started
    - theme: alt
      text: Core Capabilities
      link: /en/core/dao
    - theme: alt
      text: GitHub
      link: https://github.com/neopen/spring-boot-jpa-plus

features:
  - title: Three query styles, one condition model
    details: "JPA Criteria, HQL and native SQL share the @QueryCondition annotated conditions, safe sorting and pagination conventions, so the same query parameters can switch between the three implementations."
  - title: Joined pagination without duplicate counting
    details: "SpecWrapper automatically appends distinct when tables are joined; FetchJoinPaging uses a three-stage count → primary key paging → primary key loading query, keeping pagination on the database side."
  - title: Auditing and logical deletion out of the box
    details: "@CreatedAt, @UpdatedAt, @CreatedBy and @UpdatedBy are maintained by entity listeners; @LogicDelete appends the filter condition uniformly and can be switched globally through a configuration property."
  - title: Explicit mapping between entities and parameters
    details: "BaseVoService handles Entity → VO and ParamMapper handles Param → Entity; case and underscores are ignored and only non-null fields are overwritten, which suits partial updates."
  - title: Batch operations and projections
    details: "BatchOperation flushes / clears per batch; ResultMapper matches DTOs by alias, so order_no, orderNo and ORDER_NO all map to orderNo."
  - title: Optional extension components
    details: "Idempotency, rate limiting and the unified pagination response are independent of each other and enabled by configuration property; when they are not included, the core modules stay untouched."
---

::: warning Version notice
The documentation on this site corresponds to **version 0.5**. The current officially released Maven version is **0.4.x**, which does not support the capabilities added in 0.5.
See [Releases](/en/version) for details.
:::

## Design boundaries

Spring Boot JPA Plus is an enhancement layer built on top of Spring Data JPA, not a new ORM:

- Entities and repositories are still managed by Spring Data JPA; `JpaRepository` and `JpaSpecificationExecutor` can still be used.
- Logical deletion filtering only applies to JPA Criteria queries; native SQL has to handle the deletion condition by itself.
- Rate limiting and idempotency are implemented with local memory; multi-instance deployments need to be replaced with a distributed solution.

## Core modules

| Module | Responsibility |
| --- | --- |
| `spring-boot-jpa-plus-core` | Common DAO, dynamic conditions, Specification builder and executor, HQL / native SQL executors, logical deletion, auditing, batch operations, projection |
| `spring-boot-jpa-plus-convert` | Entity → VO conversion service, Param → Entity mapping |
| `spring-boot-jpa-plus-autoconfigure` | `neopen.jpa.*` property binding and executor registration |
| `spring-boot-jpa-plus-starter` | Single dependency entry point |
| `spring-boot-jpa-plus-extension` | Idempotency, rate limiting, operation log annotations, unified pagination response |

## The difference in one snippet

For the same paginated query, express the conditions with `SpecWrapper` and let the executor do the counting and paging:

```java
JpaPlusSpecExecutor<Order> executor =
        JpaPlusSpecExecutor.of(entityManager, Order.class, "id");

Page<Order> page = executor.findPage(
        SpecWrapper.create(Order.class)
                .like("orderNo", "DS")
                .ge("totalAmount", new BigDecimal("100"))
                .join("orderProducts")
                .like("orderProducts.productName", "机械")
                .filterDeleted()
                .toSpecification(),
        PageRequest.of(0, 10, Sorts.parse("createTime,desc", Order.class))
);
```

Duplicate rows produced by joins are handled by `distinct` and `countDistinct`; there is no need to write another count statement on the business side.
