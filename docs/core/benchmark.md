# 性能基线

本页记录 0.8.0 在标准 H2 数据集上的 JMH 基准数据，用于版本之间的回归对比。数据来自单机、单线程环境，不代表生产数据库或业务系统的绝对性能。

## 测试环境

| 项 | 值 |
| --- | --- |
| JDK | 21.0.2（Oracle，默认 JVM 参数） |
| Spring Boot | 3.4.13 |
| Hibernate | 6.6.39.Final |
| 数据库 | H2 2.3.232 内存库 |
| JMH | 1.37，1 fork、1 线程，5×3s 预热 + 5×3s 采样 |
| 操作系统 | Windows 10，本地开发机，未做 CPU 绑核与降噪 |
| 构建 | `0.8.0-SNAPSHOT`，release 17 字节码 |

## 数据集

| 场景 | 数据 |
| --- | --- |
| 查询 / 流式 | 10,000 行；租户 `tenant-a` / `tenant-b` 均分；部门 `1..4` 均分；状态 `ACTIVE` / `CLOSED` 为 4:1；每 10 行 1 行逻辑删除 |
| 批量 upsert | 500 行已存在业务键；另 500 行新增业务键；每批 500 行 |

基准实体同时标注 `@LogicDelete` 与 `@TenantId`，数据权限规则为 `deptId in (1,2)`。

## 结果

| 基准 | 模式 | 场景 | 结果 |
| --- | --- | --- | --- |
| `FilterChainBenchmark.logicDeleteOnly` | avgt | 只合并逻辑删除条件 | 21.3 ± 2.7 ns/op |
| `FilterChainBenchmark.withAllFilters` | avgt | 逻辑删除 + 租户 + 数据权限 | 61.8 ± 16.5 ns/op |
| `SpecQueryBenchmark.withFilters` | thrpt | 完整过滤链，约 1.8k 行 | 419.1 ± 35.0 ops/s |
| `SpecQueryBenchmark.withoutFilters` | thrpt | 关闭全部过滤，约 8k 行 | 91.6 ± 15.0 ops/s |
| `StreamQueryBenchmark.streamAll` | thrpt | 流式读取 10k 行 | 64.5 ± 9.2 ops/s |
| `StreamQueryBenchmark.findAllAll` | thrpt | 一次性读取 10k 行 | 79.4 ± 12.2 ops/s |
| `BatchUpsertBenchmark.upsertInsert` | thrpt | 每批新增 500 行 | 139.5 ± 41.1 ops/s |
| `BatchUpsertBenchmark.upsertUpdate` | thrpt | 每批更新 500 行 | 102.8 ± 35.3 ops/s |

误差为 99.9% 置信区间。首轮 3×2s 预热 + 5×3s 采样的结果明显偏低（例如 `upsertInsert` 为 102 ops/s），说明吞吐类基准需要更长预热，正式数据以本表为准。

## 解读

- 过滤链组装本身是纳秒级：完整链路约 62 ns/op，相对一次数据库查询可以忽略；主要成本仍在 SQL 执行与结果集物化。
- 查询吞吐的差异来自结果集大小。完整过滤链把 10k 行收敛到约 1.8k 行，因此比关闭过滤（约 8k 行）快约 4.6 倍，不能直接理解为「过滤越快越好」。
- 流式读取的吞吐低于一次性读取，因为游标与逐行物化有额外开销；它的价值在于内存占用可控，适合导出与批处理。
- 批量 upsert 的吞吐按「每次 500 行」计：新增约 139.5 次/秒（约 7 万行/秒），更新约 102.8 次/秒（约 5.1 万行/秒）。实体使用 `IDENTITY` 主键时 Hibernate 无法合并 JDBC batch，是新增路径的主要瓶颈；更新路径则以 500 条 `UPDATE` 为主。
- H2 内存库没有网络与磁盘开销，MySQL / PostgreSQL 的绝对值会明显低于本页数据。

## 指标与日志开销

