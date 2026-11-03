# Roadmap

## P0 — release foundation

- [x] Separate old order demo code from the published main source set.
- [x] Add Spring Boot 3 auto-configuration for HQL and Native SQL DAO beans.
- [x] Remove Spring Cloud / Spring Cloud Alibaba BOM coupling.
- [x] Remove Springfox annotations from generic library DTOs.
- [x] Correct JPA positional parameter indexing to 1-based.
- [x] Convert Native SQL count results through `Number`, not a provider-specific `BigInteger` cast.
- [x] Do not swallow batch persistence exceptions or clear the caller's persistence context implicitly.
- [x] Make predicate-builder failures fail visibly instead of returning a null predicate.
- [ ] Run `mvn clean verify` in a Java 17 environment and add database-backed integration tests.
- [ ] 由维护者确认开源许可证，并添加对应 `LICENSE` 文件。
- [ ] Confirm Maven Central coordinates, SCM metadata, signing and release workflow.

## P1 — API quality

- [ ] Replace checked `throws Exception` declarations with focused exceptions.
- [ ] Add Optional-based single-result API and document no-result / non-unique-result behavior.
- [ ] Add a `Page<T>` API that returns content and total count without forcing callers to issue two unrelated calls.
- [ ] Add typed query options for timeout, flush mode and lock mode where appropriate.
- [x] Add `executeUpdate` APIs for JPQL and Native SQL with named/positional parameters.
- [x] Deprecate `insertAll(List<T>)` compatibility entry points; Native SQL DAO legacy contract accepts SQL strings rather than entity values.
- [x] Add configurable batch size; leave persistence-context clearing explicit to the caller.
- [ ] Add database integration tests for PostgreSQL and MySQL.

## P2 — architecture

- [ ] Split core, autoconfigure, starter and web extension into independent Maven modules.
- [ ] Replace the custom generic `Json` response model with Spring Data `Page` / `Slice` as the default API; keep response wrappers as optional adapters.
- [x] Move Web/AOP/HTTP helpers out of the core source set so the published artifact has no servlet/AOP dependencies.
- [ ] Add configuration properties with metadata and documented defaults.
- [ ] Add compatibility CI for supported Spring Boot minor lines.

## P3 — quality and ecosystem

- [ ] Add JaCoCo coverage reporting and mutation testing for parameter binding.
- [ ] Add Spotless or Checkstyle and Maven Enforcer rules.
- [ ] Add GitHub Actions for build, tests, dependency review, release and Maven Central publishing.
- [ ] Publish Javadocs and a small runnable sample application.
