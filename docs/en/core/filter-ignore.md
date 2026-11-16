# Skipping Filters

Multi-tenancy and row-level data permission are enabled by default, so most queries should carry their filters. Login flows, scheduled jobs, data repairs and admin reports sometimes need to bypass them. `@IgnoreTenant` and `@IgnoreDataScope` provide method-level skipping.

## Annotations

| Annotation | Effect | Related switch |
| --- | --- | --- |
| `@IgnoreTenant` | Skips tenant conditions and tenant field population | `neopen.jpa.tenant.enabled` |
| `@IgnoreDataScope` | Skips data permission conditions | `neopen.jpa.data-scope.enabled` |

Both can be placed on methods or classes, and used together:

```java
@IgnoreTenant
public List<DocumentRecord> listAllTenants() {
    return documentRepository.findAll();
}

@IgnoreDataScope
public long countAllOrders() {
    return orderRepository.count();
}

@IgnoreTenant
@IgnoreDataScope
public void repair() {
    // Both filters are skipped
}
```

## Nesting

Skipping is counted per layer, so the outer state is restored after a nested call:

```java
@IgnoreTenant
public void outer() {
    inner();    // also skipped
    // still skipped here
}

@IgnoreTenant
public void inner() {
}
```

The previous state is restored on both normal return and exception.

## Programmatic Use

Use the utility methods when AOP does not apply, for example inside thread pools:

```java
Tenants.runIgnored(() -> {
    // Tenant filter skipped
});

List<DocumentRecord> all = Tenants.callIgnored(
        () -> documentRepository.findAll()
);

DataScopes.runIgnored(() -> orderRepository.deleteAll());
```

| Method | Description |
| --- | --- |
| `Tenants.isIgnored()` / `DataScopes.isIgnored()` | Whether the current thread skips the filter |
| `ignore()` / `restore()` | Start and finish one layer; must be paired |
| `runIgnored(Runnable)` | Run logic with the filter skipped |
| `callIgnored(Supplier<T>)` | Run logic with the filter skipped and return a value |

`restore()` throws `IllegalStateException` when there is no matching `ignore()`, making unbalanced calls easy to spot.

## Relationship With Scope Switches

Annotations only affect the current call on the current thread; they never change global or scoped configuration:

```yaml
neopen:
  jpa:
    tenant:
      enabled: true
    scopes:
      report:
        tenant-enabled: false
```

- Scope switches are configuration; they apply to every query in that scope.
- Annotations and `ignore()` are call-level; they apply to the current thread and invocation only.
- When both exist, a filter is skipped if either one disables it.

## Boundaries

- Annotations rely on Spring AOP and only work on container-managed bean methods; self-invocation bypasses the proxy.
- Skipping affects the current thread only; propagate context manually to async threads.
- Skipping the filter does not change stored tenant values, so writes must still set ownership correctly.
