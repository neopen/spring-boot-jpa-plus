# Dynamic Data Sources & Read/Write Splitting

Dynamic data sources switch connections through the `@DataSource` annotation and a thread-local context, which fits primary/replica read-write splitting and switching to a dedicated database per business area.

## Enabling

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

| Property | Default | Description |
| --- | --- | --- |
| `neopen.jpa.datasource.enabled` | `false` | Registers the routing data source and takes over the Spring Boot default |
| `neopen.jpa.datasource.primary` | `primary` | Default logical name used when no target is specified |
| `neopen.jpa.datasource.read-target` | `read` | Target used by `@DataSource(readOnly = true)` without a name |
| `neopen.jpa.datasource.write-target` | `write` | Target used by `@DataSource(readOnly = false)` without a name |
| `neopen.jpa.datasource.targets` | empty | Mapping from logical names to data source bean names |

`targets` must not be empty, and `primary` must be present in `targets`; otherwise the application fails to start.

## Defining Targets

The routing data source resolves targets **by bean name** lazily, so each physical data source must be declared:

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

Notes:

- A target bean must not be named `dataSource`; the routing data source itself uses that name and is marked `@Primary`.
- Once enabled, `spring.datasource.*` no longer applies; configure each physical data source separately.
- The routing data source only selects connections and does not configure transaction managers; declare `JpaTransactionManager` yourself for multi-datasource transactions.

## Annotation-Based Switching

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

| Declaration | Target |
| --- | --- |
| `@DataSource(readOnly = true)` | The data source pointed to by `read-target` |
| `@DataSource(readOnly = false)` (default) | The data source pointed to by `write-target` |
| `@DataSource("report")` | The data source pointed to by `targets.report` |

- The annotation can be placed on a class, applying to all methods; method-level annotations take precedence.
- The aspect runs at `Ordered.HIGHEST_PRECEDENCE + 100`, before transaction interceptors, so the switch also works inside transactions.
- Nested calls are supported: an inner switch only affects the inner scope and restores the outer data source afterwards.

## Manual Switching

```java
DataSourceContext.run("report", () -> reportRepository.rebuild());

List<Order> orders = DataSourceContext.call("read", orderRepository::findAll);

String current = DataSourceContext.current();   // null when unset
```

| Method | Description |
| --- | --- |
| `push(String target)` / `pop()` | Pushes/pops the context; manual calls must `pop()` in `finally` |
| `run(String target, Runnable action)` | Pops automatically, including when the action throws |
| `call(String target, Supplier<T> action)` | Same as `run`, with a return value |
| `current()` / `clear()` | Reads the current target or clears the whole stack |

The context is thread-local; async tasks and thread pools must propagate the target name themselves.

## Difference from Scopes

A data source scope decides which connections are used, while tenant and data-permission scopes decide the filter conditions; the two can be combined. Route counts and slow SQL can be collected through Micrometer; see [Metrics](/en/core/metrics).

| Capability | Entry point | Purpose |
| --- | --- | --- |
| Dynamic data source | `@DataSource`, `DataSourceContext` | Selects the physical connection |
| Data source scope | `JpaPlusScope` | Overrides logical deletion, tenant, audit, encryption switches |

Both can be combined: use `@DataSource` to pick the database, then `JpaPlusScope` to adjust query behavior on that database.
