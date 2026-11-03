# Dependency policy

## Required dependencies

| Dependency | Why it is required |
|---|---|
| `spring-boot-starter-data-jpa` | Spring Data JPA, Jakarta Persistence and JPA provider integration |
| `spring-boot-autoconfigure` | Conditional bean registration and Spring Boot auto-configuration |

The consuming application supplies its database driver, datasource and transaction configuration.

## Optional dependencies

- `jakarta.validation-api`: validation annotations on generic page/request models; the application supplies a validation provider if it wants runtime validation.
- `jackson-annotations`: serialization annotations on the generic response model.
- `lombok`: compile-time annotations used by convenience DTOs.

## Deliberately excluded

- Spring Web and Spring AOP: HTTP logging, request idempotency, date validation aspects and rate limiting are not required for JPA access. Their legacy source is retained under `examples/legacy-web-extension` and is not packaged into the core artifact.
- Hutool, Fastjson2 and Guava: used only by those legacy helpers or old business examples; not part of the core runtime.
- Spring Cloud BOM and Spring Cloud Alibaba BOM: unrelated to a standalone JPA library and create unnecessary version coupling.
- Springfox Swagger 2: API documentation belongs to the consuming application; the old Springfox line is not an appropriate baseline for Spring Boot 3 / Jakarta.
- H2 runtime dependency: an embedded database is useful for tests/examples, not a mandatory runtime dependency of the library.

## Future Maven module split

When the optional web helpers are productized, publish them as a separate artifact rather than adding their dependencies to the JPA starter:

1. `spring-boot-jpa-plus-core`: query helpers and generic Criteria utilities.
2. `spring-boot-jpa-plus-autoconfigure`: conditional bean registration and configuration properties.
3. `spring-boot-jpa-plus-starter`: dependency aggregation only.
4. `spring-boot-jpa-plus-web`: optional idempotency/logging/date-validation/rate-limiting features.

The current repository is a single-module starter for the core JPA API. The web helpers remain examples and are not included in the published jar.
