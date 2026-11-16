# 过滤跳过

多租户与行级数据权限默认开启后，大多数查询都应该带上过滤条件。但登录、定时任务、数据修复、后台统计等场景需要临时突破过滤。`@IgnoreTenant` 与 `@IgnoreDataScope` 提供了方法级的跳过能力。

## 注解

| 注解 | 作用 | 对应开关 |
| --- | --- | --- |
| `@IgnoreTenant` | 执行期间不追加租户条件，也不填充租户字段 | `neopen.jpa.tenant.enabled` |
| `@IgnoreDataScope` | 执行期间不追加数据权限条件 | `neopen.jpa.data-scope.enabled` |

两个注解都可以标注在方法或类上，也可以同时使用：

```java
@IgnoreTenant
public List<DocumentRecord> listAllTenants() {
    // 不追加 tenant_id 条件
    return documentRepository.findAll();
}

@IgnoreDataScope
public long countAllOrders() {
    // 不追加 DataRule 条件
    return orderRepository.count();
}

@IgnoreTenant
@IgnoreDataScope
public void repair() {
    // 同时跳过两种过滤
}
```

## 嵌套与恢复

注解按层计数，内层结束后自动恢复外层的跳过状态：

```java
@IgnoreTenant
public void outer() {
    // 跳过租户过滤
    inner();        // 内层也跳过
    // 内层结束后仍然跳过
}

@IgnoreTenant
public void inner() {
    // 跳过租户过滤
}
```

离开方法后无论正常返回还是抛出异常，都会恢复到调用前的状态。

## 代码中临时跳过

没有注解或不在代理范围内的场景（如线程池任务），可以直接使用工具方法：

```java
Tenants.runIgnored(() -> {
    // 跳过租户过滤
});

List<DocumentRecord> all = Tenants.callIgnored(
        () -> documentRepository.findAll()
);

DataScopes.runIgnored(() -> orderRepository.deleteAll());
```

| 方法 | 说明 |
| --- | --- |
| `Tenants.isIgnored()` / `DataScopes.isIgnored()` | 当前线程是否跳过过滤 |
| `ignore()` / `restore()` | 手动开始与结束一层跳过，必须成对调用 |
| `runIgnored(Runnable)` | 在跳过上下文中执行无返回值逻辑 |
| `callIgnored(Supplier<T>)` | 在跳过上下文中执行有返回值逻辑 |

`restore()` 在没有对应 `ignore()` 时会抛出 `IllegalStateException`，便于尽早发现配平错误。

## 与作用域开关的关系

注解只影响当前线程的当前调用，不会修改全局或作用域配置：

```yaml
neopen:
  jpa:
    tenant:
      enabled: true
    scopes:
      report:
        tenant-enabled: false
```

- 作用域开关是“配置态”，作用于该作用域内的所有查询。
- 注解与 `ignore()` 是“调用态”，只作用于当前线程与方法调用期间。
- 两者同时存在时，任一关闭即不追加条件。

## 适用边界

- 注解依赖 Spring AOP，只有在容器管理的 Bean 方法上才生效；同类内部调用不会触发代理。
- 过滤跳过只影响当前线程，异步线程需要自行传递上下文。
- 跳过租户过滤不会改变数据库中的租户字段，写操作仍需保证数据归属正确。
