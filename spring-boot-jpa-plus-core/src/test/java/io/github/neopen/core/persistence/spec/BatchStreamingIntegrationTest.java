package io.github.neopen.core.persistence.spec;

import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.core.persistence.page.KeysetOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 流式分批消费 H2 集成测试。
 *
 * <p>覆盖 Specification、HQL、原生 SQL 三个执行器的批次边界，并验证
 * 每批回调后持久化上下文被清理、异常时流被关闭。</p>
 */
class BatchStreamingIntegrationTest {

    /** 基准时间。 */
    private static final LocalDateTime BASE =
            LocalDateTime.of(2026, 1, 1, 0, 0);

    /** 实体管理器工厂。 */
    private static EntityManagerFactory entityManagerFactory;

    /** 当前测试使用的实体管理器。 */
    private EntityManager entityManager;

    /**
     * 创建实体管理器工厂。
     */
    @BeforeAll
    static void createEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory(
                "jpa-plus-core-test"
        );
    }

    /**
     * 关闭实体管理器工厂。
     */
    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    /**
     * 重建测试数据。
     */
    @BeforeEach
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from KeysetOrder")
                .executeUpdate();

        for (long id = 1L; id <= 10L; id++) {
            entityManager.persist(new KeysetOrder(
                    id,
                    (int) id,
                    BASE.plusSeconds(id)
            ));
        }

        entityManager.getTransaction().commit();
    }

    /**
     * 关闭实体管理器。
     */
    @AfterEach
    void tearDown() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
    }

    /**
     * 验证 Specification 执行器按批消费并在批间清理缓存。
     */
    @Test
    void shouldConsumeSpecResultsInBatchesAndClearContext() {
        JpaPlusSpecExecutor<KeysetOrder> executor =
                JpaPlusSpecExecutor.of(entityManager, KeysetOrder.class);

        List<Integer> sizes = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        List<KeysetOrder> previous = new ArrayList<>();

        inTransaction(() -> executor.forEachBatch(null, 3, batch -> {
            assertThat(previous)
                    .allMatch(order -> !entityManager.contains(order));

            sizes.add(batch.size());
            batch.forEach(order -> ids.add(order.getId()));
            assertThat(batch).allMatch(entityManager::contains);

            previous.clear();
            previous.addAll(batch);
        }));

        assertThat(sizes).containsExactly(3, 3, 3, 1);
        assertThat(ids)
                .doesNotHaveDuplicates()
                .containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    /**
     * 验证 HQL 执行器的批次边界。
     */
    @Test
    void shouldConsumeHqlResultsInBatches() {
        JpaPlusHqlExecutor executor = new JpaPlusHqlExecutor(entityManager);
        List<List<KeysetOrder>> batches = new ArrayList<>();

        inTransaction(() -> executor.forEachBatch(
                "select o from KeysetOrder o order by o.id",
                null,
                KeysetOrder.class,
                4,
                batches::add
        ));

        assertThat(batches)
                .extracting(List::size)
                .containsExactly(4, 4, 2);
    }

    /**
     * 验证原生 SQL 执行器的批次边界。
     */
    @Test
    void shouldConsumeNativeResultsInBatches() {
        JpaPlusSqlExecutor executor = new JpaPlusSqlExecutor(entityManager);
        List<Long> ids = new ArrayList<>();

        inTransaction(() -> executor.forEachBatch(
                "select id from keyset_order order by id",
                null,
                Long.class,
                3,
                ids::addAll
        ));

        assertThat(ids).hasSize(10).doesNotHaveDuplicates();
    }

    /**
     * 验证回调异常时异常向上传播且事务仍可使用。
     */
    @Test
    void shouldPropagateConsumerFailure() {
        JpaPlusSpecExecutor<KeysetOrder> executor =
                JpaPlusSpecExecutor.of(entityManager, KeysetOrder.class);
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> inTransaction(
                () -> executor.forEachBatch(null, 2, batch -> {
                    if (calls.incrementAndGet() == 2) {
                        throw new IllegalStateException("stop");
                    }
                })
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("stop");

        assertThat(calls).hasValue(2);
    }

    /**
     * 验证十万行导出时持久化上下文不随总量增长。
     */
    @Test
    void shouldKeepPersistenceContextBoundedForLargeExport() {
        int total = 100_000;
        seedLargeTable(total);

        JpaPlusSpecExecutor<KeysetOrder> executor =
                JpaPlusSpecExecutor.of(entityManager, KeysetOrder.class);
        AtomicInteger consumed = new AtomicInteger();
        AtomicReference<KeysetOrder> previous = new AtomicReference<>();

        inTransaction(() -> executor.forEachBatch(null, 500, batch -> {
            assertThat(batch).hasSize(500);

            KeysetOrder detached = previous.get();
            if (detached != null) {
                assertThat(entityManager.contains(detached)).isFalse();
            }

            previous.set(batch.get(0));
            consumed.addAndGet(batch.size());
        }));

        assertThat(consumed).hasValue(total);
    }

    /**
     * 重建大数据量测试数据。
     *
     * @param total 记录总数
     */
    private void seedLargeTable(int total) {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from KeysetOrder")
                .executeUpdate();
        entityManager.clear();

        for (long id = 1L; id <= total; id++) {
            entityManager.persist(new KeysetOrder(
                    id,
                    (int) (id % 100),
                    BASE.plusSeconds(id)
            ));

            if (id % 500 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.getTransaction().commit();
    }

    /**
     * 在事务中执行动作，异常时回滚。
     *
     * @param action 待执行动作
     */
    private void inTransaction(Runnable action) {
        entityManager.getTransaction().begin();

        try {
            action.run();
            entityManager.getTransaction().commit();
        } catch (RuntimeException ex) {
            entityManager.getTransaction().rollback();
            throw ex;
        }
    }
}
