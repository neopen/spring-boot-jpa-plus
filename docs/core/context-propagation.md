# 跨线程上下文传播

`JpaPlusContext` 把当前线程上的 JPA Plus 上下文捕获为不可变快照，用于传播到线程池任务或在当前线程恢复：

| 上下文 | 说明 |
| --- | --- |
| 作用域 | `JpaPlusScope` 当前绑定的作用域名称 |
| 数据权限跳过深度 | `DataScopes.ignore()` 的嵌套层数 |
| 租户过滤跳过深度 | `Tenants.ignore()` 的嵌套层数 |
| 数据源路由栈 | `DataSourceContext` 的完整栈顺序 |

租户值本身不在快照内，仍由 `TenantProvider` 决定；异步场景需要由使用方保证 `TenantProvider` 线程安全，或自行传播请求上下文。

## 基本用法

```java
JpaPlusContext snapshot = JpaPlusContext.capture();

executor.submit(() -> snapshot.run(() -> {
    // 这里的作用域、过滤开关与数据源路由与提交线程一致
    repository.findAll(spec);
}));
```

- `capture()` 复制当前状态，快照创建后不可变，可安全跨线程共享。
- `run(Runnable)` / `call(Supplier)` 执行前应用快照，执行结束后恢复工作线程原有状态，异常时同样恢复，因此线程池复用不会残留上下文。
- `apply()` 把快照精确应用到当前线程；`JpaPlusContext.clear()` 清除全部上下文，用于任务结束后的兜底清理。

## TaskDecorator

线程池推荐使用 `JpaPlusContextTaskDecorator`，在任务提交时捕获上下文：

```java
ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
executor.setTaskDecorator(new JpaPlusContextTaskDecorator());
executor.initialize();
```

自动配置在默认情况下会注册该 Bean，可直接注入：

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

自动配置不会修改已有线程池，需要业务代码主动设置装饰器。注册规则：

- `neopen.jpa.context-propagation.enabled=false` 时不再注册；
- 容器中已有 `TaskDecorator` Bean 时不做覆盖。

## 嵌套与清理

```java
JpaPlusScope.run("secondary", () -> {
    JpaPlusContext inner = JpaPlusContext.capture();

    inner.run(() -> {
        // 使用 inner 快照
    });
});

// 回到调用前的作用域
```

`run` / `call` 始终在 `finally` 中恢复执行线程原有快照，嵌套调用与异常退出都不会丢失或叠加跳过深度。

## 注意

- 快照只覆盖库自身的 ThreadLocal；新增上下文必须收敛到 `JpaPlusContext`，否则不会随快照传播。
- `run` / `call` 恢复的是**执行线程**原有状态，而不是提交线程状态；提交线程的后续修改不会影响已提交的任务。
- 回调中的异常会原样抛出，恢复逻辑不受影响。
