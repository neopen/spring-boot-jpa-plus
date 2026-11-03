# Architecture

## Design principles

1. Spring Data JPA remains the persistence foundation; this project is an adapter/helper layer.
2. Core DAO APIs depend on `EntityManager` and Spring Data types, not on a specific database.
3. Transaction ownership stays with the application service.
4. Auto-configuration registers defaults but backs off when the application provides its own beans.
5. Business entities, repositories, controllers and database drivers belong to the consuming application or examples, not the library artifact.

## Runtime view

```mermaid
flowchart TB
    App[Application Service]
    Repo[Spring Data Repository]
    HQL[EntityManagerHqlDao]
    SQL[EntityManagerSqlDao]
    Spec[Specification / Criteria Helpers]
    EM[EntityManager]
    Provider[Hibernate / JPA Provider]
    DB[(Relational Database)]

    App --> Repo
    App --> HQL
    App --> SQL
    App --> Spec
    HQL --> EM
    SQL --> EM
    Repo --> EM
    Spec --> Repo
    EM --> Provider
    Provider --> DB
```

## Auto-configuration lifecycle

```mermaid
flowchart TD
    A[Spring Boot application starts] --> B{Spring Data JPA on classpath?}
    B -- No --> C[Auto-configuration backs off]
    B -- Yes --> D[HibernateJpaAutoConfiguration]
    D --> E[JpaPlusAutoConfiguration]
    E --> F{hqlDao already defined?}
    F -- No --> G[Register EntityManagerHqlDao]
    F -- Yes --> H[Keep application bean]
    G --> I{sqlDao already defined?}
    H --> I
    I -- No --> J[Register EntityManagerSqlDao]
    I -- Yes --> K[Keep application bean]
```

## Package boundaries

| Package | Responsibility | Notes |
|---|---|---|
| `autoconfigure` | Spring Boot bean registration | Must remain conditional and override-friendly |
| `persistence.dao` | HQL and Native SQL query helpers | No business entities |
| `convert` | Criteria predicates, page and VO conversion | Optional convenience layer above Spring Data |
| `bean` | Generic result/page abstractions | Avoid coupling to Web or Swagger |
| `annotation`, `aspect`, `utils` | Cross-cutting helpers | Candidate for a separate optional web extension |
| `examples/demo` | Demonstration business code | Excluded from the published library artifact |

## Transaction boundary

Read and write methods use the transaction context provided by the caller. A write operation such as batch `persist` should normally be called from a service method annotated with `@Transactional`. The library must not catch and suppress persistence exceptions because doing so can leave a transaction marked rollback-only while returning an apparent success.

## Query and pagination behavior

- Positional parameters use JPA's 1-based parameter index.
- Named parameters use `:name` in HQL/SQL and the same key in the parameter map (without the colon).
- `findPage` applies offset and max results; it does not infer or execute a count query.
- Native SQL result mapping remains provider/database dependent.
- Dynamic sort fields must be selected from a whitelist; they cannot be safely bound as ordinary query parameters.
