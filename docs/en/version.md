# Releases

::: warning Important notice
- The current official release is **0.4.0**, available as a Maven dependency.
- The `main` branch keeps the earlier development line and is not released; capabilities from 0.5 onward will ship with **0.8.0**.
- The `dev` branch corresponds to **0.8.0**, the release-preparation version after 0.7.0. It adds cursor pagination, streaming batches and cross-thread context propagation on top of 0.7.0, with a focus on compatibility, API review and release validation.
- This documentation is written for **0.8.0** and describes the latest behavior of `dev`.
- As a result, the **0.4.0 release installed from Maven does not support** the 0.5 / 0.6 / 0.7 / 0.8 capabilities described here.
:::

## Version overview

| Version | Status | Date | Summary |
| --- | --- | --- | --- |
| `0.8.0` | In development | — | Release preparation: cursor pagination, streaming batches, cross-thread context propagation, plus the compatibility matrix, API review and performance baseline |
| `0.7.0` | In development | — | Filter skipping, `OR` rules with association paths, stream queries, JDBC batch, Micrometer metrics, asynchronous audit publishing |
| `0.6.0` | In development | — | Multi-tenancy, row-level data permission, change history, dynamic data sources and read/write splitting, logical-delete restoration, batch upsert |
| `0.5.0` | In development | — | Adds enum conversion, field encryption, desensitization, retry, slow logging, tree building and snowflake IDs |
| `0.4.0` | Released | 2026-11-13 | Logical deletion, condition groups, Specification executor; current stable version |
| `0.3.0` | Released | 2026-11-13 | Annotation-driven conditions, safe sorting, fetch join paging |
| `0.2.1` | Released | 2026-11-12 | Example and stability adjustments, no API changes |
| `0.2.0` | Released | 2026-11-12 | Chained Specification builder `SpecWrapper` |
| `0.1.2` | Released | 2026-11-12 | Chained native SQL wrapper and join examples |
| `0.1.1` | Released | 2026-11-11 | Chained HQL queries and dual example implementations |
| `0.1.0` | Released | 2026-11-05 | First release: module skeleton, DAO, query executors, conversion and extensions |

## Compatibility baseline

| Item | 0.4.0 | 0.5.0 / 0.6.0 / 0.7.0 / 0.8.0 |
| --- | --- | --- |
| JDK | 17+ | 17+ |
| Spring Boot | 3.x | 3.x |
| Hibernate | 6.x | 6.x |
| Persistence API | `jakarta.persistence` | `jakarta.persistence` |
| Maven | 3.x | 3.x |
| Modules | core / convert / autoconfigure / starter / extension | Same as 0.4.0 |
| Coordinates | `io.github.neopen:*` | Same as 0.4.0 |

`starter` only brings in `core`, `convert` and `autoconfigure`; `extension` has to be added separately in every version.

## 0.8.0 (in development)

0.8.0 adds pagination, streaming consumption and context consistency on top of 0.7.0, and completes the pre-release quality work. Module structure and Maven coordinates stay unchanged.

### New: pagination and streaming

| Capability | Entry point | Description |
| --- | --- | --- |
| Cursor pagination | `KeysetPageParams`, `KeysetCursor`, `KeysetPage`, `JpaPlusSpecExecutor#keyset` | Keyset query on `(sort key, primary key)` with an automatic unique tiebreaker; case-insensitive and nullable sort columns are rejected |
| Streaming batches | `JpaPlusSpecExecutor#forEachBatch`, `JpaPlusHqlExecutor#forEachBatch`, `JpaPlusSqlExecutor#forEachBatch` | Aggregates streamed rows into batches, runs `flush` / `clear` after each callback and closes the stream on failure or early exit |

### New: context propagation

| Capability | Entry point | Description |
| --- | --- | --- |
| Context snapshot | `JpaPlusContext` | Scope, data-permission and tenant ignore depths, and the data-source routing stack |
| Task decorator | `JpaPlusContextTaskDecorator` | Captures on submit and restores on completion, supporting nesting and exceptional exits |
| Property switch | `neopen.jpa.context-propagation.enabled` | Enabled by default; an existing custom `TaskDecorator` is never overridden |

### Quality and documentation

- Coverage gates in `verify`: core line coverage at least 70%, autoconfigure at least 72%.
- New `MetricsOverheadBenchmark`; both performance pages now include metrics and logging overhead data.
- New FAQ pages in Chinese and English covering filter order, transaction boundaries, skipping filters, async propagation and stream closing.
- New public types are registered in `API-REVIEW.md`.

## 0.7.0 (in development)

### New: filter control

