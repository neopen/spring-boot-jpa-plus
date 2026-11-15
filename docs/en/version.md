# Releases

::: warning Important notice
- The current official release is **0.4.x**, available as a Maven dependency.
- The `main` branch corresponds to **version 0.5**, which is not released yet and will be published later.
- This documentation is written for **version 0.5** and describes the latest behavior of `main`.
- As a result, the **0.4 release installed from Maven does not support** the 0.5 features described here.
:::

## Version mapping

| Item | Version | Description |
| --- | --- | --- |
| Maven release | `0.4.x` | Can be used as a dependency; contains 0.4 capabilities only |
| Source `main` branch | `0.5.0` (unreleased) | Contains all 0.5 capabilities |
| This documentation | `0.5` | Written against 0.5 behavior |

## New in 0.5

The following capabilities ship with 0.5 and are not available in 0.4:

| Capability | Entry point | Docs |
| --- | --- | --- |
| Enum value conversion | `@EnumValue`, `EnumValues`, `EnumValueConverter` | [Enum conversion](/en/core/enum-convert) |
| Field encryption | `@EncryptedField`, `EncryptedFields` | [Field encryption](/en/core/encryption) |
| Tree building | `TreeUtils` | [Tree structure](/en/core/tree) |
| Snowflake IDs | `SnowflakeIdGenerator` | [Snowflake ID](/en/core/id-generator) |
| Query builder enhancements | `raw`, `exists`, `notExists`, `alias` | [HQL queries](/en/query/hql), [Native SQL](/en/query/sql) |
| Existence check | `JpaPlusSpecExecutor#exists` | [Specification executor](/en/core/executor) |
| Data desensitization | `@Desensitize`, `Desensitizes` | [Data desensitization](/en/extension/desensitize) |
| Optimistic-lock retry | `@RetryOnOptimisticLock` | [Optimistic-lock retry](/en/extension/retry) |
| Slow method logging | `@SlowLog` | [Slow method logging](/en/extension/slow-log) |

## What 0.4 provides

0.4 already includes the module skeleton, the generic DAO, dynamic condition queries,
the Specification builder and executor, the HQL / native SQL builders, logical deletion,
auditing fields, batch operations, DTO projection, safe sorting, Entity → VO conversion,
Param → Entity mapping, plus the extension components for idempotency, rate limiting,
operation logging, and unified paged responses.

## Choosing a version

- **Stable usage**: depend on the 0.4.x release and use the 0.4 API behavior.
- **Try 0.5 early**: clone the repository, run `mvn install -DskipTests` to install into the local repository, then point the dependency version at the locally built artifact.
- **Wait for the release**: once 0.5 is published, this documentation and the Maven artifacts will match.

## Release history

| Version | Status | Description |
| --- | --- | --- |
| `0.5.0` | In development | Adds enum conversion, field encryption, tree utilities, snowflake IDs, desensitization, retry, slow logging |
| `0.4.x` | Released | Base capabilities and extension components |
