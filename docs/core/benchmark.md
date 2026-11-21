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

## 复现

```bash
# 构建基准测试 jar（含 core）
mvn -pl spring-boot-jpa-plus-benchmarks -am package -DskipTests -Dgpg.skip=true

# 完整基准
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar

# 快速冒烟（约 1 分钟）
java -jar spring-boot-jpa-plus-benchmarks/target/jpa-plus-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 1s -r 1s
```

## 回归使用

- 对比时保持 JDK、Hibernate、数据库版本与数据量一致，并尽量在空闲机器上运行。
- 单个基准的偏差在 10% 以内视为噪声；超过 20% 再排查代码或配置变更。
- 调整数据集或过滤规则后，需要同步更新本页，避免历史数字失去可比性。
