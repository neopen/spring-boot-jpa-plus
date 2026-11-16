# 更新日志 (CHANGELOG)

本项目的所有重要变更都将记录在此文件中。

格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 规范，
版本号遵循 [语义化版本 2.0.0](https://semver.org/lang/zh-CN/)。

------



## 0.7.0 (开发中)

0.7.0 在 0.6.0 的数据隔离与可观测能力上继续补齐**过滤控制、查询效率与运维可观测性**，模块结构与 Maven 坐标保持不变。

### 依赖坐标

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>0.7.0-SNAPSHOT</version>
</dependency>
```

### 版本亮点

- **过滤可控**：`@IgnoreTenant` / `@IgnoreDataScope` 与代码级跳过，支持嵌套恢复与异常安全。
- **规则更灵活**：`DataRule` 支持 `OR` 组合与 `dept.parentId` 形式的关联路径。
- **大数据量更省内存**：Specification、HQL、原生 SQL 执行器新增流式查询。
- **写入更快**：`upsertAll` 一次性查询全部业务键，并可自动开启 Hibernate JDBC 批量。
- **可观测性**：Micrometer 统计数据源路由次数与慢 SQL 次数。
- **审计不阻塞**：`AsyncAuditLogPublisher` 有界队列 + 丢弃策略。

### 新增能力

#### 1. 过滤跳过

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 方法级跳过 | `@IgnoreTenant`、`@IgnoreDataScope` | 方法或类级别生效，嵌套调用按层恢复 |
| 代码跳过 | `Tenants#ignore` / `restore` / `runIgnored` / `callIgnored`、`DataScopes` 同名方法 | 无注解场景在线程内临时跳过 |

#### 2. 数据权限增强

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| `OR` 组合 | `DataRule.or(...)`、`DataRule.Logic` | 规则按声明顺序从左到右组合，默认 `AND` |
| 关联路径 | `DataRule.of("dept.parentId", ...)` | 字段名支持 `a.b` 逐级关联 |

#### 3. 查询与批量

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 流式查询 | `JpaPlusSpecExecutor#stream`、`JpaPlusHqlExecutor#stream`、`JpaPlusSqlExecutor#stream` | 基于 `getResultStream`，事务内消费 |
| upsert 优化 | `BatchOperation#upsertAll` | 一次查询全部业务键，每批 `flush`，结束后统一 `clear` |
| JDBC 批量 | `JdbcBatchHibernateCustomizer`、`neopen.jpa.jdbc-batch.*` | 写入 `hibernate.jdbc.batch_size`、`order_inserts`、`order_updates` |

#### 4. 可观测性与审计

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 指标监控 | `JpaPlusMetrics`、`neopen.jpa.metrics.*` | 数据源路由次数与慢 SQL 次数（Micrometer `Counter`） |
| 异步审计 | `AsyncAuditLogPublisher` | 有界队列、满时丢弃并告警、关闭时排空 |

### 新增配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.jdbc-batch.enabled` | `false` | 是否写入 Hibernate JDBC 批量参数 |
| `neopen.jpa.jdbc-batch.batch-size` | `50` | 对应 `hibernate.jdbc.batch_size` |
| `neopen.jpa.jdbc-batch.order-inserts` | `true` | 对应 `hibernate.order_inserts` |
| `neopen.jpa.jdbc-batch.order-updates` | `true` | 对应 `hibernate.order_updates` |
| `neopen.jpa.metrics.enabled` | `false` | 是否注册指标采集 |
| `neopen.jpa.metrics.slow-threshold-ms` | `1000` | 慢 SQL 判定阈值 |

指标仅在引入 `micrometer-core` 且开启后生效，未引入时自动配置不加载。

### 兼容性

- 过滤跳过默认不生效，未使用注解或跳过方法的项目行为不变。
- `DataRule` 改为 `record(field, values, logic)`，原有两参构造器与 `DataRule.of(...)` 保持兼容，默认 `AND`。
- `upsertAll` 不再在每个批次后 `clear()`，传入实体在方法返回后均为游离状态。
- 版本号调整为 `0.7.0-SNAPSHOT`，模块结构与 Maven 坐标保持不变。

------

## 0.6.0 (开发中)

0.6.0 聚焦**数据隔离、变更可追溯与多库部署**，在保持模块结构与 Maven 坐标不变的前提下新增多项企业级能力。

### 依赖坐标

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>0.6.0-SNAPSHOT</version>
</dependency>
```

### 版本亮点

- **数据隔离开箱可用**：`@TenantId` 写入自动填充、查询自动过滤，支持作用域固定租户。
- **行级数据权限**：`DataPermissionProvider` + `DataRule` 统一转换为 `字段 IN (...)` 条件。
- **变更可追溯**：`@Audited` + `AuditLogPublisher` 输出字段级前后快照。
- **多库部署**：`@DataSource` 注解与 `DataSourceContext` 支持读写分离与按业务选库。
- **逻辑删除可恢复**：新增主键批量恢复。
- **自然键批量 upsert**：无需主键即可按业务键新增或更新。

### 新增能力

#### 1. 多租户

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 租户字段 | `@TenantId` | 支持 `String`、`Long`、`Integer`、`UUID` |
| 写入填充 | `TenantListener` | `@PrePersist` / `@PreUpdate` 自动写入当前租户 |
| 查询过滤 | `Tenants#filter`、`JpaPlusSpecExecutor` | 在逻辑删除之后自动追加租户条件 |
| 租户来源 | `TenantProvider`、`neopen.jpa.scopes.<name>.tenant` | 全局提供者与作用域固定租户 |

#### 2. 行级数据权限

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 权限规则 | `DataRule`、`DataPermissionProvider` | 规则转换为 `字段 IN (...)` |
| 查询过滤 | `DataScopes#filter`、`JpaPlusSpecExecutor` | 在租户条件之后追加，三层条件按 `AND` 合并 |
| 作用域隔离 | `DataScopes#setProvider`、`DataScopes#setEnabled` | 按线程作用域启用或替换规则 |

#### 3. 变更历史

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 记录开关 | `@Audited`、`AuditTrailListener` | 生命周期回调采集快照 |
| 变更模型 | `AuditEntry`、`AuditChange`、`AuditAction` | `CREATE`、`UPDATE`、`DELETE` 三类记录 |
| 发布 | `AuditLogPublisher`、`AuditTrails` | 全局与作用域发布者，异常仅记录日志 |
| 操作人 | `Audits#currentAuditor` | 与审计字段共用 `AuditorProvider` |

#### 4. 动态数据源

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 路由数据源 | `JpaPlusRoutingDataSource`、`neopen.jpa.datasource.*` | 按逻辑名称解析数据源 Bean，接管默认数据源 |
| 注解切换 | `@DataSource` | 方法或类级别选择目标，支持 `readOnly` 读写分离 |
| 手动切换 | `DataSourceContext` | 线程上下文栈，支持嵌套恢复 |

#### 5. 逻辑删除与批量增强

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 逻辑删除恢复 | `LogicDeletes#restoreById`、`restoreAllById`、`restoreAll` | 主键批量恢复或清除托管实体标记 |
| 批量 upsert | `BatchOperation#upsertAll`、`UpsertResult` | 按业务键分批查询，存在更新、不存在新增 |

### 新增配置项

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.tenant.enabled` | `true` | 多租户过滤总开关 |
| `neopen.jpa.data-scope.enabled` | `true` | 行级数据权限总开关 |
| `neopen.jpa.audit.trail-enabled` | `true` | 变更历史总开关 |
| `neopen.jpa.datasource.enabled` | `false` | 动态数据源开关 |
| `neopen.jpa.datasource.primary` | `primary` | 默认数据源逻辑名称 |
| `neopen.jpa.datasource.read-target` | `read` | 只读目标名称 |
| `neopen.jpa.datasource.write-target` | `write` | 写入目标名称 |
| `neopen.jpa.datasource.targets` | 空 | 逻辑名称到 Bean 名称的映射 |
| `neopen.jpa.scopes.<name>.tenant-enabled` | 空 | 作用域租户开关 |
| `neopen.jpa.scopes.<name>.tenant` | 空 | 作用域固定租户 |
| `neopen.jpa.scopes.<name>.trail-enabled` | 空 | 作用域变更历史开关 |

### 兼容性

- 未使用新注解与提供者的项目行为不变。
- 动态数据源仅在 `enabled=true` 且容器中没有 `DataSource` 时生效。
- 版本号调整为 `0.6.0-SNAPSHOT`，模块结构与 Maven 坐标保持不变。

------

## 0.5.0 (2026-11-15)

0.5.0 是一次较大的能力扩充，围绕**数据安全、查询表达力、多数据源治理与可观测性**补齐了多项长期需求，同时保持模块结构与 Maven 坐标不变。

### 依赖坐标

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-starter</artifactId>
    <version>0.5.0</version>
</dependency>
```

扩展组件按需单独引入：

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-extension</artifactId>
    <version>0.5.0</version>
</dependency>
```

### 版本亮点

- **字段加密可上线**：支持多密钥轮换、确定性加密（可等值查询）与按数据源隔离密钥。
- **查询构建器补全**：HQL / SQL 支持 `select`、`group by`、`having`、参数化 `exists` 与 `union`。
- **多数据源开关隔离**：逻辑删除、审计、加密与批量大小可按线程作用域独立配置。
- **数据脱敏双栈支持**：fastjson2 之外新增 Jackson 序列化脱敏。
- **可观测性**：新增慢 SQL 日志与 `AuditorProvider` 操作人 SPI。

### 新增能力

#### 1. 数据安全

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 字段加密 | `@EncryptedField`、`EncryptedFieldConverter` | AES/GCM/NoPadding，随机 12 字节 IV，密文 `Base64(IV + 密文 + 认证标签)` |
| 密钥轮换 | `EncryptedFields#setKeys`、`neopen.jpa.encryption.keys` | 新数据带密钥编号前缀，历史密文自动回退解密 |
| 确定性加密 | `DeterministicEncryptedFieldConverter`、`EncryptedFields#encryptDeterministic` | 相同明文密文一致，可按密文等值查询 |
| 数据脱敏 | `@Desensitize`、`Desensitizes`、`DesensitizeValueFilter`、`DesensitizeModule` | 姓名、手机号、身份证、银行卡、邮箱、地址、密码；支持 fastjson2 与 Jackson |
| 枚举持久化值 | `@EnumValue`、`EnumValues`、`EnumValueConverter` | 枚举以显式业务值入库，支持常量注解与无参方法两种声明 |

配置示例：

```yaml
neopen:
  jpa:
    encryption:
      enabled: true
      keys:
        v1: old-secret
        v2: new-secret
      active-key-id: v2
```

> 注意：JPQL / Criteria 查询会再次应用字段转换器，传入密文会被二次加密。Spring Data 派生查询请直接传明文，或改用原生 SQL 比较密文列。

#### 2. 审计与数据治理

- **审计字段**：`@CreatedAt` / `@UpdatedAt` / `@CreatedBy` / `@UpdatedBy` 由实体监听器自动维护。
- **操作人 SPI**：新增 `AuditorProvider`，容器注册该 Bean 后优先级高于 `neopen.jpa.audit.auditor`。
- **逻辑删除写侧**：`LogicDeletes.deleteById` / `deleteAllById` / `deleteAll` 补齐更新与删除能力。
- **多数据源作用域**：新增 `JpaPlusScope`，按线程绑定作用域，隔离逻辑删除、审计、加密与批量大小开关。

```java
JpaPlusScope.run("archive-view", () -> {
    // 该作用域关闭了逻辑删除过滤，其它线程不受影响
    archiveRepository.findAll(spec);
});
```

```yaml
neopen:
  jpa:
    scopes:
      archive-view:
        logic-delete-enabled: false
        audit-enabled: false
        encryption-key: secondary-secret
```

#### 3. 查询能力

| 能力 | 入口 | 说明 |
| --- | --- | --- |
| 分组查询 | `HqlWrapper#select/groupBy/having`、`SqlWrapper#having` | 聚合列、分组与分组过滤；SQL 统计按子查询包装 |
| union 合并 | `SqlWrapper#union/unionAll` | 合并结构一致的多段查询，参数统一重编号 |
| 参数化子查询 | `HqlWrapper#exists(subQuery, params)`、`SqlWrapper#exists(subQuery, params)` | 子查询命名参数自动重命名并绑定 |
| 原生片段与映射 | `raw`、`exists`、`notExists`、`alias` | 静态片段与查询参数名映射 |
| 请求分页 | `PageParams` | 校验页码、每页上限（默认 500）与排序字段后转 `Pageable` |
| 存在性判断 | `JpaPlusSpecExecutor#exists` | 基于计数判断，不加载实体 |
| DTO 投影 | `ResultMapper`、`queryForDtoList/Page/Object` | 按别名匹配 DTO 字段，忽略大小写与下划线 |
| 参数转实体 | `ParamMapper`、`@ParamField`、`@ParamIgnore` | 仅覆盖非空字段，支持新增与局部更新 |

#### 4. 性能与可靠性

- **乐观锁重试**：`@RetryOnOptimisticLock`，最高优先级执行，每次重试开启新事务。
- **慢方法日志**：`@SlowLog`，超过阈值输出 WARN。
- **慢 SQL 日志**：`neopen.jpa.slow-sql.enabled` / `threshold-ms`，基于 Hibernate 内置阈值，无额外计时开销。
- **批量增强**：`BatchOperation` 支持批量保存、更新、删除与分段回调。

#### 5. 基础组件

- **树构建**：`TreeUtils`，检测重复 ID、自引用与环。
- **雪花 ID**：`SnowflakeIdGenerator`，41 + 5 + 5 + 12 位结构，时钟回拨快速失败。

### 兼容性

| 项 | 0.4.0 | 0.5.0 |
| --- | --- | --- |
| JDK | 17+ | 17+ |
| Spring Boot | 3.x | 3.x（构建验证 3.4.13） |
| Hibernate | 6.x | 6.x |
| 持久化 API | `jakarta.persistence` | `jakarta.persistence` |
| Maven | 3.x | 3.x |
| 模块结构 | core / convert / autoconfigure / starter / extension | 不变 |
| Maven 坐标 | `io.github.neopen:*` | 不变 |

### 升级指南

1. **升级依赖版本**：将 starter 与 extension 版本改为 `0.5.0`。
2. **移除 `BaseVo` 继承**：0.5.0 移除了视图对象的基类约束，原先继承 `BaseVo` 的 VO 需去掉该父类。
3. **配置加密密钥**：使用 `@EncryptedField` 时必须配置 `neopen.jpa.encryption.key`（或 `keys`），否则读写会抛 `JpaPlusException`，避免明文入库。
4. **按需开启慢 SQL**：`neopen.jpa.slow-sql.enabled` 默认关闭，开启后把 `org.hibernate.SQL_SLOW` 日志级别设为 `WARN`。
5. **多数据源按需配置**：`neopen.jpa.scopes.<name>` 未配置时沿用全局设置，升级本身不会改变现有行为。
6. **Jackson 脱敏可选**：扩展模块把 `jackson-databind` 作为可选依赖，未引入 Jackson 时不会注册 `DesensitizeModule`。

### 破坏性变更

- 移除 `BaseVo` 基类约束，视图对象可以是任意 POJO。
- 未配置加密密钥时，加密字段读写会抛出异常（此前无此约束）。
- `SqlWrapper` 的统计语句统一按子查询包装，包含 `group by`、`having` 或聚合列时都能正确计数。

### 已知限制

- 字段加密只适用于字符串字段，数值与日期需先转为字符串。
- 确定性加密会泄露明文相等关系与出现频率，只建议用于必须等值检索的字段。
- `union` 不支持嵌套，子查询自身不能包含 `order by`，排序需写在外层。
- `JpaPlusScope` 为线程绑定，不自动跟随数据源路由，需要在路由切面或任务入口显式绑定。
- 慢 SQL 日志只输出 SQL 原文，不包含绑定参数。
- `@RateLimit` 基于本地 Guava `RateLimiter`，多实例部署需改用分布式限流。

### 文档与示例

- 文档站：<https://jpa.helpenx.com>
- 版本发布页：<https://jpa.helpenx.com/version>
- 示例工程：`examples/spring-boot-jpa-plus-demo`
  - `/feature/page`：`PageParams` 分页
  - `/feature/union`：union 合并查询
  - `/feature/scope`：数据源作用域
  - `/feature/secret`：加密字段写入与确定性查询
  - `AuditConfiguration`：`AuditorProvider` 接入请求上下文

示例模块测试按 `@Tag` 分为三条路线：

```bash
cd examples/spring-boot-jpa-plus-demo

mvn test                       # 全部
mvn test -Punit-test           # 纯单元测试
mvn test -Pintegration-test    # Spring + H2 集成测试
mvn test -Papi-test            # MockMvc 接口测试
```

### 验证情况

- 核心模块、自动配置与扩展模块单元测试全部通过。
- 示例工程 36 个用例通过，覆盖 JPA / HQL / 原生 SQL 三套实现与新能力示例。
- 文档站中英文构建通过。

### 反馈

- 问题反馈：<https://github.com/neopen/spring-boot-jpa-plus/issues>
- 功能建议与 PR 欢迎提交。
