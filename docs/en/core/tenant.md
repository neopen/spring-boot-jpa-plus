# Multi-Tenancy

Multi-tenancy combines an entity field annotation with a tenant provider: the tenant column is filled on write and filtered automatically on query, so business code never passes the tenant explicitly.

## Declaration

```java
@Entity
@EntityListeners(TenantListener.class)
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "doc_no", nullable = false, unique = true)
    private String docNo;
}
```

| Constraint | Description |
| --- | --- |
| Field type | `String`, `Long`, `long`, `Integer`, `int`, `UUID` |
| State | The field does not need to be initialized; the listener only fills it when null |
| Listener | `@EntityListeners(TenantListener.class)` is required for automatic filling |

An unsupported field type raises `IllegalArgumentException` when the metadata is first resolved.

## Providing the Current Tenant

Register a `TenantProvider` bean and the auto-configuration wires it into `Tenants`:

```java
@Bean
public TenantProvider tenantProvider() {
    return () -> TenantHolder.current();
}
```

Or set it directly:

```java
Tenants.setProvider(() -> TenantHolder.current());
```

When `TenantProvider#currentTenant` returns `null` or a blank string, there is no tenant context: **no filtering is applied** and writes are not blocked. If isolation must be enforced, guarantee a tenant at the business entry point.

## Query Filtering

`JpaPlusSpecExecutor` appends the tenant condition after logical deletion, and only for entities that declare `@TenantId`:

```java
// Appends: where deleted = false and tenant_id = ?
Page<DocumentRecord> page = JpaPlusSpecExecutor
        .of(entityManager, DocumentRecord.class)
        .findPage(spec, pageable);
```

To combine manually:

```java
Specification<DocumentRecord> spec = Tenants.filter(
        DocumentRecord.class,
        (root, query, cb) -> cb.equal(root.get("docNo"), "DOC-001")
);
```

| Method | Description |
| --- | --- |
| `Tenants.filter(Class<T>)` | Returns only the tenant condition |
| `Tenants.filter(Class<T>, Specification<T>)` | Combines with the given condition using `AND` |
| `Tenants.isTenantEntity(Class<?>)` | Checks whether an entity declares a tenant field |
| `Tenants.currentTenant()` | Reads the effective tenant; `null` when disabled or absent |

## Fixed Tenant per Scope

Archiving or operations jobs may need to access a single tenant's data across tenants. Fix the tenant per scope:

```yaml
neopen:
  jpa:
    scopes:
      archive-view:
        tenant: demo-tenant
```

```java
JpaPlusScope.run("archive-view", () -> {
    // Uses demo-tenant and ignores the global provider
    documentRepository.findAll(spec);
});
```

Alternatively call `Tenants.setTenant("archive-view", "demo-tenant")`; pass `null` to clear it.

Resolution priority: fixed scope tenant > scope provider > global provider.

## Configuration

```yaml
neopen:
  jpa:
    tenant:
      enabled: true
    scopes:
      report:
        tenant-enabled: false
```

| Property | Default | Description |
| --- | --- | --- |
| `neopen.jpa.tenant.enabled` | `true` | Whether tenant filtering is enabled |
| `neopen.jpa.scopes.<name>.tenant-enabled` | empty | Whether the scope filters by tenant |
| `neopen.jpa.scopes.<name>.tenant` | empty | Fixed tenant for the scope |

Disabling filtering only affects queries; `TenantListener` still fills the field when a tenant context exists.

## Boundaries

- Automatic filtering covers JPA Criteria queries only, including `SpecWrapper` and `JpaPlusSpecExecutor`.
- `HqlWrapper`, `SqlWrapper` and native SQL do not append tenant conditions; write them explicitly.
- Tenant fields only isolate queries; include the tenant column in unique indexes at the database level for cross-tenant uniqueness.
- Bulk JPQL updates/deletes do not trigger listeners; use managed entities when writing tenant data.
