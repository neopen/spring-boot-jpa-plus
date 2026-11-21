# 公开 API 盘点与冻结草案

面向 1.0 的 API 冻结前置盘点。范围：`core`、`autoconfigure`、`convert`、`extension` 四个库模块的 `public` 类型，共 122 个、36 个包；`starter` 仅做依赖聚合，`examples` 不参与 API 承诺。

## 稳定性分级

| 级别 | 含义 | 1.0 承诺 |
| --- | --- | --- |
| 稳定（Stable） | 业务代码直接依赖的注解、门面、执行器、配置项 | 次版本内不破坏，移除需经过弃用周期 |
| 扩展点（SPI） | 允许用户实现或覆盖的接口与基类 | 方法签名稳定，可新增默认方法 |
| 内部（Internal） | `public` 仅为跨包或框架装配需要 | 不承诺兼容，升级可能变更 |

## 稳定 API

### core

| 包 | 类型 |
| --- | --- |
| `persistence.logicdelete` | `LogicDelete`、`LogicDeletes` |
| `persistence.tenant` | `TenantId`、`TenantProvider`、`Tenants`、`IgnoreTenant` |
| `persistence.datascope` | `DataPermissionProvider`、`DataRule`、`DataScopes`、`IgnoreDataScope` |
| `persistence.audit` | `CreatedBy`、`CreatedAt`、`UpdatedBy`、`UpdatedAt`、`AuditorProvider`、`Audits` |
| `persistence.audit.trail` | `Audited`、`AuditEntry`、`AuditChange`、`AuditAction`、`AuditLogPublisher`、`AuditTrails`、`AsyncAuditLogPublisher` |
| `persistence.encrypt` | `EncryptedField`、`EncryptedFields` |
| `persistence.batch` | `BatchOperation` |
| `persistence.spec` | `JpaPlusSpecExecutor`、`SpecWrapper`、`SpecOperator`、`StandardSpecOperator`、`FetchJoinPaging` |
| `persistence.hql` | `JpaPlusHqlExecutor`、`HqlWrapper`、`HqlQuery`、`HqlOperator`、`StandardHqlOperator` |
| `persistence.sql` | `JpaPlusSqlExecutor`、`SqlWrapper`、`SqlQuery`、`SqlOperator`、`StandardSqlOperator` |
| `persistence.query` | `JpaPlusQueryBuilder`、`QueryWrapper`、`QueryOperator` |
| `persistence.condition` | `QueryCondition`、`QueryConditionEntry`、`QueryConditionMapper`、`ConditionApplier`、`ConditionOperator` |
| `persistence.dao` | `JpaPlusDao` |
| `persistence.datasource` | `DataSource`、`DataSourceContext` |
| `persistence.sort` / `page` / `mapping` | `Sorts`、`PageParams`、`ResultMapper` |
| `persistence.convert` | `EnumValue`、`EnumValueConverter`、`EnumValues` |
| `scope` / `util` / `id` | `JpaPlusScope`、`TreeUtils`、`SnowflakeIdGenerator` |
| `exception` | `JpaPlusException` |

### autoconfigure

| 类型 | 说明 |
| --- | --- |
| `JpaPlusProperties` | 全部 `neopen.jpa.*` 配置项的绑定契约 |
| `JpaPlusRoutingDataSource` | 自定义路由时的扩展点 |

### convert

| 包 | 类型 |
| --- | --- |
| `annotation` | `DateStrFormat`、`DateStrValid` |
| `converter` | `BaseVoService`、`BasePredicateService`、`BaseServiceFactory`、`AbstractBaseService`、`BaseVoCommom`、`ThrowingConsumer` |
| `mapping` | `ParamField`、`ParamIgnore`、`ParamMapper` |

### extension

| 包 | 类型 |
| --- | --- |
| `annotation` | `Desensitize`、`Idempotent`、`RateLimit`、`RetryOnOptimisticLock`、`SlowLog`、`SysControllerLog`、`SysServiceLog` |
| `desensitize` | `Desensitizes`、`DesensitizeType`、`DesensitizeModule` |
| `response` | `Json`、`JsonPageConverter`、`JsonPageResponseFactory`、`DefaultJsonPageResponseFactory` |
| `exception` | `RateLimitException` |
| `cache` | `LRUCache` |

## 扩展点与内部类型

