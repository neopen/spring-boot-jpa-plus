# Cross-Thread Context Propagation

`JpaPlusContext` captures the JPA Plus context of the current thread as an immutable snapshot, so it can be propagated to thread-pool tasks or restored on the current thread:

| Context | Description |
| --- | --- |
| Scope | The name bound by `JpaPlusScope` |
| Data-permission ignore depth | Nesting depth of `DataScopes.ignore()` |
| Tenant-filter ignore depth | Nesting depth of `Tenants.ignore()` |
| Data-source routing stack | Full `DataSourceContext` stack order |

The tenant value itself is not part of the snapshot: it is still resolved by `TenantProvider`. In async scenarios the caller must make `TenantProvider` thread-safe or propagate the request context itself.

## Basic Usage

```java
JpaPlusContext snapshot = JpaPlusContext.capture();

executor.submit(() -> snapshot.run(() -> {
    // Scope, filter switches and routing match the submitting thread
    repository.findAll(spec);
}));
```

- `capture()` copies the current state; the snapshot is immutable and safe to share across threads.
- `run(Runnable)` / `call(Supplier)` apply the snapshot before execution and restore the worker thread's previous state afterwards, including when an exception is thrown, so pooled threads never leak context.
- `apply()` installs the snapshot exactly; `JpaPlusContext.clear()` removes all context and is useful as a final cleanup after a task.

## TaskDecorator

For thread pools, use `JpaPlusContextTaskDecorator` to capture context at submission time:

```java
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
executor.setTaskDecorator(new JpaPlusContextTaskDecorator());
executor.initialize();
```

Auto-configuration registers the bean by default, so it can be injected directly:

```java
@Bean
ThreadPoolTaskExecutor applicationTaskExecutor(
        TaskDecorator taskDecorator
) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setTaskDecorator(taskDecorator);
    executor.initialize();
    return executor;
}
```

Auto-configuration never modifies existing thread pools: set the decorator explicitly in your own code. Registration rules:

- Nothing is registered when `neopen.jpa.context-propagation.enabled=false`;
- An existing `TaskDecorator` bean is never overridden.

## Nesting and Cleanup

```java
JpaPlusScope.run("secondary", () -> {
    JpaPlusContext inner = JpaPlusContext.capture();

    inner.run(() -> {
        // Uses the inner snapshot
    });
});

// Back to the previous scope
```

`run` / `call` always restore the previous snapshot of the executing thread in a `finally` block, so nested calls and exceptions neither lose nor stack ignore depths.

## Notes

- Snapshots only cover the library's own thread locals; new context state must be folded into `JpaPlusContext` or it will not be propagated.
- `run` / `call` restore the **executing** thread's previous state, not the submitting thread's state; later changes on the submitting thread do not affect submitted tasks.
- Exceptions from the action are rethrown unchanged; restoration is not affected.
