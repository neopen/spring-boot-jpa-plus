/*
 * Copyright 2026 neopen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.benchmarks;

import io.github.neopen.core.persistence.batch.BatchOperation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 批量写入基准。
 *
 * <p>覆盖 {@link BatchOperation#upsertAll} 的新增与更新两条路径，
 * 每批 {@link BenchSupport#UPSERT_SIZE} 行。</p>
 *
 * @author neopen
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class BatchUpsertBenchmark {

    /** 已存在业务键的前缀。 */
    private static final String EXISTING_PREFIX = "upd-";

    /** 每次新增业务键的前缀。 */
    private static final String INSERT_PREFIX = "new-";

    /** 实体管理器工厂。 */
    private EntityManagerFactory emf;

    /** 单调递增的负载序号，保证更新路径能触发脏检查。 */
    private long sequence;

    /**
     * 准备已存在的业务键。
     */
    @Setup(Level.Trial)
    public void setUp() {
        emf = BenchSupport.createEntityManagerFactory("batch-upsert");

        List<BenchDocument> existing = BenchSupport.documents(
                EXISTING_PREFIX,
                BenchSupport.UPSERT_SIZE,
                "initial"
        );

        BenchSupport.insert(emf, existing);
    }

    /**
     * 清理上一轮新增的数据，使新增基准可重复执行。
     */
    @Setup(Level.Invocation)
    public void clearInserted() {
        BenchSupport.deleteByPrefix(emf, INSERT_PREFIX);
    }

    /**
     * 释放资源。
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        emf.close();
    }

    /**
     * 新增路径：500 条全新业务键。
     *
     * @return 新增行数
     */
    @Benchmark
    public int upsertInsert() {
        List<BenchDocument> batch = BenchSupport.documents(
                INSERT_PREFIX,
                BenchSupport.UPSERT_SIZE,
                "payload-" + (++sequence)
        );

        return upsert(batch).inserted();
    }

    /**
     * 更新路径：500 条已存在业务键。
     *
     * @return 更新行数
     */
    @Benchmark
    public int upsertUpdate() {
        List<BenchDocument> batch = BenchSupport.documents(
                EXISTING_PREFIX,
                BenchSupport.UPSERT_SIZE,
                "payload-" + (++sequence)
        );

        return upsert(batch).updated();
    }

    /**
     * 执行一次批量 upsert。
     *
     * @param batch 待处理实体
     * @return 处理结果
     */
    private BatchOperation.UpsertResult upsert(List<BenchDocument> batch) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            BatchOperation.UpsertResult result = BatchOperation.upsertAll(
                    em,
                    BenchDocument.class,
                    batch,
                    BenchDocument::getDocNo,
                    "docNo",
                    BenchSupport.BATCH_SIZE
            );

            em.getTransaction().commit();

            return result;
        }
    }
}