指标默认关闭：`JpaPlusMetricsAutoConfiguration` 带 `@ConditionalOnProperty(neopen.jpa.metrics.enabled=true)`，关闭时不会注册 `JpaPlusMetrics`、数据源后处理器或慢 SQL 代理，查询、过滤链与批量写入路径与未引入指标时完全一致，因此关闭状态的开销为零。

开启后，`SlowSqlMetricsDataSource` 为每个连接与语句生成 JDK 动态代理，每次 JDBC 调用增加一次反射转发，并对 `execute*` 计时。下表在同一份 10,000 行数据上对比开启前后的耗时（5×2s 预热 + 10×2s 采样，其余环境同上）：

| 基准 | 场景 | 结果 |
| --- | --- | --- |
| `MetricsOverheadBenchmark.jdbcPlain` | 未开启指标，单条 `SELECT 1` | 2.89 ± 0.18 µs/op |
| `MetricsOverheadBenchmark.jdbcWithMetrics` | 开启指标，单条 `SELECT 1` | 3.33 ± 0.39 µs/op |
| `MetricsOverheadBenchmark.jpaCountPlain` | 未开启指标，JPA 计数查询 | 10.63 ± 0.62 µs/op |
| `MetricsOverheadBenchmark.jpaCountWithMetrics` | 开启指标，JPA 计数查询 | 18.45 ± 2.69 µs/op |
| `MetricsOverheadBenchmark.jpaPagePlain` | 未开启指标，分页查询 100 个实体 | 211.31 ± 18.76 µs/op |
| `MetricsOverheadBenchmark.jpaPageWithMetrics` | 开启指标，分页查询 100 个实体 | 211.13 ± 18.81 µs/op |
| `MetricsOverheadBenchmark.recordRoute` | 记录一次数据源路由 | 118 ± 25 ns/op |
| `MetricsOverheadBenchmark.recordSlowSql` | 记录一次慢 SQL | 231 ± 23 ns/op |

解读：

- 代理成本是「每条语句的固定开销」：单条 JDBC 约增加 0.4 µs，JPA 计数查询约增加 5–8 µs，与结果集大小基本无关。
- 结果集越大，固定开销被摊得越薄：100 个实体的分页查询两次结果落在误差范围内（约 ±19 µs），指标不再是可观测的瓶颈。
- 计数查询在 H2 内存库上只需约 10 µs，固定开销占比因此偏高；换成真实数据库（一次往返数百微秒到数毫秒）后占比会降到个位数。
- 计数器写入约 0.12–0.23 µs/次，路由与慢 SQL 都按事件计数，不存在按行累加的成本。
- 慢 SQL 分支只在耗时超过 `neopen.jpa.metrics.slow-threshold-ms` 时触发；基准把阈值设为 1 秒，H2 语句不会命中。

日志方面，核心模块与自动配置在查询路径上不写业务日志，SQL 日志仍由 Hibernate 自身的 `show_sql` 与日志级别控制，库不额外增加日志点。扩展模块的 `@SlowLog` 切面按注解生效：未标注的方法不进入切面，没有开销；标注方法每次调用只增加两次 `System.nanoTime()` 与一次注解解析，只有耗时达到阈值才输出 WARN。设置 `neopen.extension.slow-log.enabled=false` 可以完全不注册该切面。

## 复现

```bash
# 构建基准测试 jar（含 core）
mvn -pl spring-boot-jpa-plus-benchmarks -am package -DskipTests -Dgpg.skip=true

# 完整基准
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar

# 快速冒烟（约 1 分钟）
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 1s -r 1s

# 指标与日志开销
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar \
  MetricsOverheadBenchmark -wi 5 -i 10 -f 1 -w 2 -r 2
```

## 回归使用

- 对比时保持 JDK、Hibernate、数据库版本与数据量一致，并尽量在空闲机器上运行。
- 单个基准的偏差在 10% 以内视为噪声；超过 20% 再排查代码或配置变更。
- 调整数据集或过滤规则后，需要同步更新本页，避免历史数字失去可比性。
