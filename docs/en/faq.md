# FAQ

Answers to frequently asked questions, grouped by topic. Each answer links to the relevant chapter.

## Filter-chain order

**In what order are logical-delete, tenant, and data-permission predicates merged?**

Fixed order: logical delete → tenant → data permission, joined with `AND`. The merge lives in `JpaPlusSpecExecutor#effective` and the order is not configurable.

```text
(logical delete) AND (tenant) AND (data permission)
```

Each part appears only when its own switch and context allow it:

- Logical delete: skipped when the executor is created with `logicDelete(false)`, or when the entity has no `@LogicDelete`.
- Tenant: skipped when `neopen.jpa.tenant.enabled=false`, when the current thread ignores tenant filtering, or when `TenantProvider` returns `null` / blank.
- Data permission: skipped when `neopen.jpa.data-scope.enabled=false` or when the current thread ignores data permission.

For fully custom combinations, build the `Specification` yourself or append conditions with `SpecWrapper#raw(...)` instead of relying on the chain order. See [Specification executor](core/executor.md) and [Skipping filters](core/filter-ignore.md).

## Transaction boundaries

**Does the executor open or commit transactions?**

No. Every query and batch API uses the caller's `EntityManager` and transaction:

- `upsertAll`, `persistAll`, and other batch methods only write in batches and `flush`; they never commit.
- `forEachBatch` runs `flush` / `clear` after each batch and never commits.
- Call them inside a `@Transactional` method or a manual transaction, and let the caller decide to commit or roll back.

**Does `clear` in `forEachBatch` affect my entities?**

`clear` only detaches managed instances from the current persistence context. Changes made inside the callback are flushed first, then the instances become detached; later changes require a re-query or merge. See [Batch operations](core/batch.md).

## Skipping filters

**Why do `@IgnoreTenant` / `@IgnoreDataScope` have no effect?**

The annotations rely on Spring AOP and only apply to external calls on container-managed beans:

- Self-invocation inside the same class bypasses the proxy.
- Objects created with `new`, static methods, and async threads are not affected.
- For async work use `JpaPlusContextTaskDecorator`, or call `Tenants.runIgnored(...)` / `DataScopes.runIgnored(...)` inside the thread.

**What should I watch out for with manual `ignore()` / `restore()`?**

They must be paired and are counted per nesting level; `restore()` without a matching `ignore()` throws `IllegalStateException`. Prefer `runIgnored` / `callIgnored`, which balance the depth in `finally`. See [Skipping filters](core/filter-ignore.md).

## Async propagation

**Why do filters behave differently inside an async task?**

A `JpaPlusContext` snapshot carries only four pieces of state: scope, data-permission ignore depth, tenant ignore depth, and the data-source routing stack. Tenant values still come from `TenantProvider`; the library does not copy request context. For async work:

1. Make `TenantProvider` thread-safe, or propagate the tenant value to the worker thread yourself.
2. Use `JpaPlusContextTaskDecorator` to capture the context when the task is submitted.

**Can a reused thread pool leak context?**

No. `run` / `call` restore the executing thread's previous state in `finally`, including nested calls and exceptional exits. Set `neopen.jpa.context-propagation.enabled=false` to skip auto-registration; an existing custom `TaskDecorator` is never overridden. See [Context propagation](core/context-propagation.md).

## Closing streams

**Must `stream(...)` be consumed inside a transaction?**

Yes. The `Stream` is bound to the current transaction and database cursor. Consume it within the transaction and prefer try-with-resources:

```java
try (Stream<Order> stream = executor.stream(spec)) {
    stream.forEach(this::handle);
}
```

After the transaction ends the connection is returned and further reads fail. Prefer `forEachBatch` for exports and batch jobs; it closes the stream on failure or early exit.

**How do I stop `forEachBatch` early?**

Throw an exception from the callback. The executor closes the stream and rethrows it; there is no `break` semantics. See [Specification executor](core/executor.md).

## Other common questions

**Why is deleted data still in the database?**

When the entity is annotated with `@LogicDelete`, `delete` / `deleteAll` perform a logical delete (update the flag) instead of a physical delete. Use native SQL or disable logical deletion for that entity if you need a physical delete. See [Logical deletion](core/logic-delete.md).

**Why is no tenant predicate added?**

When `TenantProvider#currentTenant` returns `null` or a blank string, there is no tenant context and the library adds no filter. Enforce a tenant at the business entry point if isolation is required. See [Multi-tenancy](core/tenant.md).
