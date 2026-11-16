# Configuration Properties

All properties are bound by `JpaPlusProperties` with the prefix `neopen.jpa`. Switches of extension components use the `neopen.extension` prefix.

## JPA Plus

| Property | Type | Default Value | Description |
| --- | --- | --- | --- |
| `neopen.jpa.enabled` | `boolean` | `true` | Whether to enable auto-configuration; when disabled, no executor is registered |
| `neopen.jpa.batch-size` | `int` | `500` | Default batch size for batch operations, written to `BatchOperation` at startup |
| `neopen.jpa.hql-enabled` | `boolean` | `true` | Whether to register `JpaPlusHqlExecutor` |
| `neopen.jpa.sql-enabled` | `boolean` | `true` | Whether to register `JpaPlusSqlExecutor` |
| `neopen.jpa.logic-delete.enabled` | `boolean` | `true` | Whether to append the logic delete filter automatically in queries |
| `neopen.jpa.audit.enabled` | `boolean` | `true` | Whether to enable audit field population |
| `neopen.jpa.audit.zone` | `ZoneId` | `UTC` | Time zone used by audit time fields |
| `neopen.jpa.audit.auditor` | `String` | empty | Fixed operator name; nothing is written when it is empty. An `AuditorProvider` bean takes precedence |
| `neopen.jpa.audit.trail-enabled` | `boolean` | `true` | Whether to publish change history for `@Audited` entities; requires an `AuditLogPublisher` |
| `neopen.jpa.tenant.enabled` | `boolean` | `true` | Whether to append tenant filters for entities declaring `@TenantId` |
| `neopen.jpa.data-scope.enabled` | `boolean` | `true` | Whether to append the row-level rules returned by `DataPermissionProvider` |
| `neopen.jpa.datasource.enabled` | `boolean` | `false` | Whether to register the routing data source and take over the default data source |
| `neopen.jpa.datasource.primary` | `String` | `primary` | Default logical data source name, must be present in `targets` |
| `neopen.jpa.datasource.read-target` | `String` | `read` | Default target of `@DataSource(readOnly = true)` |
| `neopen.jpa.datasource.write-target` | `String` | `write` | Default target of `@DataSource(readOnly = false)` |
| `neopen.jpa.datasource.targets` | `Map<String, String>` | empty | Logical name to data source bean name mapping, required when dynamic data sources are enabled |
| `neopen.jpa.encryption.enabled` | `boolean` | `true` | Whether to enable field encryption; when disabled, the encryption converter reads and writes plaintext |
| `neopen.jpa.encryption.key` | `String` | empty | Encryption key, derived through SHA-256 at startup; reading or writing encrypted fields throws an exception when it is not configured |
| `neopen.jpa.encryption.keys` | `Map<String, String>` | empty | Multiple keys keyed by key id; takes precedence over `encryption.key` |
| `neopen.jpa.encryption.active-key-id` | `String` | empty | Key id used for new ciphertext; required when multiple keys are configured |
| `neopen.jpa.slow-sql.enabled` | `boolean` | `false` | Whether to log slow SQL through Hibernate's threshold |
| `neopen.jpa.slow-sql.threshold-ms` | `long` | `1000` | Slow SQL threshold in milliseconds, must be greater than 0 |
| `neopen.jpa.scopes.<name>` | `object` | empty | Overrides the switches above per datasource scope, see "Multi-datasource scope isolation" below |

## Extension Components

| Property | Type | Default Value | Description |
| --- | --- | --- | --- |
| `neopen.extension.idempotent.enabled` | `boolean` | `true` | Whether to register the idempotency aspect; enabled by default when not configured, effective only when the Servlet API is present |
| `neopen.extension.rate-limit.enabled` | `boolean` | `true` | Whether to register the rate limit aspect; enabled by default when not configured, requires AspectJ and Guava `RateLimiter` |
| `neopen.extension.retry.enabled` | `boolean` | `true` | Whether to register the optimistic lock retry aspect, see [Optimistic lock retry](/en/extension/retry) |
| `neopen.extension.slow-log.enabled` | `boolean` | `true` | Whether to register the slow method log aspect, see [Slow method log](/en/extension/slow-log) |
| `limiter.enable` | `boolean` | `false` | Request rate limit switch built into `SysLogAspect`, independent of `@RateLimit` |

## Example

```yaml
neopen:
  jpa:
    enabled: true
    batch-size: 500
    hql-enabled: true
    sql-enabled: true
    logic-delete:
      enabled: true
    audit:
      enabled: true
      zone: Asia/Shanghai
      auditor: system
      trail-enabled: true
    tenant:
      enabled: true
    data-scope:
      enabled: true
    datasource:
      enabled: false
      primary: primary
      read-target: read
      write-target: write
      targets:
        primary: dataSourcePrimary
        read: dataSourceRead
        write: dataSourceWrite
    encryption:
      enabled: true
      key: change-me-in-production
      # Use multiple keys when rotating; keys takes precedence over key
      # keys:
      #   v1: old-secret
      #   v2: new-secret
      # active-key-id: v2
    slow-sql:
      enabled: true
      threshold-ms: 1000
  extension:
    idempotent:
      enabled: true
    rate-limit:
      enabled: true
    retry:
      enabled: true
    slow-log:
      enabled: true
```

