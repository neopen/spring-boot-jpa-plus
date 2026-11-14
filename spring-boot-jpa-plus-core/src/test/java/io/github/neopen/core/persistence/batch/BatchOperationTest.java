package io.github.neopen.core.persistence.batch;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link BatchOperation} 单元测试。
 */
class BatchOperationTest {

    /**
     * 恢复默认批次大小。
     */
    @AfterEach
    void reset() {
        BatchOperation.setDefaultBatchSize(500);
    }

    /**
     * 验证按批持久化并在结束时刷新。
     */
    @Test
    void shouldPersistInBatches() {
        EntityManager entityManager = mock(EntityManager.class);
        List<String> entities = List.of("a", "b", "c", "d", "e");

        BatchOperation.persistAll(entityManager, entities, 2);

        verify(entityManager, times(5)).persist(
                org.mockito.ArgumentMatchers.any()
        );
        verify(entityManager, times(3)).flush();
        verify(entityManager, times(3)).clear();
    }

    /**
     * 验证按批删除实体。
     */
    @Test
    void shouldRemoveInBatches() {
        EntityManager entityManager = mock(EntityManager.class);
        List<String> entities = List.of("a", "b", "c");

        BatchOperation.removeAll(entityManager, entities, 2);

        verify(entityManager, times(3)).remove(
                org.mockito.ArgumentMatchers.any()
        );
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
    }

    /**
     * 验证实体管理器为空时快速失败。
     */
    @Test
    void shouldRejectNullEntityManager() {
        assertThatThrownBy(
                () -> BatchOperation.persistAll(null, List.of("a"), 10)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntityManager");

        assertThatThrownBy(
                () -> BatchOperation.removeAll(null, List.of("a"))
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EntityManager");
    }

    /**
     * 验证非法批次大小被拒绝。
     */
    @Test
    void shouldRejectInvalidBatchSize() {
        EntityManager entityManager = mock(EntityManager.class);

        assertThatThrownBy(
                () -> BatchOperation.persistAll(
                        entityManager,
                        List.of("a"),
                        0
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize");
    }

    /**
     * 验证集合按批次回调。
     */
    @Test
    void shouldInvokeConsumerPerBatch() {
        List<Integer> batches = new ArrayList<>();

        BatchOperation.forEachBatch(
                List.of(1, 2, 3, 4, 5),
                2,
                batch -> batches.add(batch.size())
        );

        assertThat(batches).containsExactly(2, 2, 1);
    }

    /**
     * 验证空集合不触发回调。
     */
    @Test
    void shouldSkipEmptyCollection() {
        List<Integer> batches = new ArrayList<>();

        BatchOperation.forEachBatch(List.<Integer>of(), 10, batches::addAll);

        assertThat(batches).isEmpty();
    }

    /**
     * 验证默认批次大小可配置。
     */
    @Test
    void shouldUseConfigurableDefaultBatchSize() {
        BatchOperation.setDefaultBatchSize(2);

        EntityManager entityManager = mock(EntityManager.class);

        BatchOperation.removeAll(
                entityManager,
                List.of("a", "b", "c", "d", "e")
        );

        verify(entityManager, times(3)).flush();
        verify(entityManager, times(3)).clear();
        assertThat(BatchOperation.getDefaultBatchSize()).isEqualTo(2);
    }

    /**
     * 验证非法默认批次大小被拒绝。
     */
    @Test
    void shouldRejectInvalidDefaultBatchSize() {
        assertThatThrownBy(() -> BatchOperation.setDefaultBatchSize(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }
}
