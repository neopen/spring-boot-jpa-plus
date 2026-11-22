# 版本发布

::: warning 特别说明
- 当前正式发布版本为 **0.4.0**，可以直接通过 Maven 依赖引入。
- `main` 分支保留早期开发线，尚未发布；0.5 起的能力将随 **0.8.0** 一并交付。
- `dev` 分支对应 **0.8.0**，是 0.7.0 之后的发布准备版本，在 0.7.0 基础上新增游标分页、流式分批与跨线程上下文传播，侧重兼容性、API 审查与发布验证。
- 本站文档按 **0.8.0** 编写，描述的是 `dev` 分支的最新行为。
- 因此，通过 Maven 引入的 **0.4.0 不支持**本文档中的 0.5 / 0.6 / 0.7 / 0.8 能力。
:::

## 版本总览

| 版本 | 状态 | 日期 | 说明 |
| --- | --- | --- | --- |
| `0.8.0` | 开发中 | — | 发布准备：游标分页、流式分批、跨线程上下文传播，以及兼容性矩阵、API 审查与性能基线 |
| `0.7.0` | 开发中 | — | 过滤跳过、规则 OR 组合与关联路径、流式查询、JDBC 批量、Micrometer 指标、异步审计发布 |
| `0.6.0` | 开发中 | — | 多租户、行级数据权限、变更历史、动态数据源与读写分离、逻辑删除恢复、批量 upsert |
| `0.5.0` | 开发中 | — | 新增枚举转换、字段加密、脱敏、重试、慢日志、树与雪花 ID 等能力 |
| `0.4.0` | 已发布 | 2026-11-13 | 逻辑删除、条件分组、Specification 执行器，当前稳定版本 |
| `0.3.0` | 已发布 | 2026-11-13 | 注解式查询条件、安全排序、fetch join 分页 |
| `0.2.1` | 已发布 | 2026-11-12 | 示例与稳定性调整，无 API 变更 |
| `0.2.0` | 已发布 | 2026-11-12 | Specification 链式构建器 `SpecWrapper` |
| `0.1.2` | 已发布 | 2026-11-12 | 原生 SQL 链式封装与连表示例 |
| `0.1.1` | 已发布 | 2026-11-11 | HQL 链式查询与示例双实现 |
| `0.1.0` | 已发布 | 2026-11-05 | 首个版本：模块骨架、DAO、查询执行器、转换与扩展 |

## 兼容性基线

| 项 | 0.4.0 | 0.5.0 / 0.6.0 / 0.7.0 / 0.8.0 |
| --- | --- | --- |
| JDK | 17+ | 17+ |
| Spring Boot | 3.x | 3.x |
| Hibernate | 6.x | 6.x |
| 持久化 API | `jakarta.persistence` | `jakarta.persistence` |
| Maven | 3.x | 3.x |
| 模块结构 | core / convert / autoconfigure / starter / extension | 与 0.4.0 相同 |
| 坐标 | `io.github.neopen:*` | 与 0.4.0 相同 |

`starter` 只传递 `core`、`convert`、`autoconfigure`；`extension` 需要单独引入，各版本保持一致。

## 0.8.0（开发中）

0.8.0 在 0.7.0 基础上补齐查询分页、流式消费与上下文一致性，并完成发布前的质量收尾，模块结构与 Maven 坐标保持不变。

### 新增：分页与流式

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 游标分页 | `KeysetPageParams`、`KeysetCursor`、`KeysetPage`、`JpaPlusSpecExecutor#keyset` | 基于 `(排序键, 主键)` 的 keyset 查询，自动追加主键 tiebreaker，禁止忽略大小写与空值排序列 |
| 流式分批 | `JpaPlusSpecExecutor#forEachBatch`、`JpaPlusHqlExecutor#forEachBatch`、`JpaPlusSqlExecutor#forEachBatch` | 按批聚合流式结果并在每批回调后 `flush` / `clear`，异常或提前退出时关闭流 |

### 新增：上下文传播

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 上下文快照 | `JpaPlusContext` | 作用域、数据权限与租户过滤跳过深度、数据源路由栈 |
| 任务装饰器 | `JpaPlusContextTaskDecorator` | 提交时捕获、结束时恢复，支持嵌套与异常退出 |
| 配置开关 | `neopen.jpa.context-propagation.enabled` | 默认开启；容器已有自定义 `TaskDecorator` 时不覆盖 |

### 质量与文档

- 覆盖率门槛接入 `verify`：core 行覆盖率不低于 70%，autoconfigure 不低于 72%。
- 新增 `MetricsOverheadBenchmark`，中英文性能文档补充指标与日志开销数据。
- 新增中英文 FAQ，覆盖过滤链顺序、事务边界、跳过过滤、异步传播与流式关闭。
- 新增公开类型已登记 `API-REVIEW.md`。

