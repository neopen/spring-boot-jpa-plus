package io.github.neopen.benchmarks;

import io.github.neopen.core.persistence.datascope.DataRule;
import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import io.github.neopen.core.persistence.tenant.Tenants;
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
 * 过滤链组装开销基准。
 *
 * <p>只度量 {@link LogicDeletes}/{@link Tenants}/{@link DataScopes}
 * 合并查询条件的成本，不访问数据库，因此结果反映执行器每次查询
 * 固定附加的静态开销。</p>
 *
 * @author neopen
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class FilterChainBenchmark {

    /** 业务查询条件。 */
    private Specification<BenchDocument> business;

    /**
     * 注册租户与数据权限提供者。
     */
    @Setup(Level.Trial)
    public void setUp() {
        Tenants.setProvider(() -> "tenant-a");
        DataScopes.setProvider(type -> List.of(
                DataRule.of("deptId", 1L, 2L)
        ));

        business = (root, query, cb) ->
                cb.equal(root.get("status"), "ACTIVE");
    }

    /**
     * 清理全局提供者，避免影响其它基准类。
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        Tenants.setProvider(null);
        DataScopes.setProvider(null);
    }

    /**
     * 仅追加逻辑删除条件。
     *
     * @return 合并后的查询条件
     */
    @Benchmark
    public Specification<BenchDocument> logicDeleteOnly() {
        return LogicDeletes.filter(BenchDocument.class, business);
    }

    /**
     * 依次追加逻辑删除、租户与数据权限条件。
     *
     * @return 合并后的查询条件
     */
    @Benchmark
    public Specification<BenchDocument> withAllFilters() {
        Specification<BenchDocument> merged = LogicDeletes.filter(
                BenchDocument.class,
                business
        );

        merged = Tenants.filter(BenchDocument.class, merged);

        return DataScopes.filter(BenchDocument.class, merged);
    }
}
