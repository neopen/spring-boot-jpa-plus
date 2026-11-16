# 指标监控

`spring-boot-jpa-plus` 可选接入 Micrometer，输出数据源路由次数与慢 SQL 次数。未引入 Micrometer 的项目不会产生任何额外依赖，也不会注册相关 Bean。

## 开启

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

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.metrics.enabled` | `false` | 是否启用指标采集 |
| `neopen.jpa.metrics.slow-threshold-ms` | `1000` | 超过该耗时的 SQL 计入慢 SQL |

需要容器中存在 `MeterRegistry`（引入 `spring-boot-starter-actuator` 即可）。

## 指标

| 指标名 | 类型 | 标签 | 说明 |
| --- | --- | --- | --- |
| `jpa.plus.datasource.routes` | Counter | `target` | 数据源被路由的次数，`default` 表示主数据源 |
| `jpa.plus.sql.slow` | Counter | `type` | 慢 SQL 次数，`type` 为 `SELECT`、`INSERT` 等语句类型 |

`type` 只保留语句动词，避免把 SQL 文本作为标签造成高基数。

## 慢 SQL 采集方式

开启指标后，自动配置会给容器中的数据源套上 `SlowSqlMetricsDataSource`：通过 JDBC 动态代理统计语句执行耗时，超过阈值的语句计入 `jpa.plus.sql.slow`。

已有 `JpaPlusRoutingDataSource` 时，路由次数由路由数据源记录，慢 SQL 由底层目标数据源记录，两者不会重复计数。

也可以手动包装：

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

## 代码中记录

```java
JpaPlusMetrics metrics = ...;

metrics.recordRoute("read");
metrics.recordSlowSql("select * from orders");
```

## 适用边界

- 慢 SQL 统计在 JDBC 层完成，包含 Connection 获取之后的语句执行耗时，不含事务提交与网络重试。
- 数据源包装只在启用指标时发生；关闭指标后数据源保持原样。
- 指标只做计数，不记录 SQL 文本，避免敏感信息进入监控系统。
