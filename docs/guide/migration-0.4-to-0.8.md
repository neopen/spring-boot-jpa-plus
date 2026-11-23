# 从 0.4 迁移到 0.8

0.8 是 0.4 之后第一个功能完整的发布版本，包含 0.5、0.6、0.7 累积的能力，以及游标分页、流式分批与跨线程上下文传播。本文给出升级步骤、新能力启用方式、行为变化与回滚方案。

## 前置条件

| 项 | 要求 |
| --- | --- |
| JDK | 17 及以上 |
| Spring Boot | 3.x（推荐 3.2 及以上） |
| JPA 实现 | Hibernate 6.x |
| 命名空间 | `jakarta.persistence.*`（不能混用 `javax.persistence.*`） |

如果项目仍停留在 Spring Boot 2.x / `javax` 命名空间，需要先完成框架升级，再按本文迁移。

## 升级步骤

### 1. 升级依赖

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>0.8.0</version>
</dependency>
```

使用扩展组件时同步升级：

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-extension</artifactId>
    <version>0.8.0</version>
</dependency>
```

- 移除对 0.4.x 的版本锁定（`dependencyManagement` 或父 POM 中的固定版本）。
- 若单独引用了 `-core`、`-autoconfigure`、`-convert`，保持与 starter 同版本。

### 2. 检查配置键

0.8 只识别 `neopen.jpa.*` 与 `neopen.extension.*` 前缀。旧前缀的配置不会报错，但也不会生效，升级后请逐项核对：

- 逻辑删除开关：`neopen.jpa.logic-delete.enabled`
- 审计字段：`neopen.jpa.audit.*`
- 批量：`neopen.jpa.batch-size`
- 加密：`neopen.jpa.encryption.*`
- 慢 SQL：`neopen.jpa.slow-sql.*`
- 指标：`neopen.jpa.metrics.*`
- 上下文传播：`neopen.jpa.context-propagation.enabled`

完整列表见[配置参考](/reference/configuration)。

### 3. 启动并回归

1. 启动应用，确认没有 `ClassNotFoundException` / `NoSuchMethodError`，说明旧版本依赖已被完全替换。
2. 打开 SQL 日志，对比升级前后的查询语句：0.4 已使用的逻辑删除、条件组、`Specification`、`HqlWrapper`、`SqlWrapper` 行为保持一致。
3. 运行核心业务流程与批量任务，确认事务边界与返回结果未变化。

### 4. 按需启用新能力

新能力全部是**显式启用**：不添加注解、不注册 Provider、不开启配置时，行为与 0.4 一致。

| 能力 | 启用方式 | 文档 |
| --- | --- | --- |
| 多租户 | `@TenantId` + `@EntityListeners(TenantListener.class)` + `TenantProvider` | [多租户](/core/tenant) |
| 行级数据权限 | 注册 `DataPermissionProvider` | [数据权限](/core/data-permission) |
| 变更历史 | `@Audited` + `@EntityListeners(AuditTrailListener.class)` + `AuditLogPublisher` | [变更历史](/core/audit-trail) |
| 动态数据源 | `neopen.jpa.datasource.enabled=true` + `targets` | [动态数据源](/core/datasource) |
| 逻辑删除恢复 | `LogicDeletes.restore(...)` | [逻辑删除](/core/logic-delete) |
| 批量 upsert | `BatchOperation.upsertAll(...)` | [批量操作](/core/batch) |
| Hibernate JDBC 批量 | `neopen.jpa.jdbc-batch.enabled=true` | [批量操作](/core/batch) |
| 流式查询 | `stream(...)` 系列方法 | [执行器](/core/executor) |
| 临时跳过过滤 | `@IgnoreTenant` / `@IgnoreDataScope` | [过滤跳过](/core/filter-ignore) |
| 指标监控 | 引入 Micrometer + `neopen.jpa.metrics.enabled=true` | [指标监控](/core/metrics) |
| 异步审计发布 | `AsyncAuditLogPublisher` | [变更历史](/core/audit-trail) |
| PostgreSQL JSONB | 自定义类型映射 | [PostgreSQL JSONB](/core/jsonb) |
| 游标分页 | `JpaPlusSpecExecutor#keyset(...)` + `KeysetPageParams` | [游标分页](/core/keyset-pagination) |
| 流式分批 | `forEachBatch(int, Consumer)`（Spec / HQL / SQL 执行器） | [执行器](/core/executor) |
| 跨线程上下文传播 | 默认开启；可用 `neopen.jpa.context-propagation.enabled=false` 关闭 | [上下文传播](/core/context-propagation) |

