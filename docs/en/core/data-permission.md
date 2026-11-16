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

// Association path: filter by the department's parent
DataRule nested = DataRule.of("dept.parentId", 1L);

// OR combination: own department or records owned by me
List<DataRule> rules = List.of(
        DataRule.of("deptId", 1L, 2L),
        DataRule.or("ownerId", 9L)
);
// deptId in (1,2) or ownerId in (9)
```

| Constraint | Description |
| --- | --- |
| `field` | Entity field name; association paths such as `dept.parentId` are supported, and it must not be blank |
| `values` | Allowed values, must not be empty |
| `logic` | How the rule combines with the previous one; defaults to `AND`, use `DataRule.or(...)` for `OR` |
| Empty rule | Returning `null` or an empty list means no restriction |

Rules are combined left to right in declaration order, so `a OR b AND c` means `(a OR b) AND c`. When you need more complex precedence, convert the conditions into field-value sets inside the provider.

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

## Skipping Filters

Background statistics or data-repair jobs can skip permission conditions per method:

```java
@IgnoreDataScope
public long countAllOrders() {
    return orderRepository.count();
}
```

Nested calls restore level by level, and `DataScopes.runIgnored(...)` / `callIgnored(...)` skip the filter programmatically. See [Skipping Filters](/en/core/filter-ignore) for details.

## Boundaries

- Only JPA Criteria queries are covered; `HqlWrapper`, `SqlWrapper` and native SQL need explicit conditions.
- Rules support `field IN set`, association paths and `OR` combinations, but not subqueries; convert complex permissions into field-value sets inside the provider.
- Bulk JPQL updates/deletes bypass the executor; `LogicDeletes` and `BatchOperation` do not append permission conditions, so validate the data scope before calling them.
