/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
package io.github.neopen.core.persistence.batch;

import io.github.neopen.core.scope.JpaPlusScope;
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
        BatchOperation.clearScope("secondary");
        JpaPlusScope.clear();
    }

    /**
     * 验证按数据源作用域隔离批次大小。
     */
    @Test
    void shouldUseScopedBatchSize() {
        BatchOperation.setDefaultBatchSize("secondary", 2);
        BatchOperation.setDefaultBatchSize(3);

        assertThat(BatchOperation.getDefaultBatchSize()).isEqualTo(3);
        assertThat(BatchOperation.getDefaultBatchSize("secondary"))
                .isEqualTo(2);
        assertThat(BatchOperation.getDefaultBatchSize("unknown"))
                .isEqualTo(3);

        EntityManager entityManager = mock(EntityManager.class);
        List<String> entities = List.of("a", "b", "c");

        JpaPlusScope.run(
                "secondary",
                () -> BatchOperation.mergeAll(entityManager, entities)
        );

        verify(entityManager, times(3)).merge(
                org.mockito.ArgumentMatchers.any()
        );
        verify(entityManager, times(2)).flush();
        verify(entityManager, times(2)).clear();
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
