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
import static org.mockito.Mockito.when;

/**
 * {@link BatchOperation} 单元测试。
 */
class BatchOperationTest {

    /**
     * 验证按业务键区分新增与更新。
     */
    @Test
    void shouldUpsertByBusinessKey() {
        EntityManager entityManager = mock(EntityManager.class);
        jakarta.persistence.metamodel.Metamodel metamodel =
                mock(jakarta.persistence.metamodel.Metamodel.class);
        @SuppressWarnings("unchecked")
        jakarta.persistence.metamodel.EntityType<Order> entityType =
                mock(jakarta.persistence.metamodel.EntityType.class);
        @SuppressWarnings("unchecked")
        jakarta.persistence.TypedQuery<Order> query =
                mock(jakarta.persistence.TypedQuery.class);

        Order existing = new Order("A", "old");
        Order created = new Order("B", "created");

        when(entityManager.getMetamodel()).thenReturn(metamodel);
        when(metamodel.entity(Order.class)).thenReturn(entityType);
        when(entityType.getName()).thenReturn("Order");
        when(entityManager.createQuery(
                "select e from Order e where e.code in :keys",
                Order.class
        )).thenReturn(query);
        when(query.setParameter(
                org.mockito.ArgumentMatchers.eq("keys"),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(existing));

        BatchOperation.UpsertResult result = BatchOperation.upsertAll(
                entityManager,
                Order.class,
                List.of(new Order("A", "existing"), created),
                Order::getCode,
                "code",
                10
        );

        assertThat(result.inserted()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
        assertThat(existing.getDescription()).isEqualTo("existing");
        verify(entityManager).persist(created);
        verify(entityManager, times(0)).persist(existing);
        verify(entityManager).flush();
        verify(entityManager).clear();
    }

    /**
     * 验证空集合不查询也不写库。
     */
    @Test
    void shouldSkipUpsertForEmptyEntities() {
        EntityManager entityManager = mock(EntityManager.class);

        BatchOperation.UpsertResult result = BatchOperation.upsertAll(
                entityManager,
                Order.class,
                List.of(),
                Order::getCode,
                "code",
                10
        );

        assertThat(result.total()).isZero();
        verify(entityManager, times(0)).flush();
    }

    /**
     * 验证业务键字段名为空时被拒绝。
     */
    @Test
    void shouldRejectBlankUpsertKeyProperty() {
        EntityManager entityManager = mock(EntityManager.class);

        assertThatThrownBy(() -> BatchOperation.upsertAll(
                entityManager,
                Order.class,
                List.of(new Order("A", "a")),
                Order::getCode,
                " ",
                10
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key property must not be blank");
    }

    /** 测试用订单。 */
    private static class Order {

        /** 业务键。 */
        private final String code;

        /** 描述。 */
        private String description;

        /**
         * 创建订单。
         *
         * @param code        业务键
         * @param description 描述
         */
        private Order(String code, String description) {
            this.code = code;
            this.description = description;
        }

        /**
         * 获取业务键。
         *
         * @return 业务键
         */
        String getCode() {
            return code;
        }

        /**
         * 获取描述。
         *
         * @return 描述
         */
        String getDescription() {
            return description;
        }
    }

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
