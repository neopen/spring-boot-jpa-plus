# API Index

User-facing entry classes are listed by module. For complete signatures and parameter descriptions, see [Javadoc](https://jpa.helpenx.com/api/).

## core

| Class | Responsibility |
| --- | --- |
| `io.github.neopen.core.persistence.dao.JpaPlusDao` | Generic data access interface |
| `io.github.neopen.core.persistence.dao.AbstractJpaPlusDao` | Abstract DAO implementation based on `EntityManager` |
| `io.github.neopen.core.persistence.batch.BatchOperation` | Batch persist / merge / remove and chunked consumption |
| `io.github.neopen.core.persistence.query.QueryWrapper` | Dynamic condition query builder |
| `io.github.neopen.core.persistence.query.QueryOperator` | Dynamic condition operator enum |
| `io.github.neopen.core.persistence.condition.QueryCondition` | Annotation-based query conditions |
| `io.github.neopen.core.persistence.condition.ConditionOperator` | Annotation condition operator enum |
| `io.github.neopen.core.persistence.condition.QueryConditionMapper` | Resolver from a parameter object to a condition list |
| `io.github.neopen.core.persistence.spec.SpecWrapper` | Chained `Specification` builder |
| `io.github.neopen.core.persistence.spec.SpecOperator` / `StandardSpecOperator` | Custom operator interface and built-in implementation |
| `io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor` | `Specification` executor (pagination, slices, counting, deletion) |
| `io.github.neopen.core.persistence.spec.FetchJoinPaging` | Three-stage fetch join pagination |
| `io.github.neopen.core.persistence.sort.Sorts` | Safe sort resolver |
| `io.github.neopen.core.persistence.page.PageParams` | Request paging parameters with page, size and sort validation |
| `io.github.neopen.core.scope.JpaPlusScope` | Datasource scope context isolating logic-delete, audit, encryption and batch switches |
| `io.github.neopen.core.persistence.logicdelete.LogicDelete` | Logic delete field annotation |
| `io.github.neopen.core.persistence.logicdelete.LogicDeletes` | Logic delete filtering and delete operations |
| `io.github.neopen.core.persistence.audit.CreatedAt` / `UpdatedAt` / `CreatedBy` / `UpdatedBy` | Audit field annotations |
| `io.github.neopen.core.persistence.audit.AuditListener` | Audit field entity listener |
| `io.github.neopen.core.persistence.audit.Audits` | Audit runtime switches and configuration |
| `io.github.neopen.core.persistence.audit.AuditorProvider` | SPI that supplies the current auditor |
| `io.github.neopen.core.persistence.mapping.ResultMapper` | Mapper from results to DTOs |
| `io.github.neopen.core.persistence.hql.HqlWrapper` | Chained HQL builder |
| `io.github.neopen.core.persistence.hql.HqlOperator` / `StandardHqlOperator` | HQL custom operator interface and built-in implementation |
| `io.github.neopen.core.persistence.hql.HqlQuery` | Build artifact: statement, count statement, parameters, sorting |
| `io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor` | HQL executor |
| `io.github.neopen.core.persistence.sql.SqlWrapper` | Chained native SQL builder |
| `io.github.neopen.core.persistence.sql.SqlOperator` / `StandardSqlOperator` | SQL custom operator interface and built-in implementation |
| `io.github.neopen.core.persistence.sql.SqlQuery` | Build artifact: statement, count statement, parameters, sorting |
| `io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor` | Native SQL executor |
| `io.github.neopen.core.persistence.convert.EnumValue` | Enum persisted value annotation |
| `io.github.neopen.core.persistence.convert.EnumValues` | Conversion between enum values and constants |
| `io.github.neopen.core.persistence.convert.EnumValueConverter` | Base class of enum JPA converters |
| `io.github.neopen.core.persistence.encrypt.EncryptedField` | Encrypted field marker annotation |
| `io.github.neopen.core.persistence.encrypt.EncryptedFields` | AES-GCM encryption and decryption utility |
| `io.github.neopen.core.persistence.encrypt.EncryptedFieldConverter` | Encrypted field JPA converter |
| `io.github.neopen.core.persistence.encrypt.DeterministicEncryptedFieldConverter` | Deterministic encrypted field JPA converter with equality search support |
| `io.github.neopen.core.util.TreeUtils` | Builds a tree from a flat list |
| `io.github.neopen.core.id.SnowflakeIdGenerator` | Snowflake ID generator |

## convert

| Class | Responsibility |
| --- | --- |
| `io.github.neopen.convert.converter.BaseVoService` | Entity → VO conversion service interface |
| `io.github.neopen.convert.converter.BaseVoCommom` | Conversion functional interface |
| `io.github.neopen.convert.converter.AbstractBaseService` | Base implementation of conversion and predicate building |
| `io.github.neopen.convert.converter.BasePredicateService` | Criteria predicate building extension point |
| `io.github.neopen.convert.mapping.ParamMapper` | Param → Entity mapping |
| `io.github.neopen.convert.mapping.ParamField` / `ParamIgnore` | Mapping alias and ignore annotations |
| `io.github.neopen.convert.annotation.DateStrFormat` / `DateStrValid` | Date string formatting and validation annotations |

## autoconfigure

| Class | Responsibility |
| --- | --- |
| `io.github.neopen.autoconfigure.JpaPlusProperties` | `neopen.jpa.*` property binding |
| `io.github.neopen.autoconfigure.JpaPlusAutoConfiguration` | Executor registration and runtime configuration application |
| `io.github.neopen.autoconfigure.JpaPlusRuntimeConfigurer` | Writes configuration into static switches (batch size, logic delete, audit, encryption) |

## extension

| Class | Responsibility |
| --- | --- |
| `io.github.neopen.extension.annotation.Idempotent` | Idempotency control |
| `io.github.neopen.extension.annotation.RateLimit` | Endpoint rate limiting |
| `io.github.neopen.extension.annotation.SysControllerLog` / `SysServiceLog` | Operation and exception log descriptions |
| `io.github.neopen.extension.annotation.Desensitize` | Data masking marker |
| `io.github.neopen.extension.annotation.RetryOnOptimisticLock` | Optimistic lock conflict retry |
| `io.github.neopen.extension.annotation.SlowLog` | Slow method log |
| `io.github.neopen.extension.desensitize.DesensitizeType` / `Desensitizes` | Masking types and utilities |
| `io.github.neopen.extension.desensitize.DesensitizeValueFilter` | fastjson2 masking filter |
| `io.github.neopen.extension.desensitize.DesensitizeModule` / `DesensitizeJsonSerializer` | Jackson masking module and serializer |
| `io.github.neopen.extension.response.Json` | Unified response structure |
| `io.github.neopen.extension.response.JsonPageConverter` | Paginated response conversion |
| `io.github.neopen.extension.response.JsonPageResponseFactory` | Paginated response structure extension point |
| `io.github.neopen.extension.exception.RateLimitException` | Rate limit exception |

## Internal Implementation Notes

The following classes do not guarantee compatibility and are not recommended for direct use in application code:

- Statement derivation logic inside `JpaPlusQueryBuilder`, `HqlParameterCollector` and `JpaPlusSqlExecutor`
- Three-stage implementation details of `FetchJoinPaging` (the static entry `findPage` can be reused)
- Utility classes under `extension.utils` (`IdempotentUtil`, `RateLimiterUtil`, `NetworkUtil`)
- `LRUCache` and `MyLruCache` under `extension.cache`
