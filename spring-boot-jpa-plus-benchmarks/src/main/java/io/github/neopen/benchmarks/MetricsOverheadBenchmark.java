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

import com.zaxxer.hikari.HikariDataSource;
import io.github.neopen.autoconfigure.JpaPlusMetrics;
import io.github.neopen.autoconfigure.SlowSqlMetricsDataSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 指标开销基准。
 *
 * <p>对比开启 {@code neopen.jpa.metrics} 前后单语句 JDBC 与 JPA 查询的
 * 平均耗时，并单独度量两个计数器的写入成本。指标关闭时自动配置不注册
 * 任何 Bean，运行期不存在代理与计数逻辑，开销为零。</p>
 *
 * <p>为了把指标开销与连接创建成本区分开，两个数据源共享同一个 Hikari
 * 连接池；指标数据源只在最外层增加 {@link SlowSqlMetricsDataSource}
 * 动态代理。慢 SQL 阈值取 1 秒，H2 语句不会触发计数，慢 SQL 分支的
 * 成本由 {@link #recordSlowSql()} 单独度量。</p>
 *
 * @author neopen
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class MetricsOverheadBenchmark {

    /** 指标 JPA 查询语句。 */
    private static final String COUNT_JPQL =
            "select count(e) from BenchDocument e where e.status = :status";

    /** 单语句 JDBC 查询。 */
    private static final String SELECT_ONE = "select 1";

    /** 分页查询返回的实体数量。 */
    private static final int PAGE_SIZE = 100;

    /** 分页实体查询语句。 */
    private static final String PAGE_JPQL = "select e from BenchDocument e"
            + " where e.status = :status order by e.id";

    /** 慢 SQL 阈值毫秒数：H2 语句不会超过该值。 */
    private static final long SLOW_THRESHOLD_MS = 1_000L;

    /** 内存库连接串，与持久化单元保持一致。 */
    private static final String URL =
            "jdbc:h2:mem:metrics-overhead;DB_CLOSE_DELAY=-1;"
                    + "DB_CLOSE_ON_EXIT=FALSE";

    /** 未包装的数据源。 */
    private DataSource plainDataSource;

    /** 连接池，两个数据源共享。 */
    private HikariDataSource pool;

    /** 包装了慢 SQL 指标的数据源。 */
    private DataSource metricsDataSource;

    /** 指标收集器。 */
    private JpaPlusMetrics metrics;

    /** 未包装的实体管理器工厂。 */
    private EntityManagerFactory plainEmf;

    /** 使用指标数据源的实体管理器工厂。 */
    private EntityManagerFactory metricsEmf;

    /**
     * 准备数据源、数据集与两个实体管理器工厂。
     */
    @Setup(Level.Trial)
    public void setUp() {
        pool = new HikariDataSource();
        pool.setJdbcUrl(URL);
        pool.setUsername("sa");
        pool.setPassword("");
        pool.setMaximumPoolSize(2);
        pool.setMinimumIdle(0);
        pool.setPoolName("metrics-overhead");
        plainDataSource = new NoCredentialsDataSource(pool);

        metrics = new JpaPlusMetrics(new SimpleMeterRegistry());
        metricsDataSource = new SlowSqlMetricsDataSource(
                plainDataSource,
                metrics,
                SLOW_THRESHOLD_MS
        );

        plainEmf = BenchSupport.createEntityManagerFactory(
                plainDataSource,
                "create-drop"
        );
        BenchSupport.insert(
                plainEmf,
                BenchSupport.dataset(BenchSupport.DATASET_SIZE)
        );
        metricsEmf = BenchSupport.createEntityManagerFactory(
                metricsDataSource,
                "none"
        );
    }

    /**
     * 关闭实体管理器工厂与连接池。
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        plainEmf.close();
        metricsEmf.close();
        pool.close();
    }

    /**
     * 未开启指标时的单语句 JDBC 查询。
     *
     * @return 查询结果
     * @throws SQLException 执行失败时抛出
     */
    @Benchmark
    public long jdbcPlain() throws SQLException {
        try (Connection connection = plainDataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SELECT_ONE);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();

            return resultSet.getLong(1);
        }
    }

    /**
     * 开启指标后的单语句 JDBC 查询。
     *
     * @return 查询结果
     * @throws SQLException 执行失败时抛出
     */
    @Benchmark
    public long jdbcWithMetrics() throws SQLException {
        try (Connection connection = metricsDataSource.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(SELECT_ONE);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();

            return resultSet.getLong(1);
        }
    }

    /**
     * 未开启指标时的 JPA 计数查询。
     *
     * @return 计数结果
     */
    @Benchmark
    public long jpaCountPlain() {
        try (EntityManager em = plainEmf.createEntityManager()) {
            return em.createQuery(COUNT_JPQL, Long.class)
                    .setParameter("status", "ACTIVE")
                    .getSingleResult();
        }
    }

    /**
     * 开启指标后的 JPA 计数查询。
     *
     * @return 计数结果
     */
    @Benchmark
    public long jpaCountWithMetrics() {
        try (EntityManager em = metricsEmf.createEntityManager()) {
            return em.createQuery(COUNT_JPQL, Long.class)
                    .setParameter("status", "ACTIVE")
                    .getSingleResult();
        }
    }

    /**
     * 未开启指标时的 JPA 分页实体查询。
     *
     * @return 结果行数
     */
    @Benchmark
    public int jpaPagePlain() {
        try (EntityManager em = plainEmf.createEntityManager()) {
            return em.createQuery(PAGE_JPQL, BenchDocument.class)
                    .setParameter("status", "ACTIVE")
                    .setMaxResults(PAGE_SIZE)
                    .getResultList()
                    .size();
        }
    }

    /**
     * 开启指标后的 JPA 分页实体查询。
     *
     * @return 结果行数
     */
    @Benchmark
    public int jpaPageWithMetrics() {
        try (EntityManager em = metricsEmf.createEntityManager()) {
            return em.createQuery(PAGE_JPQL, BenchDocument.class)
                    .setParameter("status", "ACTIVE")
                    .setMaxResults(PAGE_SIZE)
                    .getResultList()
                    .size();
        }
    }

    /**
     * 记录一次数据源路由。
     *
     * @return 固定值，避免基准被优化掉
     */
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int recordRoute() {
        metrics.recordRoute("primary");

        return 1;
    }

    /**
     * 记录一次慢 SQL。
     *
     * @return 固定值，避免基准被优化掉
     */
    @Benchmark
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int recordSlowSql() {
        metrics.recordSlowSql(
                "select payload from bench_document where id = 1"
        );

        return 1;
    }

    /**
     * 忽略显式账号的数据源。
     *
     * <p>Hibernate 在持久化单元配置了账号时会调用
     * {@code getConnection(user, password)}，而 Hikari 基于
     * {@code jdbcUrl} 的配置不支持该重载，这里统一回落到无参获取。</p>
     */
    private static final class NoCredentialsDataSource
            extends DelegatingDataSource {

        /**
         * 包装目标数据源。
         *
         * @param target 目标数据源
         */
        private NoCredentialsDataSource(DataSource target) {
            super(target);
        }

        /**
         * 忽略账号获取连接。
         *
         * @param username 用户名
         * @param password 密码
         * @return 连接
         * @throws SQLException 获取失败时抛出
         */
        @Override
        public Connection getConnection(
                String username,
                String password
        ) throws SQLException {
            return getTargetDataSource().getConnection();
        }
    }
}
