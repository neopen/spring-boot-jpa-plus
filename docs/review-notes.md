# Review notes for the first refactoring pass

## What changed

- Converted the root project from a runnable business demo into a JPA-focused Spring Boot starter artifact.
- Moved order entities, repositories, controllers and service code into `examples/demo`.
- Moved HTTP/AOP/date-validation/idempotency/rate-limit helpers and their cache utilities into `examples/legacy-web-extension`; they are not packaged in the core jar.
- Added Boot 3 auto-configuration metadata and conditional `hqlDao` / `sqlDao` bean registration.
- Removed Spring Cloud and Spring Cloud Alibaba BOMs, Springfox, Web/AOP, Hutool, Fastjson2 and Guava from the core dependency graph.
- Fixed positional parameter binding to use JPA's 1-based indexes.
- Made native SQL count conversion provider-neutral by accepting `Number`.
- Added `executeUpdate` for HQL and Native SQL.
- Added `persistAll(Collection, batchSize)`; it flushes in chunks and does not silently clear the caller's persistence context.
- Stopped swallowing batch persistence and predicate-building exceptions.
- Added a maximum page size and corrected small-page-size handling.
- Rewrote the old LRU implementation retained in the legacy example area and made its operations synchronized.
- Added unit tests for parameter binding, native count conversion, update execution, page parameter behavior and LRU behavior.
- Added README, architecture, dependency policy and roadmap documentation.

## Deliberate non-decisions

- No license was selected. The repository owner should choose the license and add the correct `LICENSE` file before public release.
- No Spring Boot 2 / `javax.persistence` compatibility is claimed. The current code line targets Java 17 and Spring Boot 3 / Jakarta.
- No Maven Central publication metadata, signing credentials or release workflow was invented.

## Verification status

The archive was statically inspected and the POM XML was parsed. `mvn clean verify` could not be executed in the current environment because Maven is not installed. Run the full build on a Java 17+ machine before merging or publishing. Add at least one PostgreSQL or MySQL integration test because native SQL behavior is provider/database dependent.
