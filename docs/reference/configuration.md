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
| `neopen.jpa.audit.auditor` | `String` | 空 | 固定的操作人名称，为空时不写入 |
| `neopen.jpa.encryption.enabled` | `boolean` | `true` | 是否启用字段加密；关闭后加密转换器按明文读写 |
| `neopen.jpa.encryption.key` | `String` | 空 | 加密密钥，启动时经 SHA-256 派生；未配置时读写加密字段会抛出异常 |

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

这些值在应用启动时写入静态开关，因此同一 JVM 内的所有使用方共享。运行期也可以通过对应工具类的 setter 调整。

未配置的属性不会覆盖代码中已设置的值。

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
