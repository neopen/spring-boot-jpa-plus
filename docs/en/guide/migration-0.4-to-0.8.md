# Migrating from 0.4 to 0.8

0.8 is the first feature-complete release after 0.4 and includes everything accumulated in 0.5, 0.6 and 0.7. This guide covers upgrade steps, how to enable new capabilities, behavior changes and rollback.

## Prerequisites

| Item | Requirement |
| --- | --- |
| JDK | 17 or later |
| Spring Boot | 3.x (3.2+ recommended) |
| JPA provider | Hibernate 6.x |
| Namespace | `jakarta.persistence.*` (`javax.persistence.*` is not supported) |

If the application is still on Spring Boot 2.x / `javax`, complete that framework upgrade first.

## Upgrade Steps

### 1. Upgrade dependencies

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>0.8.0</version>
</dependency>
```

Upgrade the extension module in the same pass when it is used:

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-extension</artifactId>
    <version>0.8.0</version>
</dependency>
```

- Remove any 0.4.x version pin in `dependencyManagement` or the parent POM.
- If `-core`, `-autoconfigure` or `-convert` are referenced directly, keep them on the same version as the starter.

### 2. Check configuration keys

0.8 only reads the `neopen.jpa.*` and `neopen.extension.*` prefixes. Keys under an old prefix are silently ignored, so review them after upgrading:

- Logical deletion: `neopen.jpa.logic-delete.enabled`
- Audit fields: `neopen.jpa.audit.*`
- Batch: `neopen.jpa.batch-size`
- Encryption: `neopen.jpa.encryption.*`
- Slow SQL: `neopen.jpa.slow-sql.*`

See the [configuration reference](/en/reference/configuration) for the full list.

### 3. Start and regression-test

1. Start the application and confirm there is no `ClassNotFoundException` / `NoSuchMethodError`, which would mean an old dependency is still on the classpath.
2. Enable SQL logging and compare statements with the pre-upgrade output: logical deletion, condition groups, `Specification`, `HqlWrapper` and `SqlWrapper` keep their 0.4 behavior.
3. Run core business flows and batch jobs to confirm transaction boundaries and results are unchanged.

### 4. Enable new capabilities as needed

Every new capability is **explicitly enabled**: without the annotation, provider or configuration, behavior matches 0.4.

| Capability | How to enable | Documentation |
| --- | --- | --- |
| Multi-tenancy | `@TenantId` + `@EntityListeners(TenantListener.class)` + `TenantProvider` | [Multi-tenancy](/en/core/tenant) |
| Row-level permission | Register a `DataPermissionProvider` | [Data permission](/en/core/data-permission) |
| Change history | `@Audited` + `@EntityListeners(AuditTrailListener.class)` + `AuditLogPublisher` | [Change history](/en/core/audit-trail) |
| Dynamic data sources | `neopen.jpa.datasource.enabled=true` + `targets` | [Data sources](/en/core/datasource) |
| Restore logically deleted rows | `LogicDeletes.restore(...)` | [Logical deletion](/en/core/logic-delete) |
| Batch upsert | `BatchOperation.upsertAll(...)` | [Batch operations](/en/core/batch) |
| Hibernate JDBC batch | `neopen.jpa.jdbc-batch.enabled=true` | [Batch operations](/en/core/batch) |
| Stream queries | `stream(...)` methods | [Executor](/en/core/executor) |
| Skipping filters | `@IgnoreTenant` / `@IgnoreDataScope` | [Skipping filters](/en/core/filter-ignore) |
| Metrics | Add Micrometer + `neopen.jpa.metrics.enabled=true` | [Metrics](/en/core/metrics) |
| Asynchronous audit publishing | `AsyncAuditLogPublisher` | [Change history](/en/core/audit-trail) |
| PostgreSQL JSONB | Custom type mapping | [PostgreSQL JSONB](/en/core/jsonb) |

## Behavior and Caveats

### Filter chain order

`JpaPlusSpecExecutor` appends conditions in a fixed order: **logical deletion → multi-tenancy → row-level permission**. The layers are combined with `Specification.and(...)`; a disabled or empty layer does not affect the others.

### Batch upsert semantics

`upsertAll` queries all business keys in one go, then persists or updates in chunks, flushing each chunk and clearing once at the end:

- All input entities are detached after the call; do not rely on lazy loading afterwards.
- Business keys must be unique; duplicates in one chunk match the same row.

### Two different "batch sizes"

| Configuration | Purpose |
| --- | --- |
| `neopen.jpa.batch-size` | Rows flushed / cleared per chunk by `BatchOperation`, default 500 |
| `hibernate.jdbc.batch_size` | Statements merged by the JDBC driver, written from `neopen.jpa.jdbc-batch.batch-size`, default 50 |

### No metrics data

With `neopen.jpa.metrics.enabled=true` but no Micrometer on the classpath, the metrics auto-configuration is not loaded and no meter is produced. Make sure `micrometer-core` or Actuator is present.

### Stream queries must be consumed inside a transaction

The stream returned by `stream(...)` is bound to the current persistence context. Consume it inside a transaction with try-with-resources; consuming it after the transaction ends fails.

## Verification Checklist

- [ ] Only one 0.8.0 version in the dependency tree (`mvn dependency:tree | findstr jpa-plus`)
- [ ] Application starts without auto-configuration errors
- [ ] Core query SQL matches the pre-upgrade statements
- [ ] Batch and scheduled jobs produce identical results
- [ ] Each enabled capability passes a smoke test following its documentation

## Rollback

- Downgrading the starter / extension to 0.4.x restores the previous behavior.
- If 0.5–0.7 annotations or APIs are already in use, remove them (and their configuration) before rolling back, otherwise startup fails on missing classes.
- Most capabilities do not change the schema; extra columns added for multi-tenancy or logical deletion can stay and are harmless to 0.4.

## FAQ

**Why do queries return fewer rows after upgrading?**

Check whether `@TenantId` was added by mistake, or whether a `DataPermissionProvider` returns a non-empty set. An empty collection from `DataScopes` means "no restriction", not "no permission".

**Why is no change history recorded?**

`@Audited` and `@EntityListeners(AuditTrailListener.class)` are both required, and an `AuditLogPublisher` must be registered. Publishing failures only log a WARNING and never break the business transaction.

**Why is filtering not applied?**

Make sure the query goes through `JpaPlusSpecExecutor` or `SpecWrapper`; `HqlWrapper`, `SqlWrapper` and native SQL never append filter conditions automatically.
