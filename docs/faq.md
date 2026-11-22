# 常见问题

按主题汇总高频问题，每条给出结论与对应章节链接。

## 过滤链顺序

**逻辑删除、租户与数据权限的条件以什么顺序合并？**

固定顺序：逻辑删除 → 租户 → 数据权限，最终以 `AND` 连接。合并逻辑集中在 `JpaPlusSpecExecutor#effective`，不提供自定义排序。

```text
(逻辑删除条件) AND (租户条件) AND (数据权限条件)
```

每一段是否出现取决于各自开关与上下文：

- 逻辑删除：执行器 `logicDelete(false)` 时不追加；实体没有 `@LogicDelete` 时也不追加。
- 租户：`neopen.jpa.tenant.enabled=false`、当前线程跳过租户过滤，或 `TenantProvider` 返回 `null` / 空白时，不追加。
- 数据权限：`neopen.jpa.data-scope.enabled=false` 或当前线程跳过数据权限时，不追加。

需要完全自定义条件组合时，直接构造 `Specification`，或通过 `SpecWrapper#raw(...)` 追加自定义条件，不要依赖过滤链的拼接顺序。参考 [Specification 执行器](core/executor.md)、[过滤跳过](core/filter-ignore.md)。

## 事务边界

**执行器会自动开启或提交事务吗？**

不会。所有查询与批量 API 都使用调用方提供的 `EntityManager` 与事务：

- `upsertAll`、`persistAll` 等批量方法只负责分批写入与 `flush`，不提交。
- `forEachBatch` 每批回调后执行 `flush` / `clear`，同样不提交。
- 需要在 `@Transactional` 方法或手动事务中调用，并由调用方决定提交或回滚。

**`forEachBatch` 的 `clear` 会影响我的实体吗？**

`clear` 只清空当前持久化上下文的托管实例。回调内对实体做的修改会先被 `flush` 到数据库，然后实例脱离上下文；后续修改需要重新查询或合并。参考 [批量操作](core/batch.md)。

## 跳过过滤

**`@IgnoreTenant` / `@IgnoreDataScope` 为什么不生效？**

注解依赖 Spring AOP，只对容器管理的 Bean 的外部调用生效：

- 同类内部方法调用不经过代理，不会触发注解。
- `new` 出来的对象、静态方法、异步线程同样不生效。
- 异步任务请使用 `JpaPlusContextTaskDecorator`，或在线程内使用 `Tenants.runIgnored(...)` / `DataScopes.runIgnored(...)`。

**手动 `ignore()` / `restore()` 要注意什么？**

必须成对调用，按层计数；`restore()` 没有对应的 `ignore()` 会抛出 `IllegalStateException`。异常路径推荐使用 `runIgnored` / `callIgnored`，它们会在 `finally` 中自动配平。参考 [过滤跳过](core/filter-ignore.md)。

## 异步传播

**异步任务里的过滤结果为什么和提交线程不一致？**

`JpaPlusContext` 快照只传播四类状态：作用域、数据权限跳过深度、租户过滤跳过深度和数据源路由栈。租户值本身仍由 `TenantProvider` 决定，库不会复制请求上下文。异步场景需要：

1. 保证 `TenantProvider` 线程安全，或自行把租户值传播到工作线程；
2. 使用 `JpaPlusContextTaskDecorator` 在任务提交时捕获上下文。

**线程池复用会残留上下文吗？**

不会。`run` / `call` 在 `finally` 中恢复执行线程原有状态，异常退出与嵌套调用都安全。`neopen.jpa.context-propagation.enabled=false` 可关闭自动注册；容器已有自定义 `TaskDecorator` 时不覆盖。参考 [跨线程上下文传播](core/context-propagation.md)。

## 流式关闭

**`stream(...)` 必须在事务内消费吗？**

是。`Stream` 绑定当前事务与数据库游标，应在事务范围内消费完毕，并优先使用 try-with-resources 关闭：

```java
try (Stream<Order> stream = executor.stream(spec)) {
    stream.forEach(this::handle);
}
```

事务结束后连接已归还，再读取会失败。导出与批处理优先使用 `forEachBatch`，它会在异常或提前退出时关闭流。

**`forEachBatch` 如何提前结束？**

在回调内抛出异常，执行器会关闭流并原样抛出异常；没有 `break` 语义。参考 [Specification 执行器](core/executor.md)。

## 其他高频问题

**删除后数据还在数据库里？**

实体标注了 `@LogicDelete` 时，`delete` / `deleteAll` 走逻辑删除（更新删除标记），不会物理删除。需要物理删除时使用原生 SQL 或关闭该实体的逻辑删除能力。参考 [逻辑删除](core/logic-delete.md)。

**查询没有带租户条件？**

`TenantProvider#currentTenant` 返回 `null` 或空白字符串时视为没有租户上下文，库不做任何过滤。需要强制隔离时，请在业务入口保证租户一定存在。参考 [多租户](core/tenant.md)。
