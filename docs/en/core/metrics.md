# Metrics

`spring-boot-jpa-plus` optionally integrates with Micrometer to expose data source routing counts and slow SQL counts. Projects without Micrometer get no extra dependency and no extra beans.

## Enable

```yaml
neopen:
  jpa:
    metrics:
      enabled: true
      slow-threshold-ms: 1000
```

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>
```

| Property | Default | Description |
| --- | --- | --- |
| `neopen.jpa.metrics.enabled` | `false` | Enables metric collection |
| `neopen.jpa.metrics.slow-threshold-ms` | `1000` | Statements slower than this are counted |

A `MeterRegistry` bean must be present, which `spring-boot-starter-actuator` provides.

## Metrics

| Metric | Type | Tag | Description |
| --- | --- | --- | --- |
| `jpa.plus.datasource.routes` | Counter | `target` | Routing count per data source; `default` is the primary one |
| `jpa.plus.sql.slow` | Counter | `type` | Slow SQL count; `type` is `SELECT`, `INSERT`, etc. |

Only the statement verb is used as a tag, keeping cardinality low.

## How Slow SQL Is Collected

When metrics are enabled, the auto-configuration wraps the container `DataSource` with `SlowSqlMetricsDataSource`, which times JDBC statement execution through a dynamic proxy and counts statements above the threshold.

With `JpaPlusRoutingDataSource`, routing counts are recorded by the routing data source and slow SQL by the target data sources, so nothing is counted twice.

Manual wrapping is also supported:

```java
@Bean
public DataSource dataSource(
        DataSourceProperties properties,
        JpaPlusMetrics metrics
) {
    DataSource target = properties
            .initializeDataSourceBuilder()
            .build();

    return new SlowSqlMetricsDataSource(target, metrics, 1000L);
}
```

## Recording Manually

```java
JpaPlusMetrics metrics = ...;

metrics.recordRoute("read");
metrics.recordSlowSql("select * from orders");
```

## Boundaries

- Timing happens at the JDBC layer, covering statement execution only, not transaction commit or network retries.
- The data source is wrapped only when metrics are enabled; otherwise it stays untouched.
- Metrics are counters only; SQL text is never recorded, keeping sensitive data out of monitoring systems.
