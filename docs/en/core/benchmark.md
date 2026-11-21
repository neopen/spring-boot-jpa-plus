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

## Reproduce

```bash
# Build the benchmark jar (including core)
mvn -pl spring-boot-jpa-plus-benchmarks -am package -DskipTests -Dgpg.skip=true

# Full benchmark
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar

# Quick smoke run (about one minute)
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 1s -r 1s
```

## Regression Guidance

- Keep the JDK, Hibernate, database version, and dataset identical when comparing, and run on an idle machine.
- Treat a single-benchmark difference within 10% as noise; investigate code or configuration changes beyond 20%.
- Update this page whenever the dataset or filter rules change so historical numbers stay comparable.