## 行为与注意事项

### 过滤链顺序

`JpaPlusSpecExecutor` 按固定顺序追加条件：**逻辑删除 → 多租户 → 行级权限**。三层都通过 `Specification.and(...)` 组合，任一层关闭或没有条件时不影响其他层。

### 批量 upsert 语义

`upsertAll` 会先一次性查询全部业务键，再按批 `persist` / 更新，每批 `flush`，全部完成后统一 `clear`：

- 调用结束后所有输入实体处于游离态，不要继续依赖其延迟加载。
- 业务键必须唯一，同批重复键会匹配同一行。

### 两个「批次大小」不要混淆

| 配置 | 作用 |
| --- | --- |
| `neopen.jpa.batch-size` | `BatchOperation` 每批 flush / clear 的行数，默认 500 |
| `hibernate.jdbc.batch_size` | JDBC 驱动层合并的语句数，由 `neopen.jpa.jdbc-batch.batch-size` 写入，默认 50 |

### 指标为空

`neopen.jpa.metrics.enabled=true` 但类路径没有 Micrometer 时，指标自动配置不会加载，也不会有指标数据。确认已引入 `micrometer-core` 或 Actuator。

### 流式查询必须在事务内消费

`stream(...)` 返回的流绑定当前持久化上下文，需在事务中通过 try-with-resources 使用，离开事务后再消费会失败。

`forEachBatch(...)` 同样需要在事务内调用：回调内按批 `flush` / `clear`，但不会提交事务；提前退出请抛出异常，流会在关闭时释放。

### 上下文传播默认开启

0.8 默认注册 `JpaPlusContextTaskDecorator`，提交到线程池的任务会自动继承作用域与过滤开关。容器中已有自定义 `TaskDecorator` 时不会被覆盖；需要完全关闭时设置 `neopen.jpa.context-propagation.enabled=false`。任务结束后 ThreadLocal 会恢复为提交线程的状态，不会泄漏到池中其他任务。

## 验证清单

- [ ] 依赖树中只有 0.8.0 一个版本（`mvn dependency:tree | findstr jpa-plus`）
- [ ] 应用启动无自动配置报错
- [ ] 核心查询 SQL 与升级前一致
- [ ] 批量任务、定时任务执行结果一致
- [ ] 异步任务中的过滤结果与提交线程一致
- [ ] 启用的新能力逐项按文档冒烟

## 回滚

- 将 starter / extension 版本降回 0.4.x 即可恢复原行为。
- 若已使用 0.5–0.8 的新注解或 API，回滚前必须一并移除相关注解、配置与代码，否则会因类不存在而启动失败。
- 新能力大多不改变表结构；多租户、逻辑删除新增的列需要自行保留（对 0.4 无副作用）。

## 常见问题

**升级后查询结果变少了？**

检查是否误加了 `@TenantId`，或注册了返回空集合以外的 `DataPermissionProvider`。`DataScopes` 返回空集合表示「不限制」，与「无权限」不同。

**变更历史没有记录？**

`@Audited` 与 `@EntityListeners(AuditTrailListener.class)` 缺一不可，且必须注册 `AuditLogPublisher`。发布异常只记录 WARNING，不会中断业务。

**过滤没有生效？**

确认查询走的是 `JpaPlusSpecExecutor` 或 `SpecWrapper`；`HqlWrapper`、`SqlWrapper` 与原生 SQL 不会自动追加过滤条件。