## 0.7.0（开发中）

### 新增：过滤控制

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 方法级跳过 | `@IgnoreTenant`、`@IgnoreDataScope` | 按方法或类临时跳过租户与数据权限条件，支持嵌套与异常恢复 |
| 代码跳过 | `Tenants#runIgnored` / `callIgnored`、`DataScopes#runIgnored` / `callIgnored` | 无注解场景在线程内临时跳过过滤 |

### 新增：数据权限增强

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| OR 组合 | `DataRule.or(...)`、`DataRule.Logic` | 规则按声明顺序从左到右组合，支持 `AND` 与 `OR` |
| 关联路径 | `DataRule.of("dept.parentId", ...)` | 字段名支持 `a.b` 形式的关联路径 |

### 新增：查询与批量

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 流式查询 | `JpaPlusSpecExecutor#stream`、`JpaPlusHqlExecutor#stream`、`JpaPlusSqlExecutor#stream` | 基于 `getResultStream` 逐条消费，适合大数据量导出 |
| upsert 优化 | `BatchOperation#upsertAll` | 一次查询全部业务键，再分批写入并统一清理 |
| JDBC 批量 | `neopen.jpa.jdbc-batch.*` | 自动写入 `hibernate.jdbc.batch_size`、`hibernate.order_inserts`、`hibernate.order_updates` |

### 新增：可观测性与审计

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 指标监控 | `JpaPlusMetrics`、`neopen.jpa.metrics.*` | 通过 Micrometer 统计数据源路由次数与慢 SQL 次数 |
| 异步审计 | `AsyncAuditLogPublisher` | 有界队列 + 丢弃策略 + 关闭时排空，避免阻塞业务事务 |

### 新增：配置项

- `neopen.jpa.jdbc-batch.enabled`（默认 `false`）、`batch-size`（默认 `50`）、`order-inserts` / `order-updates`（默认 `true`）。
- `neopen.jpa.metrics.enabled`（默认 `false`）、`slow-threshold-ms`（默认 `1000`）。

### 变更与迁移

- 过滤跳过默认不生效，只有标注注解或显式调用跳过方法时才有影响。
- `DataRule` 改为 `record(field, values, logic)`；`new DataRule(field, values)` 与 `DataRule.of(...)` 保持兼容，默认 `AND`，需要 `OR` 时使用 `DataRule.or(...)`。
- `BatchOperation#upsertAll` 改为一次性查询全部业务键，批次中间只 `flush()`，全部处理完成后统一 `clear()`。
- 指标相关能力仅在引入 `micrometer-core` 且 `neopen.jpa.metrics.enabled=true` 时生效；慢 SQL 统计在 JDBC 层包装 `DataSource`，对 SQL 写法无侵入。

### 文档

- 新增 PostgreSQL JSONB 字段映射与查询示例，详见 [PostgreSQL JSONB](/core/jsonb)。

## 0.6.0（开发中）

### 新增：多租户与数据权限

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 多租户 | `@TenantId`、`TenantProvider`、`TenantListener`、`Tenants` | 写入自动填充租户字段，查询自动追加租户条件，支持作用域固定租户 |
| 行级数据权限 | `DataPermissionProvider`、`DataRule`、`DataScopes` | 规则转换为 `字段 IN (...)` 条件，与逻辑删除、租户条件按 `AND` 合并 |

### 新增：变更历史

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 变更历史 | `@Audited`、`AuditTrailListener`、`AuditTrails`、`AuditLogPublisher` | 字段级前后快照，输出 `CREATE` / `UPDATE` / `DELETE` 记录 |
| 记录模型 | `AuditEntry`、`AuditChange`、`AuditAction` | 实体、主键、变更明细、操作人与发生时间 |

### 新增：动态数据源

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 路由数据源 | `neopen.jpa.datasource.*`、`JpaPlusRoutingDataSource` | 按逻辑名称映射到容器中的数据源 Bean，接管 Spring Boot 默认数据源 |
| 注解切换 | `@DataSource` | 按方法或类选择目标数据源，支持 `readOnly` 读写分离 |
| 手动切换 | `DataSourceContext` | 线程上下文栈，支持 `run` / `call` 与嵌套恢复 |

### 新增：逻辑删除与批量增强

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 逻辑删除恢复 | `LogicDeletes#restoreById`、`restoreAllById`、`restoreAll` | 按主键批量恢复或对托管实体清除删除标记 |
| 批量 upsert | `BatchOperation#upsertAll`、`UpsertResult` | 按业务唯一键分批查询，存在则更新、不存在则新增 |

### 新增：配置项

