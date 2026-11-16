# Example Project

`examples/spring-boot-jpa-plus-demo` is a runnable example application that uses an H2 in-memory database and covers two scenarios: order aggregation and product stock.

## What it covers

- **Order aggregation**: JPA CRUD, batch loading of order items (avoiding N+1), and HQL plus native SQL implementations of the same query.
- **Joins and filtering**: `OrderJpaServiceImpl.buildPredicates` uses `SpecWrapper` to join `orderProducts`, filters by item name and quantity, enables `distinct` automatically so that pagination counting stays correct, and supports the sender phone number and order amount range.
- **Product stock**: a `SpecWrapper` query compared with HQL and native SQL implementations, native field comparisons, batch restocking, and aggregate statistics based on `SqlWrapper`.
- **Three implementations of the same interface**: `OrderService` and `ProductService` each have JPA, HQL and native SQL implementations.
- **New in 0.5**: `QueryFeatureService`, `CustomerSecretService` and `AuditConfiguration` demonstrate request paging, union, datasource scopes, field encryption and the audit auditor.
- **New in 0.6**: `DocumentFeatureService` and `FeatureDataConfiguration` demonstrate multi-tenancy, row-level data permission, logical-delete restoration, change history and batch upsert.

## 0.5 feature examples

`QueryFeatureController` collects the capabilities added in 0.5. See `service/QueryFeatureService` and `service/CustomerSecretService`:

| Method | Path | Demonstration |
| --- | --- | --- |
| GET | `/feature/page?page=1&size=10&sort=id,desc` | `PageParams` validates page number, page size and sort fields |
| GET | `/feature/union` | `SqlWrapper.union` merges paid and cancelled orders |
| GET | `/feature/scope?orderNo=SD-` | `JpaPlusScope` binds the `archive-view` scope to read logically deleted orders |
| POST | `/feature/secret?customerNo=C-001&idCard=...&phone=...` | Writes fields with random IV and deterministic encryption |
| GET | `/feature/secret?phone=13800001234` | Equality search by deterministic ciphertext |

The matching configuration lives in `application.yml`:

```yaml
neopen:
  jpa:
    encryption:
      key: demo-encryption-key
    scopes:
      archive-view:
        logic-delete-enabled: false
```

- The `archive-view` scope only disables the logic delete filter; other queries still exclude deleted rows.
- Phone numbers use `DeterministicEncryptedFieldConverter`, so encrypt the plaintext before querying; ID cards use a random IV, so the same plaintext produces different ciphertext each time.
- `AuditConfiguration` registers an `AuditorProvider` that reads the `X-Demo-User` header and falls back to `demo-user` outside a web request.

## 0.6 feature examples

`DocumentFeatureController` collects the capabilities added in 0.6. See `bean/entity/DocumentRecord` and `config/FeatureDataConfiguration`:

| Method | Path | Demonstration |
| --- | --- | --- |
| GET | `/feature/documents` | Appends tenant and department conditions automatically |
| POST | `/feature/documents?docNo=DOC-001&title=...` | Fills the tenant column on write |
| POST | `/feature/documents/{id}/rename?title=...` | Produces an `UPDATE` change-history record |
| DELETE | `/feature/documents/{id}` | Logical deletion |
| POST | `/feature/documents/{id}/restore` | `LogicDeletes.restoreById` restores the row |
| POST | `/feature/documents/upsert` | `BatchOperation.upsertAll` inserts or updates by `docNo` |
| GET | `/feature/documents/audit` | Returns the in-process change history |

The `X-Demo-Tenant` and `X-Demo-Dept` headers set the tenant and department; they default to `demo-tenant` and the default department:

```bash
curl -H "X-Demo-Tenant: tenant-a" -H "X-Demo-Dept: 10" \
  "http://localhost:8080/feature/documents"
```

- `FeatureDataConfiguration` registers a `TenantProvider`, a `DataPermissionProvider` and an `AuditLogPublisher`.
- `DocumentRecord` is annotated with `@Audited`, `@TenantId` and `@LogicDelete`, and registers both listeners in `@EntityListeners`.
- `DemoAuditStore` keeps change history in an in-memory list, which is cleared when the demo restarts.

## Route layout

| Prefix | Implementation |
| --- | --- |
| `/order`, `/product` | JPA |
| `/order/hql`, `/product/hql` | HQL |
| `/order/sql`, `/product/sql` | Native SQL (`SqlWrapper` + `JpaPlusSqlExecutor`) |

Main endpoints:

| Method | Path | Implementation |
| --- | --- | --- |
| POST | `/order` | JPA |
| GET | `/order/page`, `/order/list`, `/order/{orderNo}` | JPA |
| GET | `/order/hql/page`, `/order/hql/list`, `/order/hql/{orderNo}` | HQL |
| GET | `/order/sql/page`, `/order/sql/list`, `/order/sql/{orderNo}` | Native SQL |
| POST | `/product` | JPA |
| GET | `/product/page`, `/product/list`, `/product/{id}` | JPA |
| GET | `/product/low-stock`, `/product/stock-summary` | JPA |
| POST | `/product/restock` | JPA entity update |
| GET | `/product/hql/page`, `/product/hql/list`, `/product/hql/{id}` | HQL |
| GET | `/product/hql/low-stock`, `/product/hql/stock-summary` | HQL + native SQL |
| POST | `/product/hql/restock` | HQL batch update |
| GET | `/product/sql/page`, `/product/sql/list`, `/product/sql/{id}` | Native SQL |
| GET | `/product/sql/low-stock`, `/product/sql/stock-summary` | Native SQL |
| POST | `/product/sql/restock` | Native SQL batch update |

## Running

```bash
cd examples/spring-boot-jpa-plus-demo
mvn spring-boot:run
```

## Testing

The tests are split into three tracks by JUnit `@Tag`:

```bash
mvn test                       # 全部
mvn test -Punit-test           # 纯单元测试，不启动 Spring 上下文
mvn test -Pintegration-test    # Spring + H2 服务测试，覆盖三套实现
mvn test -Papi-test            # MockMvc HTTP 测试
```

## Recommended reading

It is recommended to read the code by comparing the three implementations of the same interface: first the JPA implementation to understand `SpecWrapper` and the executor, then the HQL implementation to understand the fluent construction and the count rules, and finally the SQL implementation to understand column name conversion and result mapping.
