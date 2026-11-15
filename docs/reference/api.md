# API 索引

按模块列出面向用户的入口类。完整签名与参数说明见 [Javadoc](https://jpa.helpenx.com/api/)。

## core

| 类 | 职责 |
| --- | --- |
| `io.github.neopen.core.persistence.dao.JpaPlusDao` | 通用数据访问接口 |
| `io.github.neopen.core.persistence.dao.AbstractJpaPlusDao` | 基于 `EntityManager` 的 DAO 抽象实现 |
| `io.github.neopen.core.persistence.batch.BatchOperation` | 批量 persist / merge / remove 与分批消费 |
| `io.github.neopen.core.persistence.query.QueryWrapper` | 动态条件查询构造器 |
| `io.github.neopen.core.persistence.query.QueryOperator` | 动态条件运算符枚举 |
| `io.github.neopen.core.persistence.condition.QueryCondition` | 注解式查询条件 |
| `io.github.neopen.core.persistence.condition.ConditionOperator` | 注解条件运算符枚举 |
| `io.github.neopen.core.persistence.condition.QueryConditionMapper` | 参数对象到条件列表的解析器 |
| `io.github.neopen.core.persistence.spec.SpecWrapper` | `Specification` 链式构建器 |
| `io.github.neopen.core.persistence.spec.SpecOperator` / `StandardSpecOperator` | 自定义运算符接口与内置实现 |
| `io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor` | `Specification` 执行器（分页、切片、计数、删除） |
| `io.github.neopen.core.persistence.spec.FetchJoinPaging` | fetch join 三段式分页 |
| `io.github.neopen.core.persistence.sort.Sorts` | 安全排序解析器 |
| `io.github.neopen.core.persistence.logicdelete.LogicDelete` | 逻辑删除字段注解 |
| `io.github.neopen.core.persistence.logicdelete.LogicDeletes` | 逻辑删除过滤与删除操作 |
| `io.github.neopen.core.persistence.audit.CreatedAt` / `UpdatedAt` / `CreatedBy` / `UpdatedBy` | 审计字段注解 |
| `io.github.neopen.core.persistence.audit.AuditListener` | 审计字段实体监听器 |
| `io.github.neopen.core.persistence.audit.Audits` | 审计运行时开关与配置 |
| `io.github.neopen.core.persistence.mapping.ResultMapper` | 结果到 DTO 的映射器 |
| `io.github.neopen.core.persistence.hql.HqlWrapper` | HQL 链式构建器 |
| `io.github.neopen.core.persistence.hql.HqlOperator` / `StandardHqlOperator` | HQL 自定义运算符接口与内置实现 |
| `io.github.neopen.core.persistence.hql.HqlQuery` | 构建产物：语句、count 语句、参数、排序 |
| `io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor` | HQL 执行器 |
| `io.github.neopen.core.persistence.sql.SqlWrapper` | 原生 SQL 链式构建器 |
| `io.github.neopen.core.persistence.sql.SqlOperator` / `StandardSqlOperator` | SQL 自定义运算符接口与内置实现 |
| `io.github.neopen.core.persistence.sql.SqlQuery` | 构建产物：语句、count 语句、参数、排序 |
| `io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor` | 原生 SQL 执行器 |
| `io.github.neopen.core.persistence.convert.EnumValue` | 枚举持久化值注解 |
| `io.github.neopen.core.persistence.convert.EnumValues` | 枚举值与常量互转 |
| `io.github.neopen.core.persistence.convert.EnumValueConverter` | 枚举 JPA 转换器基类 |
| `io.github.neopen.core.persistence.encrypt.EncryptedField` | 加密字段标记注解 |
| `io.github.neopen.core.persistence.encrypt.EncryptedFields` | AES-GCM 加解密工具 |
| `io.github.neopen.core.persistence.encrypt.EncryptedFieldConverter` | 加密字段 JPA 转换器 |
| `io.github.neopen.core.util.TreeUtils` | 平铺列表构建树 |
| `io.github.neopen.core.id.SnowflakeIdGenerator` | 雪花 ID 生成器 |

## convert

| 类 | 职责 |
| --- | --- |
| `io.github.neopen.convert.converter.BaseVoService` | Entity → VO 转换服务接口 |
| `io.github.neopen.convert.converter.BaseVoCommom` | 转换函数式接口 |
| `io.github.neopen.convert.converter.AbstractBaseService` | 转换与谓词构建的基础实现 |
| `io.github.neopen.convert.converter.BasePredicateService` | Criteria 谓词构建扩展点 |
| `io.github.neopen.convert.mapping.ParamMapper` | Param → Entity 映射 |
| `io.github.neopen.convert.mapping.ParamField` / `ParamIgnore` | 映射别名与忽略注解 |
| `io.github.neopen.convert.annotation.DateStrFormat` / `DateStrValid` | 日期字符串格式化与校验注解 |

## autoconfigure

| 类 | 职责 |
| --- | --- |
| `io.github.neopen.autoconfigure.JpaPlusProperties` | `neopen.jpa.*` 配置绑定 |
| `io.github.neopen.autoconfigure.JpaPlusAutoConfiguration` | 执行器注册与运行时配置应用 |
| `io.github.neopen.autoconfigure.JpaPlusRuntimeConfigurer` | 把配置写入静态开关（批量大小、逻辑删除、审计、加密） |

## extension

| 类 | 职责 |
| --- | --- |
| `io.github.neopen.extension.annotation.Idempotent` | 幂等控制 |
| `io.github.neopen.extension.annotation.RateLimit` | 接口限流 |
| `io.github.neopen.extension.annotation.SysControllerLog` / `SysServiceLog` | 操作与异常日志描述 |
| `io.github.neopen.extension.annotation.Desensitize` | 数据脱敏标记 |
| `io.github.neopen.extension.annotation.RetryOnOptimisticLock` | 乐观锁冲突重试 |
| `io.github.neopen.extension.annotation.SlowLog` | 慢方法日志 |
| `io.github.neopen.extension.desensitize.DesensitizeType` / `Desensitizes` | 脱敏类型与工具 |
| `io.github.neopen.extension.desensitize.DesensitizeValueFilter` | fastjson2 脱敏过滤器 |
| `io.github.neopen.extension.response.Json` | 统一响应结构 |
| `io.github.neopen.extension.response.JsonPageConverter` | 分页响应转换 |
| `io.github.neopen.extension.response.JsonPageResponseFactory` | 分页响应结构扩展点 |
| `io.github.neopen.extension.exception.RateLimitException` | 限流异常 |

## 内部实现说明

下列类不承诺兼容性，不建议在业务中直接使用：

- `JpaPlusQueryBuilder`、`HqlParameterCollector`、`JpaPlusSqlExecutor` 内部的语句派生逻辑
- `FetchJoinPaging` 的三段式实现细节（静态入口 `findPage` 可复用）
- `extension.utils` 下的工具类（`IdempotentUtil`、`RateLimiterUtil`、`NetworkUtil`）
- `extension.cache` 下的 `LRUCache`、`MyLruCache`
