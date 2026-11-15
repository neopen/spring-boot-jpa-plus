# 版本发布

::: warning 特别说明
- 当前正式发布版本为 **0.4.x**，可以直接通过 Maven 依赖引入。
- 代码仓库 `main` 分支对应 **0.5 版本**，尚未发布，将在后续正式发布。
- 本站文档按 **0.5 版本**编写，描述的是 `main` 分支的最新行为。
- 因此，通过 Maven 引入的 **0.4 版本不支持**本文档中的 0.5 新功能。
:::

## 版本对应关系

| 项 | 版本 | 说明 |
| --- | --- | --- |
| Maven 正式发布 | `0.4.x` | 可直接依赖，仅包含 0.4 已有能力 |
| 源码 `main` 分支 | `0.5.0`（未发布） | 包含全部 0.5 新增能力 |
| 本站文档 | `0.5` | 按 0.5 行为编写 |

## 0.5 新增能力

以下能力随 0.5 版本发布，0.4 版本不包含：

| 能力 | 入口类 / 注解 | 文档 |
| --- | --- | --- |
| 枚举值转换 | `@EnumValue`、`EnumValues`、`EnumValueConverter` | [枚举值转换](/core/enum-convert) |
| 字段加密 | `@EncryptedField`、`EncryptedFields` | [字段加密](/core/encryption) |
| 树形结构构建 | `TreeUtils` | [树形结构](/core/tree) |
| 雪花 ID | `SnowflakeIdGenerator` | [雪花 ID](/core/id-generator) |
| 查询构造器增强 | `raw`、`exists`、`notExists`、`alias` | [HQL 查询](/query/hql)、[原生 SQL 查询](/query/sql) |
| 存在性判断 | `JpaPlusSpecExecutor#exists` | [Specification 执行器](/core/executor) |
| 数据脱敏 | `@Desensitize`、`Desensitizes` | [数据脱敏](/extension/desensitize) |
| 乐观锁重试 | `@RetryOnOptimisticLock` | [乐观锁重试](/extension/retry) |
| 慢方法日志 | `@SlowLog` | [慢方法日志](/extension/slow-log) |

## 0.4 能力范围

0.4 版本已经包含模块骨架、通用 DAO、动态条件查询、Specification 构建器与执行器、
HQL / 原生 SQL 构建器、逻辑删除、审计字段、批量操作、DTO 投影、安全排序、
Entity → VO 转换、Param → Entity 映射，以及幂等、限流、操作日志、统一分页响应等扩展组件。

## 选择依赖版本

- **稳定使用**：依赖 0.4.x 发布版本，并按 0.4 的行为使用 API。
- **提前体验 0.5**：克隆源码后执行 `mvn install -DskipTests` 安装到本地仓库，再将依赖版本改为本地构建版本。
- **等待正式发布**：0.5 正式发布后，本站文档与 Maven 依赖将保持一致。

## 发布记录

| 版本 | 状态 | 说明 |
| --- | --- | --- |
| `0.5.0` | 开发中 | 新增枚举转换、字段加密、树工具、雪花 ID、脱敏、重试与慢日志等能力 |
| `0.4.x` | 已发布 | 基础能力与扩展组件 |
