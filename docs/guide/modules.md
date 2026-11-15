# 模块与依赖

## 模块划分

```
spring-boot-jpa-plus
├── spring-boot-jpa-plus-core          核心：DAO、查询构建、逻辑删除、审计、批量、投影
├── spring-boot-jpa-plus-convert       转换：Entity → VO、Param → Entity
├── spring-boot-jpa-plus-autoconfigure 配置绑定与 Bean 注册
├── spring-boot-jpa-plus-starter       统一依赖入口
├── spring-boot-jpa-plus-extension     可选扩展：幂等、限流、操作日志、统一响应
└── examples/spring-boot-jpa-plus-demo 示例应用
```

## 各模块职责

| 模块 | 主要内容 | 是否被 starter 引入 |
| --- | --- | --- |
| `core` | `JpaPlusDao`、`AbstractJpaPlusDao`、`QueryWrapper`、`SpecWrapper`、`JpaPlusSpecExecutor`、`FetchJoinPaging`、`HqlWrapper`、`SqlWrapper`、`JpaPlusHqlExecutor`、`JpaPlusSqlExecutor`、`LogicDeletes`、`Audits`、`BatchOperation`、`ResultMapper`、`Sorts` | 是 |
| `convert` | `BaseVoService`、`AbstractBaseService`、`BaseVoCommom`、`ParamMapper`、`@ParamField`、`@ParamIgnore`、`@DateStrFormat`、`@DateStrValid` | 是 |
| `autoconfigure` | `JpaPlusAutoConfiguration`、`JpaPlusProperties`、`JpaPlusRuntimeConfigurer` | 是 |
| `starter` | 仅依赖声明，不含代码 | — |
| `extension` | `@Idempotent`、`@RateLimit`、`@SysControllerLog`、`@SysServiceLog`、`Json`、`JsonPageConverter` | 否 |

## 依赖关系

```
starter
 ├── autoconfigure ──> core
 ├── core
 └── convert ───────> core（使用 JpaPlusException）
```

`extension` 独立于上述链路，按需引入，不影响核心模块的使用方式。

## 选型建议

- **只用查询能力**：引入 `starter` 即可，核心能力全部可用。
- **需要幂等 / 限流 / 统一分页响应**：额外引入 `spring-boot-jpa-plus-extension`。
- **只想要对象转换**：单独引入 `spring-boot-jpa-plus-convert`。
- **不希望自动配置**：引入 `core`，手动 `new JpaPlusHqlExecutor(entityManager)`，或配置 `neopen.jpa.enabled=false`。

## 版本对齐

所有模块由父 POM 统一管理版本，业务侧只需声明一次 `${version}`。使用 `dependencyManagement` 统一版本时，`extension` 也要一并纳入，否则容易与其他模块版本错位。
