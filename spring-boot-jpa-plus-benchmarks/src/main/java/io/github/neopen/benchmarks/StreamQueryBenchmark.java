package io.github.neopen.benchmarks;

import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import io.github.neopen.core.persistence.tenant.Tenants;
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

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 流式查询基准。
 *
 * <p>对比 {@link JpaPlusSpecExecutor#stream} 与
 * {@link JpaPlusSpecExecutor#findAll} 消费
 * {@link BenchSupport#DATASET_SIZE} 行数据的吞吐。</p>
 *
 * @author neopen
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class StreamQueryBenchmark {

    /** 实体管理器工厂。 */
    private EntityManagerFactory emf;

    /**
     * 准备数据。
     */
    @Setup(Level.Trial)
    public void setUp() {
        emf = BenchSupport.createEntityManagerFactory("stream-query");
        BenchSupport.insert(emf, BenchSupport.dataset(
                BenchSupport.DATASET_SIZE
        ));
    }

    /**
     * 释放资源。
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        emf.close();
    }

    /**
     * 流式读取全部数据。
     *
     * <p>关闭过滤链以覆盖完整数据集，单独度量流式读取本身。</p>
     *
     * @return 消费的行数
     */
    @Benchmark
    public long streamAll() {
        return Tenants.callIgnored(() -> DataScopes.callIgnored(() -> {
            try (EntityManager em = emf.createEntityManager()) {
                em.getTransaction().begin();

                try (Stream<BenchDocument> stream =
                             JpaPlusSpecExecutor.of(em, BenchDocument.class)
                                     .logicDelete(false)
                                     .stream(null)) {
                    long count = stream.count();
                    em.getTransaction().commit();
                    return count;
                }
            }
        }));
    }

    /**
     * 一次性读取全部数据到列表。
     *
     * @return 读取的行数
     */
    @Benchmark
    public int findAllAll() {
        return Tenants.callIgnored(() -> DataScopes.callIgnored(() -> {
            try (EntityManager em = emf.createEntityManager()) {
                return JpaPlusSpecExecutor.of(em, BenchDocument.class)
                        .logicDelete(false)
                        .findAll(null)
                        .size();
            }
        }));
    }
}
