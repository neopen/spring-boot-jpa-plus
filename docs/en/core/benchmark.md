# Performance Baseline

This page records JMH benchmark data for 0.8.0 on a standard H2 dataset. It is intended for regression comparison between versions, not as an absolute performance claim for production databases or applications.

## Environment

| Item | Value |
| --- | --- |
| JDK | 21.0.2 (Oracle, default JVM options) |
| Spring Boot | 3.4.13 |
| Hibernate | 6.6.39.Final |
| Database | H2 2.3.232 in-memory |
| JMH | 1.37, 1 fork, 1 thread, 5×3s warmup + 5×3s measurement |
| OS | Windows 10, local developer machine, no CPU pinning or noise reduction |
| Build | `0.8.0-SNAPSHOT`, release 17 bytecode |

## Dataset

| Scenario | Data |
| --- | --- |
| Query / stream | 10,000 rows; tenants `tenant-a` / `tenant-b` split evenly; departments `1..4` split evenly; status `ACTIVE` / `CLOSED` at 4:1; every 10th row logically deleted |
| Batch upsert | 500 existing business keys and 500 new business keys; 500 rows per batch |

The benchmark entity is annotated with both `@LogicDelete` and `@TenantId`, and the data permission rule is `deptId in (1,2)`.

## Results

| Benchmark | Mode | Scenario | Result |
| --- | --- | --- | --- |
| `FilterChainBenchmark.logicDeleteOnly` | avgt | Merge the logical-delete predicate only | 21.3 ± 2.7 ns/op |
| `FilterChainBenchmark.withAllFilters` | avgt | Logical delete + tenant + data permission | 61.8 ± 16.5 ns/op |
| `SpecQueryBenchmark.withFilters` | thrpt | Full filter chain, about 1.8k rows | 419.1 ± 35.0 ops/s |
| `SpecQueryBenchmark.withoutFilters` | thrpt | All filters disabled, about 8k rows | 91.6 ± 15.0 ops/s |
| `StreamQueryBenchmark.streamAll` | thrpt | Stream 10k rows | 64.5 ± 9.2 ops/s |
| `StreamQueryBenchmark.findAllAll` | thrpt | Load 10k rows into a list | 79.4 ± 12.2 ops/s |
| `BatchUpsertBenchmark.upsertInsert` | thrpt | Insert 500 rows per batch | 139.5 ± 41.1 ops/s |
| `BatchUpsertBenchmark.upsertUpdate` | thrpt | Update 500 rows per batch | 102.8 ± 35.3 ops/s |

Errors are 99.9% confidence intervals. An earlier run with 3×2s warmup + 5×3s measurement produced visibly lower throughput (for example, 102 ops/s for `upsertInsert`), which indicates that throughput benchmarks need a longer warmup. The table above is the reference data.

## Interpretation

- Composing the filter chain costs nanoseconds: about 62 ns/op for the full chain, negligible compared with a database query. SQL execution and result materialization dominate.
- The query throughput gap comes from result-set size. The full filter chain narrows 10k rows to about 1.8k, roughly 4.6× faster than the unfiltered query (about 8k rows); this is not a direct measure of filter overhead.
- Streaming has lower throughput than loading a list because of cursor management and row-by-row materialization. Its value is bounded memory usage for exports and batch jobs.
- Batch upsert throughput is measured per 500 rows: about 139.5 inserts/s (about 70k rows/s) and 102.8 updates/s (about 51k rows/s). With an `IDENTITY` primary key Hibernate cannot merge inserts into a JDBC batch, which dominates the insert path; the update path issues 500 `UPDATE` statements.
- The H2 in-memory database has no network or disk latency, so MySQL / PostgreSQL numbers will be noticeably lower.

## Metrics and logging overhead

Metrics are off by default: `JpaPlusMetricsAutoConfiguration` is guarded by `@ConditionalOnProperty(neopen.jpa.metrics.enabled=true)`. When disabled, no `JpaPlusMetrics`, data-source post-processor, or slow-SQL proxy is registered, so the query, filter-chain, and batch paths are identical to a build without the metrics feature. Overhead in the disabled state is therefore zero.

When enabled, `SlowSqlMetricsDataSource` creates a JDK dynamic proxy for every connection and statement, adding one reflective delegation per JDBC call and timing each `execute*` invocation. The table below compares both states on the same 10,000-row dataset (5×2s warmup + 10×2s measurement; all other conditions as above):

| Benchmark | Scenario | Result |
| --- | --- | --- |
| `MetricsOverheadBenchmark.jdbcPlain` | Metrics off, single `SELECT 1` | 2.89 ± 0.18 µs/op |
| `MetricsOverheadBenchmark.jdbcWithMetrics` | Metrics on, single `SELECT 1` | 3.33 ± 0.39 µs/op |
| `MetricsOverheadBenchmark.jpaCountPlain` | Metrics off, JPA count query | 10.63 ± 0.62 µs/op |
| `MetricsOverheadBenchmark.jpaCountWithMetrics` | Metrics on, JPA count query | 18.45 ± 2.69 µs/op |
| `MetricsOverheadBenchmark.jpaPagePlain` | Metrics off, page query of 100 entities | 211.31 ± 18.76 µs/op |
| `MetricsOverheadBenchmark.jpaPageWithMetrics` | Metrics on, page query of 100 entities | 211.13 ± 18.81 µs/op |
| `MetricsOverheadBenchmark.recordRoute` | Record one data-source route | 118 ± 25 ns/op |
| `MetricsOverheadBenchmark.recordSlowSql` | Record one slow SQL statement | 231 ± 23 ns/op |

Interpretation:

- Proxy cost is a fixed per-statement overhead: about 0.4 µs for a single JDBC statement and 5–8 µs for a JPA query, largely independent of result-set size.
- Larger result sets amortize that fixed cost: the 100-entity page query lands within the error bars (about ±19 µs), so metrics are no longer an observable bottleneck.
- The count query needs only about 10 µs on the in-memory H2 database, which inflates the relative share; against a real database (hundreds of microseconds to milliseconds per round trip) the share drops to single digits.
- Writing a counter costs about 0.12–0.23 µs per event. Routes and slow SQL are counted per event, never per row.
- The slow-SQL branch runs only when a statement exceeds `neopen.jpa.metrics.slow-threshold-ms`; the benchmark sets it to one second, which H2 statements never reach.

For logging, the core and autoconfigure modules do not emit business logs on the query path, and SQL logging is still controlled by Hibernate's own `show_sql` and log levels. The extension module's `@SlowLog` aspect is annotation-driven: unannotated methods never enter the advice and pay nothing, while annotated methods add two `System.nanoTime()` calls and one annotation lookup per invocation, emitting a WARN only when the threshold is exceeded. Setting `neopen.extension.slow-log.enabled=false` skips registering the aspect entirely.

## Reproduce

```bash
# Build the benchmark jar (including core)
mvn -pl spring-boot-jpa-plus-benchmarks -am package -DskipTests -Dgpg.skip=true

# Full benchmark
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar

# Quick smoke run (about one minute)
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 1s -r 1s

# Metrics and logging overhead
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar \
  MetricsOverheadBenchmark -wi 5 -i 10 -f 1 -w 2 -r 2
```

## Regression Guidance

- Keep the JDK, Hibernate, database version, and dataset identical when comparing, and run on an idle machine.
- Treat a single-benchmark difference within 10% as noise; investigate code or configuration changes beyond 20%.
- Update this page whenever the dataset or filter rules change so historical numbers stay comparable.