| Capability | Entry point | Description |
| --- | --- | --- |
| Per-method skipping | `@IgnoreTenant`, `@IgnoreDataScope` | Skips tenant and data-permission conditions per method or class, with nested restoration and exception safety |
| Programmatic skipping | `Tenants#runIgnored` / `callIgnored`, `DataScopes#runIgnored` / `callIgnored` | Skips filtering inside a thread without annotations |

### New: data-permission enhancements

| Capability | Entry point | Description |
| --- | --- | --- |
| `OR` combination | `DataRule.or(...)`, `DataRule.Logic` | Rules are combined left to right in declaration order, with `AND` and `OR` |
| Association paths | `DataRule.of("dept.parentId", ...)` | Field names support `a.b` association paths |

### New: queries and batching

| Capability | Entry point | Description |
| --- | --- | --- |
| Stream queries | `JpaPlusSpecExecutor#stream`, `JpaPlusHqlExecutor#stream`, `JpaPlusSqlExecutor#stream` | Consumes results through `getResultStream`, suitable for large exports |
| upsert optimization | `BatchOperation#upsertAll` | Queries all business keys once, then writes in chunks and clears at the end |
| JDBC batch | `neopen.jpa.jdbc-batch.*` | Writes `hibernate.jdbc.batch_size`, `hibernate.order_inserts` and `hibernate.order_updates` automatically |

### New: observability and auditing

| Capability | Entry point | Description |
| --- | --- | --- |
| Metrics | `JpaPlusMetrics`, `neopen.jpa.metrics.*` | Micrometer counters for data source routes and slow SQL |
| Async audit | `AsyncAuditLogPublisher` | Bounded queue with a discard policy and drain-on-close, so the business transaction never blocks |

### New: configuration

- `neopen.jpa.jdbc-batch.enabled` (default `false`), `batch-size` (default `50`), `order-inserts` / `order-updates` (default `true`).
- `neopen.jpa.metrics.enabled` (default `false`), `slow-threshold-ms` (default `1000`).

### Changes and migration

- Filter skipping is off by default; it only applies when the annotation is present or the skip methods are called.
- `DataRule` is now `record(field, values, logic)`; `new DataRule(field, values)` and `DataRule.of(...)` stay compatible and default to `AND`, use `DataRule.or(...)` for `OR`.
- `BatchOperation#upsertAll` now queries all business keys once, only `flush()`es between chunks and `clear()`s once after everything is processed.
- Metrics only apply when `micrometer-core` is present and `neopen.jpa.metrics.enabled=true`; slow SQL timing wraps the `DataSource` at the JDBC layer and does not depend on SQL style.

### Documentation

- Added a PostgreSQL JSONB field mapping and query example; see [PostgreSQL JSONB](/en/core/jsonb).

## 0.6.0 (in development)

### New: multi-tenancy and data permission

| Capability | Entry point | Description |
| --- | --- | --- |
| Multi-tenancy | `@TenantId`, `TenantProvider`, `TenantListener`, `Tenants` | Fills the tenant column on write and filters automatically on query, with an optional fixed tenant per scope |
| Row-level data permission | `DataPermissionProvider`, `DataRule`, `DataScopes` | Converts rules into `field IN (...)` conditions combined with logical deletion and tenant conditions using `AND` |

### New: change history

| Capability | Entry point | Description |
| --- | --- | --- |
| Change history | `@Audited`, `AuditTrailListener`, `AuditTrails`, `AuditLogPublisher` | Field-level before/after snapshots published as `CREATE` / `UPDATE` / `DELETE` records |
| Record model | `AuditEntry`, `AuditChange`, `AuditAction` | Entity, id, change details, actor and timestamp |

### New: dynamic data sources

| Capability | Entry point | Description |
| --- | --- | --- |
| Routing data source | `neopen.jpa.datasource.*`, `JpaPlusRoutingDataSource` | Maps logical names to data source beans and takes over the Spring Boot default |
| Annotation switching | `@DataSource` | Selects a target per method or class, supports read/write splitting via `readOnly` |
| Manual switching | `DataSourceContext` | Thread-local stack with `run` / `call` and nested restoration |

### New: logical deletion and batch improvements

| Capability | Entry point | Description |
| --- | --- | --- |
| Logical-delete restoration | `LogicDeletes#restoreById`, `restoreAllById`, `restoreAll` | Restores by primary key in bulk or clears the flag on managed entities |
| Batch upsert | `BatchOperation#upsertAll`, `UpsertResult` | Queries by business key in chunks and inserts or updates accordingly |

### New: configuration

