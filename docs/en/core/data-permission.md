# Row-Level Data Permission

Row-level data permission abstracts "which rows a user can see" into field-set rules. `DataScopes` converts them into query conditions and combines them with existing conditions using `AND`.

## Defining a Provider

```java
@Bean
public DataPermissionProvider dataPermissionProvider() {
    return entityType -> {
        if (!DocumentRecord.class.equals(entityType)) {
            return List.of();
        }

        Set<Long> deptIds = SecurityContext.visibleDeptIds();

        return deptIds.isEmpty()
                ? List.of()
                : List.of(DataRule.of("deptId", deptIds.toArray()));
    };
}
```

A `DataRule` means "the field value must be within the given set":

```java
DataRule rule = new DataRule("deptId", List.of(1L, 2L));
DataRule same = DataRule.of("deptId", 1L, 2L);
```

| Constraint | Description |
| --- | --- |
| `field` | Entity field name, must not be blank |
| `values` | Allowed values, must not be empty |
| Empty rule | Returning `null` or an empty list means no restriction |

Exceptions thrown by the provider abort the query; handle degradation inside the provider. An empty list means **no restriction**, which differs from "no permission". To deny all data, return an impossible rule or throw at the business layer.

## Query Filtering

`JpaPlusSpecExecutor` appends the permission condition automatically. The order is logical deletion → tenant → data permission:

```java
Specification<DocumentRecord> spec =
        (root, query, cb) -> cb.equal(root.get("docNo"), "DOC-001");

Page<DocumentRecord> page = JpaPlusSpecExecutor
        .of(entityManager, DocumentRecord.class)
        .findPage(spec, pageable);
// where deleted = false and tenant_id = ? and dept_id in (?, ?)
```

Manual combination:

```java
Specification<DocumentRecord> scoped =
        DataScopes.filter(DocumentRecord.class, spec);
```

| Method | Description |
| --- | --- |
| `DataScopes.filter(Class<T>)` | Returns only the permission condition |
| `DataScopes.filter(Class<T>, Specification<T>)` | Combines with the given condition using `AND` |
| `DataScopes.isActive()` | Whether an effective provider exists |
| `DataScopes.setProvider(...)` | Sets the provider programmatically |

## Scope Overrides

```java
DataScopes.setProvider("batch-job", entityType -> List.of());
DataScopes.setEnabled("batch-job", false);

JpaPlusScope.run("batch-job", () -> documentRepository.findAll(spec));
```

- Scope providers take precedence over the global provider.
- When disabled, no condition is appended even if a provider exists.

Global switch:

```yaml
neopen:
  jpa:
    data-scope:
      enabled: true
```

`neopen.jpa.scopes` currently has no data-permission fields; register scope-level switches and providers with `DataScopes.setEnabled(scope, false)` and `DataScopes.setProvider(scope, provider)`.

## Combination Order

| Order | Filter | Switch |
| --- | --- | --- |
| 1 | Logical deletion | `@LogicDelete`, `neopen.jpa.logic-delete.enabled` |
| 2 | Multi-tenancy | `@TenantId`, `neopen.jpa.tenant.enabled` |
| 3 | Row-level permission | `DataPermissionProvider`, `neopen.jpa.data-scope.enabled` |

All three layers are combined with `Specification.and(...)`; a disabled or data-less layer does not affect the others.

## Boundaries

- Only JPA Criteria queries are covered; `HqlWrapper`, `SqlWrapper` and native SQL need explicit conditions.
- Rules only support "field IN set" and do not handle nested associations or subqueries; convert complex permissions into field-value sets inside the provider.
- Bulk JPQL updates/deletes bypass the executor; `LogicDeletes` and `BatchOperation` do not append permission conditions, so validate the data scope before calling them.