| 类型 | 级别 | 原因 |
| --- | --- | --- |
| `TenantListener`、`AuditListener`、`AuditTrailListener`、`EncryptedFieldConverter`、`DeterministicEncryptedFieldConverter` | 扩展点 | 需在实体上通过 `@EntityListeners` / `@Convert` 引用，不能改为非 public |
| `JpaPlusAutoConfiguration`、`JpaPlusDataSourceAutoConfiguration`、`ExtensionAutoConfiguration`、`JpaPlusMetricsAutoConfiguration` | 内部 | 仅由自动配置导入文件加载 |
| `DataSourceAspect`、`IgnoreFilterAspect` | 内部 | AOP 装配细节 |
| `SlowSqlHibernateCustomizer`、`JdbcBatchHibernateCustomizer`、`JpaPlusRuntimeConfigurer` | 内部 | Hibernate/运行时装配细节 |
| `JpaPlusMetrics`、`JpaPlusMetricsBeanPostProcessor`、`SlowSqlMetricsDataSource` | 内部 | 指标实现细节，用户只需开启配置 |
| `DateStrAspect`、`IdempotentAspect`、`RateLimitAspect`、`OptimisticLockRetryAspect`、`SlowLogAspect`、`SysLogAspect` | 内部 | 切面实现，用户通过注解使用 |
| `DesensitizeJsonSerializer`、`DesensitizeValueFilter` | 内部 | Jackson 序列化细节 |
| `IdempotentUtil`、`NetworkUtil`、`RateLimiterUtil`、`MyLruCache` | 内部 | 工具类，已改为私有构造器；`MyLruCache` 与 `LRUCache` 功能重复 |

## 发现的问题

| 编号 | 问题 | 影响 | 处理建议 |
| --- | --- | --- | --- |
| API-1 | `BaseVoCommom` 拼写错误（应为 `Common`） | 公开类型名带入 1.0 后难以修正 | 0.9 新增 `BaseVoCommon` 并弃用旧名，1.0 移除 |
| API-2 | `condition.QueryCondition` 与 `query.QueryCondition` 同名 | 通配导入时产生歧义 | 文档统一推荐 `condition` 包；0.9 评估合并，1.0 前给出结论 |
| API-3 | `MyLruCache` 与 `LRUCache` 并存 | 职责重叠、命名不一致 | 归为内部；0.9 合并为一个实现 |
| API-4 | `JpaPlusException` 语义未声明 | 难以判断捕获边界 | 在 javadoc 中固定为「实体元数据或类型转换失败」，参数错误仍用 `IllegalArgumentException` |
| API-5 | `Json` 名称过于通用 | 通配导入易冲突 | 保持现状，文档提示按需导入 |
| API-6 | autoconfigure 包内 public 实现类较多 | 用户可能误用为 API | 本次盘点已列为内部，1.0 冻结时不承诺兼容 |

## 0.8 已完成的一致性修正

**异常语义统一**

| 场景 | 异常类型 |
| --- | --- |
| 参数、配置非法 | `IllegalArgumentException` |
| 运行状态不满足（重复提交、令牌过期、无 Web 请求上下文、未处于忽略状态时恢复） | `IllegalStateException` |
| 触发限流 | `RateLimitException` |
| 实体元数据或类型转换失败 | `JpaPlusException` |

- `DateStrAspect` 的参数校验由 `InvalidPropertiesFormatException`（受检）改为 `IllegalArgumentException`
- `IdempotentAspect` 的重复提交/令牌过期/无请求上下文改为 `IllegalStateException`，幂等键为空改为 `IllegalArgumentException`，不再声明受检异常
- `SysLogAspect` 的限流由 `InvalidObjectException` 改为 `RateLimitException`；仅保留 `IOException`（解析客户端地址）

**配置校验**

- 新增 `neopen.jpa.audit.zone` 绑定期校验：非空且必须是合法时区编号
- 已有校验：`batch-size`、`slow-sql.threshold-ms`、`jdbc-batch.batch-size`、`metrics.slow-threshold-ms` 必须大于 0

**javadoc 严格模式**

- `core`、`autoconfigure`、`convert`、`extension` 在 `doclint=all` + `failOnWarnings=true` 下通过
- 补齐隐式默认构造器的注释；静态工具类改为私有构造器，避免误实例化

## 1.0 冻结规则草案

1. 稳定与扩展点类型只做新增，不做删除或签名修改；必须变更时先标记 `@Deprecated(forRemoval = true)` 并保留至少一个次版本。
2. 所有公开 API 标注 `@since`，内部类型在 javadoc 中显式写明「内部实现，不承诺兼容」。
3. 破坏性变更只允许出现在次版本（0.x → 0.y）中，并在 CHANGELOG 的「不兼容变更」段落列明迁移方式。
4. 1.0 发布时以本文档的稳定清单作为冻结基线，`autoconfigure` 内部类型不纳入兼容承诺。
