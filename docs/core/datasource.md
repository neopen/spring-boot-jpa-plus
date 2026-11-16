# 动态数据源与读写分离

动态数据源通过 `@DataSource` 注解与线程上下文切换连接，适合主从读写分离、按业务切换到独立库等场景。

## 启用

```yaml
neopen:
  jpa:
    datasource:
      enabled: true
      primary: primary
      read-target: read
      write-target: write
      targets:
        primary: dataSourcePrimary
        read: dataSourceRead
        write: dataSourceWrite
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.datasource.enabled` | `false` | 开启后注册路由数据源并接管 Spring Boot 默认数据源 |
| `neopen.jpa.datasource.primary` | `primary` | 默认数据源逻辑名称，未指定目标时使用 |
| `neopen.jpa.datasource.read-target` | `read` | `@DataSource(readOnly = true)` 未指定名称时使用 |
| `neopen.jpa.datasource.write-target` | `write` | `@DataSource(readOnly = false)` 未指定名称时使用 |
| `neopen.jpa.datasource.targets` | 空 | 逻辑名称到容器中数据源 Bean 名称的映射 |

`targets` 不能为空，`primary` 必须出现在 `targets` 中，否则启动失败。

## 定义目标数据源

路由数据源按 **Bean 名称** 延迟解析目标数据源，因此需要自行定义每个物理数据源：

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSourcePrimary(
            @ConfigurationProperties("demo.datasource.primary")
            DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public DataSource dataSourceRead(
            @ConfigurationProperties("demo.datasource.read")
            DataSourceProperties properties
    ) {
        return properties.initializeDataSourceBuilder().build();
    }
}
```

注意事项：

- 目标数据源 Bean 名称不能是 `dataSource`，路由数据源自身占用该名称并标记为 `@Primary`。
- 开启动态数据源后，`spring.datasource.*` 不再生效，需要为每个物理数据源提供独立配置。
- 路由数据源只负责选择连接，不会自动配置事务管理器；多数据源事务需自行声明 `JpaTransactionManager`。

## 注解切换

```java
@Service
public class OrderService {

    @DataSource(readOnly = true)
    public List<Order> listOrders() {
        return orderRepository.findAll();
    }

    @DataSource("report")
    public OrderReport report() {
        return reportRepository.build();
    }
}
```

| 声明 | 目标 |
| --- | --- |
| `@DataSource(readOnly = true)` | `read-target` 指向的数据源 |
| `@DataSource(readOnly = false)`（默认） | `write-target` 指向的数据源 |
| `@DataSource("report")` | `targets.report` 指向的数据源 |

- 注解可以放在类上，类中所有方法生效；方法注解优先于类注解。
- 切面执行顺序为 `Ordered.HIGHEST_PRECEDENCE + 100`，早于事务拦截器，因此事务内也能切换到目标数据源。
- 支持嵌套调用：内层切换只影响内层执行范围，返回后自动恢复外层数据源。

## 手动切换

```java
DataSourceContext.run("report", () -> reportRepository.rebuild());

List<Order> orders = DataSourceContext.call("read", orderRepository::findAll);

String current = DataSourceContext.current();   // 未设置时返回 null
```

| 方法 | 说明 |
| --- | --- |
| `push(String target)` / `pop()` | 压栈与出栈，手动调用时必须在 `finally` 中 `pop()` |
| `run(String target, Runnable action)` | 执行后自动出栈，异常时同样恢复 |
| `call(String target, Supplier<T> action)` | 有返回值版本 |
| `current()` / `clear()` | 读取当前目标或清空整个栈 |

上下文按线程隔离，异步任务与线程池需要自行传递目标名称。

## 与数据源作用域的区别

数据源作用域决定使用哪套连接，租户与数据权限作用域决定过滤条件，两者可以组合使用。路由次数与慢 SQL 可以通过 Micrometer 采集，详见 [指标监控](/core/metrics)。

| 能力 | 入口 | 作用 |
| --- | --- | --- |
| 动态数据源 | `@DataSource`、`DataSourceContext` | 选择物理连接 |
| 数据源作用域 | `JpaPlusScope` | 覆盖逻辑删除、租户、审计、加密等开关 |

两者可以组合：先用 `@DataSource` 选库，再用 `JpaPlusScope` 调整该库上的查询行为。