- `neopen.jpa.tenant.enabled`（默认 `true`）。
- `neopen.jpa.data-scope.enabled`（默认 `true`）。
- `neopen.jpa.audit.trail-enabled`（默认 `true`）。
- `neopen.jpa.datasource.*`：`enabled`、`primary`、`read-target`、`write-target`、`targets`。
- `neopen.jpa.scopes.<name>` 新增 `tenant-enabled`、`tenant`、`trail-enabled`。

### 变更与迁移

- 数据源路由通过 `@AutoConfiguration(before = DataSourceAutoConfiguration.class)` 注册，仅在 `neopen.jpa.datasource.enabled=true` 且容器中没有 `DataSource` 时生效；已自定义数据源的项目不受影响。
- 租户与数据权限默认启用，但只有声明 `@TenantId` 或注册 `DataPermissionProvider` 时才产生条件，未使用相关能力的项目行为不变。
- 变更历史默认启用，但只有标注 `@Audited` 且声明 `AuditTrailListener` 的实体会产生记录。

## 0.5.0（开发中）

### 新增：字段转换与数据安全

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 枚举值转换 | `@EnumValue`、`EnumValues`、`EnumValueConverter` | 枚举持久化为显式业务值，支持常量注解与无参方法两种声明 |
| 字段加密 | `@EncryptedField`、`EncryptedFields`、`EncryptedFieldConverter` | AES/GCM 透明加解密，随机 IV，支持字符串密钥与 16/24/32 字节密钥 |
| 密钥轮换 | `EncryptedFields#setKeys`、`neopen.jpa.encryption.keys` | 多密钥配置，新数据带密钥编号前缀，历史密文继续可读 |
| 确定性加密 | `EncryptedFields#encryptDeterministic`、`DeterministicEncryptedFieldConverter` | IV 由明文派生，相同明文密文一致，支持按密文等值查询 |
| 数据脱敏 | `@Desensitize`、`Desensitizes`、`DesensitizeValueFilter` | 姓名、手机号、身份证、银行卡、邮箱、地址、密码脱敏，并提供 fastjson2 与 Jackson 两种序列化脱敏 |

### 新增：性能与可靠性

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 乐观锁重试 | `@RetryOnOptimisticLock` | 捕获乐观锁异常并按次数重试，默认 3 次、间隔 50ms |
| 慢方法日志 | `@SlowLog` | 超过阈值（默认 500ms）打印 WARN 日志 |
| 慢 SQL 日志 | `neopen.jpa.slow-sql` | 基于 Hibernate 内置阈值记录超过阈值的 SQL 与执行耗时 |
| 审计字段 | `@CreatedAt`、`@UpdatedAt`、`@CreatedBy`、`@UpdatedBy`、`Audits` | 由实体监听器自动维护创建与更新信息，支持 `AuditorProvider` 接入登录用户 |
| 逻辑删除写侧 | `LogicDeletes` | 在 0.4.0 查询过滤基础上补齐更新与删除 |
| 批量增强 | `BatchOperation` | 分批 flush / clear，支持批量保存、更新、删除 |

### 新增：查询能力补强

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 原生片段与子查询 | `HqlWrapper#raw`、`SqlWrapper#raw`、`exists`、`notExists` | 追加静态条件片段与 `exists` 子查询，并支持命名参数绑定 |
| 分组查询 | `HqlWrapper#select`、`groupBy`、`having`、`SqlWrapper#having` | 聚合列、分组与分组过滤，SQL 统计语句按子查询包装 |
| union 合并 | `SqlWrapper#union`、`unionAll` | 合并结构一致的多段查询，参数统一重编号，分页统计正确 |
| 请求分页 | `PageParams` | 校验页码、每页数量上限与排序字段后转换为 `Pageable` |
| 字段映射 | `HqlWrapper#alias`、`SqlWrapper#alias` | 查询参数名与实体属性/列名不一致时做映射 |
| 存在性判断 | `JpaPlusSpecExecutor#exists` | 基于计数判断是否存在，避免加载实体 |
| DTO 投影 | `ResultMapper` | 按别名匹配 DTO 字段，兼容驼峰与下划线 |
| 参数转实体 | `ParamMapper`、`@ParamField`、`@ParamIgnore` | 参数对象映射到实体，仅覆盖非空字段 |

### 新增：基础组件

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 树构建 | `TreeUtils` | 扁平列表按 id / parentId 组装为树，检测重复、自引用与环 |
| 雪花 ID | `SnowflakeIdGenerator` | 41 位时间戳 + 5 位数据中心 + 5 位机器 + 12 位序列，时钟回拨抛错 |

### 新增：扩展与配置