## Effect at Startup

| Property | Applied To |
| --- | --- |
| `batch-size` | `BatchOperation.setDefaultBatchSize(...)` |
| `logic-delete.enabled` | `LogicDeletes.setEnabled(...)` |
| `audit.enabled` / `audit.auditor` / `audit.zone` | `Audits.setEnabled(...)`, `Audits.setAuditorSupplier(...)`, `Audits.setClock(...)` |
| `audit.trail-enabled` | `AuditTrails.setEnabled(...)`, registers the `AuditLogPublisher` bean |
| `tenant.enabled` | `Tenants.setEnabled(...)`, registers the `TenantProvider` bean |
| `data-scope.enabled` | `DataScopes.setEnabled(...)`, registers the `DataPermissionProvider` bean |
| `datasource.*` | Registers `jpaPlusRoutingDataSource` and `DataSourceAspect`, resolving targets from `targets` |
| `encryption.enabled` / `encryption.key` | `EncryptedFields.setEnabled(...)`, `EncryptedFields.setKey(...)` (written only when the key is configured) |
| `encryption.keys` / `encryption.active-key-id` | `EncryptedFields.setKeys(...)` (used when multiple keys are configured) |
| `slow-sql.enabled` / `slow-sql.threshold-ms` | Registers `SlowSqlHibernateCustomizer` with Hibernate's slow query threshold |

These values are written to static switches when the application starts, so all consumers in the same JVM share them. They can also be adjusted at runtime through the setters of the corresponding utility classes.

Properties that are not configured do not override values already set in code.

## Multi-datasource scope isolation

The logic-delete, auditing, encryption and batch-size switches apply to the whole JVM by default. A multi-datasource application can configure independent switches per scope so a read replica or archive database is not affected by the primary configuration:

```yaml
neopen:
  jpa:
    batch-size: 500
    logic-delete:
      enabled: true
    audit:
      enabled: true
      auditor: system
    encryption:
      key: global-secret
    scopes:
      secondary:
        batch-size: 200
        logic-delete-enabled: false
        audit-enabled: false
        audit-auditor: batch-job
        encryption-enabled: true
        encryption-keys:
          v1: secondary-secret
```

| Property | Default | Description |
| --- | --- | --- |
| `scopes.<name>.batch-size` | empty | Default batch size for the scope, must be greater than 0 |
| `scopes.<name>.logic-delete-enabled` | empty | Whether the scope filters logically deleted rows |
| `scopes.<name>.audit-enabled` | empty | Whether the scope fills audit fields |
| `scopes.<name>.audit-auditor` | empty | Fixed auditor used by the scope |
| `scopes.<name>.encryption-enabled` | empty | Whether field encryption is enabled for the scope |
| `scopes.<name>.encryption-key` | empty | Single encryption key for the scope |
| `scopes.<name>.encryption-keys` | empty | Multiple keys for the scope |
| `scopes.<name>.encryption-active-key-id` | empty | Key id used for new ciphertext in the scope |
| `scopes.<name>.tenant-enabled` | empty | Whether the scope filters by tenant |
| `scopes.<name>.tenant` | empty | Fixed tenant for the scope, takes precedence over providers |
| `scopes.<name>.trail-enabled` | empty | Whether the scope publishes change history |

Unconfigured items fall back to the global settings. Bind the scope when accessing that datasource:

```java
JpaPlusScope.run("secondary", () -> {
    // Logic delete, auditing, encryption and batch size follow the secondary scope
    secondaryRepository.findAll(spec);
});

// Use call when a value is returned
Page<Order> page = JpaPlusScope.call(
        "secondary",
        () -> secondaryRepository.findAll(spec, pageable)
);
```

- The binding affects only the current thread and restores the previous scope afterwards.
- Bind it in a datasource routing aspect, an `@Async` task or a scheduled job entry instead of every DAO call.
- Unknown scope names do not fail; they simply fall back to the global settings.

## Auto-Configuration Conditions

`JpaPlusAutoConfiguration` takes effect only when all of the following are met:

- `jakarta.persistence` and Spring Data JPA are present on the classpath
- An `EntityManagerFactory` exists in the container
- `neopen.jpa.enabled` is `true`

The executor beans are named `jpaPlusHqlExecutor` and `jpaPlusSqlExecutor`, both annotated with `@ConditionalOnMissingBean`: they are not overridden when the application defines a bean of the same type.

## Disabling Part of the Capabilities

```yaml
neopen:
  jpa:
    hql-enabled: false       # 不用 HQL 时不注册执行器
    sql-enabled: false       # 不用原生 SQL 时不注册执行器
    logic-delete:
      enabled: false         # 维护任务需要读取已删除数据
    audit:
      enabled: false         # 历史数据导入时不希望触发审计
    encryption:
      enabled: false         # 迁移历史明文数据时临时关闭加解密
```

Note that `logic-delete.enabled=false` only affects query filtering; write-side methods such as `LogicDeletes.deleteById` still mark the field as deleted.
