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
| `neopen.jpa.audit.auditor` | `String` | empty | Fixed operator name; nothing is written when it is empty |
| `neopen.jpa.encryption.enabled` | `boolean` | `true` | Whether to enable field encryption; when disabled, the encryption converter reads and writes plaintext |
| `neopen.jpa.encryption.key` | `String` | empty | Encryption key, derived through SHA-256 at startup; reading or writing encrypted fields throws an exception when it is not configured |

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
    encryption:
      enabled: true
      key: change-me-in-production
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
| `encryption.enabled` / `encryption.key` | `EncryptedFields.setEnabled(...)`, `EncryptedFields.setKey(...)` (written only when the key is configured) |

These values are written to static switches when the application starts, so all consumers in the same JVM share them. They can also be adjusted at runtime through the setters of the corresponding utility classes.

Properties that are not configured do not override values already set in code.

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