- `neopen.jpa.tenant.enabled` (default `true`).
- `neopen.jpa.data-scope.enabled` (default `true`).
- `neopen.jpa.audit.trail-enabled` (default `true`).
- `neopen.jpa.datasource.*`: `enabled`, `primary`, `read-target`, `write-target`, `targets`.
- `neopen.jpa.scopes.<name>` gains `tenant-enabled`, `tenant` and `trail-enabled`.

### Changes and migration

- The routing data source is registered through `@AutoConfiguration(before = DataSourceAutoConfiguration.class)` and only applies when `neopen.jpa.datasource.enabled=true` and no `DataSource` bean exists; projects with a custom data source are unaffected.
- Tenant and data-permission filtering are enabled by default but only produce conditions when `@TenantId` is declared or a `DataPermissionProvider` is registered, so behavior is unchanged for projects that do not use them.
- Change history is enabled by default but only records entities annotated with `@Audited` and the `AuditTrailListener`.

## 0.5.0 (in development)

### New: field conversion and data security

| Capability | Entry point | Description |
| --- | --- | --- |
| Enum value conversion | `@EnumValue`, `EnumValues`, `EnumValueConverter` | Persists enums as explicit business values, declared either on constants or on a no-arg method |
| Field encryption | `@EncryptedField`, `EncryptedFields`, `EncryptedFieldConverter` | Transparent AES/GCM encryption with random IV; accepts a string secret or a 16/24/32-byte key |
| Key rotation | `EncryptedFields#setKeys`, `neopen.jpa.encryption.keys` | Multiple keys with a key-id prefix on new ciphertext; historical data stays readable |
| Deterministic encryption | `EncryptedFields#encryptDeterministic`, `DeterministicEncryptedFieldConverter` | IV derived from the plaintext so equal values produce equal ciphertext and support equality search |
| Data desensitization | `@Desensitize`, `Desensitizes`, `DesensitizeValueFilter` | Masks names, phones, ID cards, bank cards, emails, addresses and passwords, with fastjson2 and Jackson integration |

### New: performance and reliability

| Capability | Entry point | Description |
| --- | --- | --- |
| Optimistic-lock retry | `@RetryOnOptimisticLock` | Retries on optimistic-lock failures, default 3 attempts with a 50ms delay |
| Slow method logging | `@SlowLog` | Logs a WARN when a method exceeds the threshold (default 500ms) |
| Slow SQL logging | `neopen.jpa.slow-sql` | Uses Hibernate's slow-query threshold to log SQL and execution time above the limit |
| Auditing | `@CreatedAt`, `@UpdatedAt`, `@CreatedBy`, `@UpdatedBy`, `Audits` | Maintains creation and update metadata through an entity listener; plug in `AuditorProvider` for the current user |
| Logical deletion write side | `LogicDeletes` | Completes update and delete handling on top of the 0.4.0 query filter |
| Batch enhancements | `BatchOperation` | Flushes / clears per batch and supports batch save, update and delete |

### New: query enhancements

| Capability | Entry point | Description |
| --- | --- | --- |
| Raw fragments and subqueries | `HqlWrapper#raw`, `SqlWrapper#raw`, `exists`, `notExists` | Appends static condition fragments and `exists` subqueries with named parameter binding |
| Grouped queries | `HqlWrapper#select`, `groupBy`, `having`, `SqlWrapper#having` | Aggregate columns, grouping and group filters; the SQL count is wrapped as a subquery |
| union | `SqlWrapper#union`, `unionAll` | Merges query parts with the same shape; parameters are renumbered and pagination counts correctly |
| Request paging | `PageParams` | Validates page number, size limit and sort fields before converting to `Pageable` |
| Field mapping | `HqlWrapper#alias`, `SqlWrapper#alias` | Maps query parameter names to entity properties or column names |
| Existence check | `JpaPlusSpecExecutor#exists` | Count-based existence check without loading entities |
| DTO projection | `ResultMapper` | Matches DTO fields by alias, accepting camelCase and snake_case |
| Param to entity mapping | `ParamMapper`, `@ParamField`, `@ParamIgnore` | Maps a parameter object onto an entity, overwriting non-null fields only |

### New: building blocks

| Capability | Entry point | Description |
| --- | --- | --- |
| Tree building | `TreeUtils` | Builds trees from flat lists by id / parentId and detects duplicates, self references and cycles |
| Snowflake ID | `SnowflakeIdGenerator` | 41-bit timestamp + 5-bit datacenter + 5-bit worker + 12-bit sequence; fails fast on clock rollback |

### New: extensions and configuration