- 注解式限流 `@RateLimit` 与幂等能力增强。
- 扩展切面开关：`neopen.extension.retry.enabled`、`neopen.extension.slow-log.enabled`，默认开启。
- 加密配置：`neopen.jpa.encryption.enabled`（默认 `true`）、`neopen.jpa.encryption.key`。
- 密钥轮换配置：`neopen.jpa.encryption.keys`（密钥编号 → 密钥）、`neopen.jpa.encryption.active-key-id`。
- 慢 SQL 配置：`neopen.jpa.slow-sql.enabled`（默认 `false`）、`neopen.jpa.slow-sql.threshold-ms`（默认 `1000`）。
- 扩展模块引入 Jackson 时自动注册 `DesensitizeModule`，未引入 Jackson 不受影响。
- 审计可注册 `AuditorProvider` Bean 动态获取当前操作人，优先级高于 `neopen.jpa.audit.auditor`。
- 多数据源隔离：`JpaPlusScope` 按作用域绑定逻辑删除、审计、加密与批次大小开关，配置入口为 `neopen.jpa.scopes.<name>`。

### 变更与迁移

- 移除 `BaseVo` 基类约束，视图对象可以是任意 POJO；0.4.0 中继承 `BaseVo` 的 VO 需要去掉该父类。
- 未配置 `neopen.jpa.encryption.key` 时，`@EncryptedField` 字段读写会抛出 `JpaPlusException`，避免明文入库。
- 新增的切面默认启用，不需要时通过上述开关关闭。

## 0.4.0（当前发布版本）

### 功能范围

- **核心持久化**：`JpaPlusDao`、`AbstractJpaPlusDao` 提供 CRUD、分页与批量能力；`QueryWrapper` 提供链式动态条件。
- **查询构建**：`HqlWrapper`、`SqlWrapper` 链式构造 HQL 与原生 SQL，`JpaPlusHqlExecutor`、`JpaPlusSqlExecutor` 负责分页与计数。
- **Specification**：`SpecWrapper` 构造 `Specification`，`JpaPlusSpecExecutor` 处理计数、分页与 `countDistinct`，`FetchJoinPaging` 通过 count → 主键分页 → 主键加载避免连表重复行。
- **注解式条件**：`@QueryCondition` 支持操作符、字段别名、区间结束字段与 `group` 分组（组内 OR、组间 AND）。
- **安全排序**：`Sorts` 按实体字段或白名单校验排序表达式。
- **逻辑删除**：`@LogicDelete` + `LogicDeletes` 在 Criteria 查询中自动追加过滤条件。
- **转换**：`BaseVoService`、`AbstractBaseService`、`BaseVoCommom`、`BaseServiceFactory` 完成 Entity → VO 转换；`@DateStrFormat`、`@DateStrValid` 处理日期字符串。
- **扩展**：`@Idempotent` 幂等控制、`@SysControllerLog` / `@SysServiceLog` 操作日志、`JsonPageConverter` 统一分页响应、`RateLimiterUtil` 本地限流工具类。

### 限制

- 审计字段、DTO 投影、参数转实体映射、`@RateLimit` 注解尚未提供，这些能力从 0.5.0 开始提供。
- 逻辑删除只作用于查询；更新与删除的写侧处理在 0.5.0 补齐。
- 限流仅为工具类，需要业务自行调用。

## 0.3.0

- 新增 `@QueryCondition` 注解式查询条件映射。
- 新增 `Sorts` 安全排序。
- 新增 `FetchJoinPaging` fetch join 分页。

## 0.2.1

- 示例工程与稳定性调整，未新增或变更公共 API。

## 0.2.0

- 新增 `SpecWrapper`，用链式风格构造 JPA `Specification`。

## 0.1.2

- 新增 `SqlWrapper`、`JpaPlusSqlExecutor` 原生 SQL 链式查询与执行。
- 补充 JPA 连表查询示例。

## 0.1.1

- 新增 `HqlWrapper`、`JpaPlusHqlExecutor` HQL 链式查询。
- 示例项目提供 JPA 与 HQL 两套实现。

## 0.1.0

- 首个发布版本：`core`、`convert`、`autoconfigure`、`starter`、`extension` 模块骨架。
- 提供 `JpaPlusDao` / `AbstractJpaPlusDao`、`QueryWrapper`、HQL 与 SQL 执行器、`BatchOperation`。
- 提供 Entity → VO 转换服务，以及幂等、操作日志、统一分页响应扩展。

## 选择依赖版本

- **稳定使用**：依赖 0.4.0，并按 0.4.0 的 API 使用文档中已发布的能力。
- **提前体验 0.5 / 0.6 / 0.7 / 0.8**：克隆源码后执行 `mvn install -DskipTests` 安装到本地仓库，再将依赖版本改为本地构建版本。
- **等待正式发布**：0.5.0 起的能力会随 0.8.0 一并发布，本站文档与 Maven 依赖届时保持一致。
