# 配置项

所有配置项由 `JpaPlusProperties` 绑定，前缀为 `neopen.jpa`。扩展组件的开关使用 `neopen.extension` 前缀。

## JPA Plus

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `neopen.jpa.enabled` | `boolean` | `true` | 是否启用自动配置；关闭后不注册任何执行器 |
| `neopen.jpa.batch-size` | `int` | `500` | 批量操作默认批次大小，启动时写入 `BatchOperation` |
| `neopen.jpa.hql-enabled` | `boolean` | `true` | 是否注册 `JpaPlusHqlExecutor` |
| `neopen.jpa.sql-enabled` | `boolean` | `true` | 是否注册 `JpaPlusSqlExecutor` |
| `neopen.jpa.logic-delete.enabled` | `boolean` | `true` | 是否在查询中自动追加逻辑删除过滤 |
| `neopen.jpa.audit.enabled` | `boolean` | `true` | 是否启用审计字段填充 |
| `neopen.jpa.audit.zone` | `ZoneId` | `UTC` | 审计时间字段使用的时区 |
| `neopen.jpa.audit.auditor` | `String` | 空 | 固定的操作人名称，为空时不写入；容器中存在 `AuditorProvider` Bean 时以该 Bean 为准 |
| `neopen.jpa.encryption.enabled` | `boolean` | `true` | 是否启用字段加密；关闭后加密转换器按明文读写 |
| `neopen.jpa.encryption.key` | `String` | 空 | 加密密钥，启动时经 SHA-256 派生；未配置时读写加密字段会抛出异常 |
| `neopen.jpa.encryption.keys` | `Map<String, String>` | 空 | 多密钥配置，键为密钥编号、值为密钥；配置后优先于 `encryption.key` |
| `neopen.jpa.encryption.active-key-id` | `String` | 空 | 当前加密使用的密钥编号；配置多把密钥时必须指定 |
| `neopen.jpa.slow-sql.enabled` | `boolean` | `false` | 是否记录慢 SQL，开启后通过 Hibernate 阈值输出 |
| `neopen.jpa.slow-sql.threshold-ms` | `long` | `1000` | 慢 SQL 阈值，单位毫秒，必须大于 0 |
| `neopen.jpa.scopes.<name>` | `object` | 空 | 按数据源作用域覆盖上述开关，见下文「多数据源作用域隔离」 |

## 扩展组件

| 配置项 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `neopen.extension.idempotent.enabled` | `boolean` | `true` | 是否注册幂等切面；未配置时默认启用，仅在存在 Servlet API 时生效 |
| `neopen.extension.rate-limit.enabled` | `boolean` | `true` | 是否注册限流切面；未配置时默认启用，需存在 AspectJ 与 Guava `RateLimiter` |
| `neopen.extension.retry.enabled` | `boolean` | `true` | 是否注册乐观锁重试切面，见 [乐观锁重试](/extension/retry) |
| `neopen.extension.slow-log.enabled` | `boolean` | `true` | 是否注册慢方法日志切面，见 [慢方法日志](/extension/slow-log) |
| `limiter.enable` | `boolean` | `false` | `SysLogAspect` 内置的请求限流开关，与 `@RateLimit` 相互独立 |

## 示例

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
      # 需要轮换时改用多密钥配置，keys 优先于 key
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

## 启动时的作用

| 配置 | 作用点 |
| --- | --- |
| `batch-size` | `BatchOperation.setDefaultBatchSize(...)` |
| `logic-delete.enabled` | `LogicDeletes.setEnabled(...)` |
| `audit.enabled` / `audit.auditor` / `audit.zone` | `Audits.setEnabled(...)`、`Audits.setAuditorSupplier(...)`、`Audits.setClock(...)` |
| `encryption.enabled` / `encryption.key` | `EncryptedFields.setEnabled(...)`、`EncryptedFields.setKey(...)`（仅在配置了密钥时写入） |
| `encryption.keys` / `encryption.active-key-id` | `EncryptedFields.setKeys(...)`（配置了多密钥时优先使用） |
| `slow-sql.enabled` / `slow-sql.threshold-ms` | 注册 `SlowSqlHibernateCustomizer`，写入 Hibernate 慢查询阈值 |

这些值在应用启动时写入静态开关，因此同一 JVM 内的所有使用方共享。运行期也可以通过对应工具类的 setter 调整。

未配置的属性不会覆盖代码中已设置的值。

## 多数据源作用域隔离

逻辑删除、审计、加密和批量大小的开关默认对整个 JVM 生效。多数据源应用可以按作用域名称配置独立开关，避免从库或归档库被主库配置影响：

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

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `scopes.<name>.batch-size` | 空 | 该作用域的默认批次大小，必须大于 0 |
| `scopes.<name>.logic-delete-enabled` | 空 | 该作用域是否追加逻辑删除过滤 |
| `scopes.<name>.audit-enabled` | 空 | 该作用域是否填充审计字段 |
| `scopes.<name>.audit-auditor` | 空 | 该作用域使用的固定操作人 |
| `scopes.<name>.encryption-enabled` | 空 | 该作用域是否启用字段加密 |
| `scopes.<name>.encryption-key` | 空 | 该作用域的单密钥 |
| `scopes.<name>.encryption-keys` | 空 | 该作用域的多密钥配置 |
| `scopes.<name>.encryption-active-key-id` | 空 | 该作用域当前加密使用的密钥编号 |

未配置的项沿用全局配置。访问对应数据源时通过 `JpaPlusScope` 绑定作用域：

```java
JpaPlusScope.run("secondary", () -> {
    // 逻辑删除、审计、加密与批量大小都使用 secondary 的配置
    secondaryRepository.findAll(spec);
});

// 有返回值时使用 call
Page<Order> page = JpaPlusScope.call(
        "secondary",
        () -> secondaryRepository.findAll(spec, pageable)
);
```

- 绑定只作用于当前线程，执行结束后自动恢复外层作用域。
- 建议在多数据源路由切面、`@Async` 任务或定时任务入口统一绑定，避免在每个 DAO 调用处重复设置。
- 未注册的作用域名称不会报错，直接沿用全局配置。

## 自动配置条件

`JpaPlusAutoConfiguration` 生效需要同时满足：

- classpath 中存在 `jakarta.persistence` 与 Spring Data JPA
- 容器中存在 `EntityManagerFactory`
- `neopen.jpa.enabled` 为 `true`

执行器 Bean 名称为 `jpaPlusHqlExecutor` 与 `jpaPlusSqlExecutor`，均为 `@ConditionalOnMissingBean`：业务自定义同类型 Bean 时不会被覆盖。

## 关闭部分能力

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

需要注意：`logic-delete.enabled=false` 只影响查询过滤，写侧的 `LogicDeletes.deleteById` 等方法仍然会把字段置为已删除。