- Annotation-driven rate limiting via `@RateLimit` and idempotency enhancements.
- Aspect switches: `neopen.extension.retry.enabled` and `neopen.extension.slow-log.enabled`, enabled by default.
- Encryption settings: `neopen.jpa.encryption.enabled` (default `true`) and `neopen.jpa.encryption.key`.
- Key rotation settings: `neopen.jpa.encryption.keys` (key id to secret) and `neopen.jpa.encryption.active-key-id`.
- Slow SQL settings: `neopen.jpa.slow-sql.enabled` (default `false`) and `neopen.jpa.slow-sql.threshold-ms` (default `1000`).
- When Jackson is present, the extension module registers `DesensitizeModule` automatically; without Jackson nothing changes.
- Register an `AuditorProvider` bean to resolve the current user dynamically; it takes precedence over `neopen.jpa.audit.auditor`.
- Multi-datasource isolation: `JpaPlusScope` binds logic-delete, auditing, encryption and batch-size switches per scope, configured through `neopen.jpa.scopes.<name>`.

### Changes and migration

- The `BaseVo` base class constraint is removed, so view objects can be any POJO; VOs extending `BaseVo` in 0.4.0 must drop that parent class.
- Without `neopen.jpa.encryption.key`, reading or writing `@EncryptedField` fields throws `JpaPlusException` instead of silently storing plaintext.
- The new aspects are enabled by default and can be disabled with the switches above.

## 0.4.0 (current release)

### Scope

- **Core persistence**: `JpaPlusDao` and `AbstractJpaPlusDao` provide CRUD, pagination and batch operations; `QueryWrapper` provides chained dynamic conditions.
- **Query builders**: `HqlWrapper` and `SqlWrapper` build HQL and native SQL chains; `JpaPlusHqlExecutor` and `JpaPlusSqlExecutor` handle pagination and counting.
- **Specification**: `SpecWrapper` builds `Specification`; `JpaPlusSpecExecutor` handles counting, pagination and `countDistinct`; `FetchJoinPaging` uses count → primary key paging → primary key loading to avoid duplicate rows from joins.
- **Annotated conditions**: `@QueryCondition` supports operators, field aliases, range end fields and `group` grouping (OR within a group, AND across groups).
- **Safe sorting**: `Sorts` validates sort expressions against entity fields or an allow-list.
- **Logical deletion**: `@LogicDelete` + `LogicDeletes` automatically appends the filter to Criteria queries.
- **Conversion**: `BaseVoService`, `AbstractBaseService`, `BaseVoCommom` and `BaseServiceFactory` provide Entity → VO conversion; `@DateStrFormat` and `@DateStrValid` handle date strings.
- **Extensions**: `@Idempotent` idempotency, `@SysControllerLog` / `@SysServiceLog` operation logging, `JsonPageConverter` unified paged responses, and the `RateLimiterUtil` local rate limiting utility.

### Limitations

- Auditing fields, DTO projection, param-to-entity mapping and the `@RateLimit` annotation are not available yet; they arrive in 0.5.0.
- Logical deletion only applies to queries; write-side update and delete handling is completed in 0.5.0.
- Rate limiting is a utility only and has to be invoked by application code.

## 0.3.0

- Added `@QueryCondition` annotation-driven query conditions.
- Added `Sorts` safe sorting.
- Added `FetchJoinPaging` fetch join pagination.

## 0.2.1

- Example project and stability adjustments; no public API changes.

## 0.2.0

- Added `SpecWrapper` for building JPA `Specification` in a chained style.

## 0.1.2

- Added `SqlWrapper` and `JpaPlusSqlExecutor` for chained native SQL queries.
- Added JPA join query examples.

## 0.1.1

- Added `HqlWrapper` and `JpaPlusHqlExecutor` for chained HQL queries.
- The example project provides both JPA and HQL implementations.

## 0.1.0

- First release: module skeleton for `core`, `convert`, `autoconfigure`, `starter` and `extension`.
- Provided `JpaPlusDao` / `AbstractJpaPlusDao`, `QueryWrapper`, HQL and SQL executors, and `BatchOperation`.
- Provided Entity → VO conversion plus idempotency, operation logging and unified paged response extensions.

## Choosing a version

- **Stable usage**: depend on 0.4.0 and use the capabilities documented for 0.4.0.
- **Try 0.5 / 0.6 / 0.7 / 0.8 early**: clone the repository, run `mvn install -DskipTests` to install into the local repository, then point the dependency version at the locally built artifact.
- **Wait for the release**: capabilities from 0.5.0 onward will ship together with 0.8.0, and this documentation will then match the Maven artifacts.
