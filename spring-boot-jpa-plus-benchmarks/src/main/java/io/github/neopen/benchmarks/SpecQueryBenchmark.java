package io.github.neopen.benchmarks;

import io.github.neopen.core.persistence.datascope.DataRule;
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
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Specification 查询基准。
 *
 * <p>对比完整过滤链与关闭过滤时的查询吞吐，数据量为
 * {@link BenchSupport#DATASET_SIZE} 行。</p>
 *
 * @author neopen
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class SpecQueryBenchmark {

    /** 实体管理器工厂。 */
    private EntityManagerFactory emf;

    /** 业务查询条件。 */
    private Specification<BenchDocument> business;

    /**
     * 准备数据与过滤链。
     */
    @Setup(Level.Trial)
    public void setUp() {
        Tenants.setProvider(() -> "tenant-a");
        DataScopes.setProvider(type -> List.of(
                DataRule.of("deptId", 1L, 2L)
        ));

        emf = BenchSupport.createEntityManagerFactory("spec-query");
        BenchSupport.insert(emf, BenchSupport.dataset(
                BenchSupport.DATASET_SIZE
        ));

        business = (root, query, cb) ->
                cb.equal(root.get("status"), "ACTIVE");
    }

    /**
     * 关闭过滤并释放资源。
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        Tenants.setProvider(null);
        DataScopes.setProvider(null);
        emf.close();
    }

    /**
     * 完整过滤链：逻辑删除 + 租户 + 数据权限。
     *
     * @return 结果行数
     */
    @Benchmark
    public int withFilters() {
        try (EntityManager em = emf.createEntityManager()) {
            return JpaPlusSpecExecutor.of(em, BenchDocument.class)
                    .findAll(business)
                    .size();
        }
    }

    /**
     * 跳过租户、数据权限与逻辑删除过滤。
     *
     * @return 结果行数
     */
    @Benchmark
    public int withoutFilters() {
        return Tenants.callIgnored(() -> DataScopes.callIgnored(() -> {
            try (EntityManager em = emf.createEntityManager()) {
                return JpaPlusSpecExecutor.of(em, BenchDocument.class)
                        .logicDelete(false)
                        .findAll(business)
                        .size();
            }
        }));
    }
}
