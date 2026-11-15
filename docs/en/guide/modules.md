# Modules and Dependencies

## Module layout

```
spring-boot-jpa-plus
├── spring-boot-jpa-plus-core          核心：DAO、查询构建、逻辑删除、审计、批量、投影
├── spring-boot-jpa-plus-convert       转换：Entity → VO、Param → Entity
├── spring-boot-jpa-plus-autoconfigure 配置绑定与 Bean 注册
├── spring-boot-jpa-plus-starter       统一依赖入口
├── spring-boot-jpa-plus-extension     可选扩展：幂等、限流、操作日志、统一响应
└── examples/spring-boot-jpa-plus-demo 示例应用
```

## Responsibilities of each module

| Module | Main contents | Included by starter |
| --- | --- | --- |
| `core` | `JpaPlusDao`, `AbstractJpaPlusDao`, `QueryWrapper`, `SpecWrapper`, `JpaPlusSpecExecutor`, `FetchJoinPaging`, `HqlWrapper`, `SqlWrapper`, `JpaPlusHqlExecutor`, `JpaPlusSqlExecutor`, `LogicDeletes`, `Audits`, `BatchOperation`, `ResultMapper`, `Sorts`, `EnumValues`, `EncryptedFields`, `TreeUtils`, `SnowflakeIdGenerator` | Yes |
| `convert` | `BaseVoService`, `AbstractBaseService`, `BaseVoCommom`, `ParamMapper`, `@ParamField`, `@ParamIgnore`, `@DateStrFormat`, `@DateStrValid` | Yes |
| `autoconfigure` | `JpaPlusAutoConfiguration`, `JpaPlusProperties`, `JpaPlusRuntimeConfigurer` | Yes |
| `starter` | Dependency declarations only, no code | — |
| `extension` | `@Idempotent`, `@RateLimit`, `@SysControllerLog`, `@SysServiceLog`, `@Desensitize`, `@RetryOnOptimisticLock`, `@SlowLog`, `Json`, `JsonPageConverter` | No |

## Dependency relationships

```
starter
 ├── autoconfigure ──> core
 ├── core
 └── convert ───────> core（使用 JpaPlusException）
```

`extension` is independent of the chain above; include it on demand without affecting how the core modules are used.

## Choosing the modules

- **Query capabilities only**: include `starter` and all core capabilities are available.
- **Need idempotency / rate limiting / desensitization / retry / unified pagination response**: additionally include `spring-boot-jpa-plus-extension`.
- **Object conversion only**: include `spring-boot-jpa-plus-convert` on its own.
- **Do not want auto-configuration**: include `core` and create `new JpaPlusHqlExecutor(entityManager)` manually, or set `neopen.jpa.enabled=false`.

## Version alignment

All modules are versioned by the parent POM, so the business side only declares `${version}` once. When `dependencyManagement` is used to align versions, `extension` must be included as well; otherwise it can easily drift from the versions of the other modules.
