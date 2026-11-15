# Spring Boot JPA Plus 0.5.0 发布说明

> 发布版本：`0.5.0`
> 发布日期：2026-11-15
> 版本类型：功能版本（包含一项破坏性变更）

0.5.0 是一次较大的能力扩充，围绕**数据安全、查询表达力、多数据源治理与可观测性**补齐了多项长期需求，同时保持模块结构与 Maven 坐标不变。

## 依赖坐标

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

## 版本亮点

- **字段加密可上线**：支持多密钥轮换、确定性加密（可等值查询）与按数据源隔离密钥。
- **查询构建器补全**：HQL / SQL 支持 `select`、`group by`、`having`、参数化 `exists` 与 `union`。
- **多数据源开关隔离**：逻辑删除、审计、加密与批量大小可按线程作用域独立配置。
- **数据脱敏双栈支持**：fastjson2 之外新增 Jackson 序列化脱敏。
- **可观测性**：新增慢 SQL 日志与 `AuditorProvider` 操作人 SPI。

## 新增能力

### 1. 数据安全

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

### 2. 审计与数据治理

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

### 3. 查询能力

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

### 4. 性能与可靠性

- **乐观锁重试**：`@RetryOnOptimisticLock`，最高优先级执行，每次重试开启新事务。
- **慢方法日志**：`@SlowLog`，超过阈值输出 WARN。
- **慢 SQL 日志**：`neopen.jpa.slow-sql.enabled` / `threshold-ms`，基于 Hibernate 内置阈值，无额外计时开销。
- **批量增强**：`BatchOperation` 支持批量保存、更新、删除与分段回调。

### 5. 基础组件

- **树构建**：`TreeUtils`，检测重复 ID、自引用与环。
- **雪花 ID**：`SnowflakeIdGenerator`，41 + 5 + 5 + 12 位结构，时钟回拨快速失败。

## 兼容性

| 项 | 0.4.0 | 0.5.0 |
| --- | --- | --- |
| JDK | 17+ | 17+ |
| Spring Boot | 3.x | 3.x（构建验证 3.4.13） |
| Hibernate | 6.x | 6.x |
| 持久化 API | `jakarta.persistence` | `jakarta.persistence` |
| Maven | 3.x | 3.x |
| 模块结构 | core / convert / autoconfigure / starter / extension | 不变 |
| Maven 坐标 | `io.github.neopen:*` | 不变 |

## 升级指南

1. **升级依赖版本**：将 starter 与 extension 版本改为 `0.5.0`。
2. **移除 `BaseVo` 继承**：0.5.0 移除了视图对象的基类约束，原先继承 `BaseVo` 的 VO 需去掉该父类。
3. **配置加密密钥**：使用 `@EncryptedField` 时必须配置 `neopen.jpa.encryption.key`（或 `keys`），否则读写会抛 `JpaPlusException`，避免明文入库。
4. **按需开启慢 SQL**：`neopen.jpa.slow-sql.enabled` 默认关闭，开启后把 `org.hibernate.SQL_SLOW` 日志级别设为 `WARN`。
5. **多数据源按需配置**：`neopen.jpa.scopes.<name>` 未配置时沿用全局设置，升级本身不会改变现有行为。
6. **Jackson 脱敏可选**：扩展模块把 `jackson-databind` 作为可选依赖，未引入 Jackson 时不会注册 `DesensitizeModule`。

## 破坏性变更

- 移除 `BaseVo` 基类约束，视图对象可以是任意 POJO。
- 未配置加密密钥时，加密字段读写会抛出异常（此前无此约束）。
- `SqlWrapper` 的统计语句统一按子查询包装，包含 `group by`、`having` 或聚合列时都能正确计数。

## 已知限制

- 字段加密只适用于字符串字段，数值与日期需先转为字符串。
- 确定性加密会泄露明文相等关系与出现频率，只建议用于必须等值检索的字段。
- `union` 不支持嵌套，子查询自身不能包含 `order by`，排序需写在外层。
- `JpaPlusScope` 为线程绑定，不自动跟随数据源路由，需要在路由切面或任务入口显式绑定。
- 慢 SQL 日志只输出 SQL 原文，不包含绑定参数。
- `@RateLimit` 基于本地 Guava `RateLimiter`，多实例部署需改用分布式限流。

## 文档与示例

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

## 验证情况

- 核心模块、自动配置与扩展模块单元测试全部通过。
- 示例工程 36 个用例通过，覆盖 JPA / HQL / 原生 SQL 三套实现与新能力示例。
- 文档站中英文构建通过。

## 反馈

- 问题反馈：<https://github.com/neopen/spring-boot-jpa-plus/issues>
- 功能建议与 PR 欢迎提交。
