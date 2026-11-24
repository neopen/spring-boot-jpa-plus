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
package io.github.neopen.core.persistence.manager;

import io.github.neopen.core.persistence.hql.HqlWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JpaPlusHqlExecutor} 分页查询单元测试。
 */
class JpaPlusHqlExecutorTest {

    /** 被测试执行器使用的实体管理器。 */
    private final EntityManager entityManager =
            mock(EntityManager.class);

    /** 被测执行器。 */
    private final JpaPlusHqlExecutor executor =
            new JpaPlusHqlExecutor(entityManager);

    @Test
    void shouldQueryPageWithDerivedCountAndSort() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "select o from Order o where o.status = :status";

        when(entityManager.createQuery(
                hql + " order by o.createTime desc",
                String.class
        )).thenReturn(dataQuery);

        when(entityManager.createQuery(
                "select count(*) from Order o where o.status = :status",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(10)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList())
                .thenReturn(List.of("o-1", "o-2"));
        when(countQuery.getSingleResult()).thenReturn(42L);

        Page<String> page = executor.queryForPage(
                hql,
                Map.of("status", "PAID"),
                String.class,
                PageRequest.of(
                        1,
                        10,
                        Sort.by(Sort.Direction.DESC, "createTime")
                )
        );

        assertThat(page.getContent())
                .containsExactly("o-1", "o-2");
        assertThat(page.getTotalElements()).isEqualTo(42);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);

        verify(dataQuery).setParameter("status", "PAID");
        verify(countQuery).setParameter("status", "PAID");
    }

    @Test
    void shouldUseExplicitCountHql() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "select o.status from Order o group by o.status";
        String countHql =
                "select count(distinct o.status) from Order o";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(entityManager.createQuery(countHql, Long.class))
                .thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of("PAID"));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<String> page = executor.queryForPage(
                hql,
                countHql,
                null,
                String.class,
                PageRequest.of(0, 5)
        );

        assertThat(page.getContent()).containsExactly("PAID");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void shouldStripOrderByFromDerivedCountQuery() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "select o from Order o order by o.id desc";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(entityManager.createQuery(
                "select count(*) from Order o",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<String> page = executor.queryForPage(
                hql,
                null,
                String.class,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldApplySortWithPropertyNameWhenRootHasNoAlias() {
        TypedQuery<String> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        String hql = "from Order where status = :status";

        when(entityManager.createQuery(
                hql + " order by createTime desc",
                String.class
        )).thenReturn(dataQuery);
        when(entityManager.createQuery(
                "select count(*) from Order where status = :status",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(10)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        executor.queryForPage(
                hql,
                Map.of("status", "PAID"),
                String.class,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.DESC, "createTime")
                )
        );
    }

    @Test
    void shouldRejectGroupByWithoutCountHql() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select o.status from Order o group by o.status",
                null,
                String.class,
                PageRequest.of(0, 10)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("countHql");
    }

    @Test
    void shouldRejectSortWhenHqlAlreadyContainsOrderBy() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select o from Order o order by o.id desc",
                null,
                String.class,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order by");
    }

    @Test
    void shouldRejectUnpagedRequest() {
        assertThatThrownBy(() -> executor.queryForPage(
                "select o from Order o",
                null,
                String.class,
                Pageable.unpaged()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("paged");
    }

    @Test
    void shouldQueryPageUsingWrapperAndPageableSort() {
        TypedQuery<SampleEntity> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();
        SampleEntity entity = new SampleEntity();

        HqlWrapper<SampleEntity> wrapper =
                HqlWrapper.create(SampleEntity.class)
                        .eq("status", "PAID");

        when(entityManager.createQuery(
                "select o from SampleEntity o"
                        + " where o.status = :p1"
                        + " order by o.id desc",
                SampleEntity.class
        )).thenReturn(dataQuery);

        when(entityManager.createQuery(
                "select count(*) from SampleEntity o"
                        + " where o.status = :p1",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList())
                .thenReturn(List.of(entity));
        when(countQuery.getSingleResult()).thenReturn(1L);

        Page<SampleEntity> page = executor.queryForPage(
                wrapper,
                PageRequest.of(
                        0,
                        5,
                        Sort.by(Sort.Direction.DESC, "id")
                )
        );

        assertThat(page.getContent()).containsExactly(entity);
        assertThat(page.getTotalElements()).isEqualTo(1);

        verify(dataQuery).setParameter("p1", "PAID");
        verify(countQuery).setParameter("p1", "PAID");
    }

    @Test
    void shouldUseWrapperOrderWhenPageableUnsorted() {
        TypedQuery<SampleEntity> dataQuery = mockQuery();
        TypedQuery<Long> countQuery = mockQuery();

        HqlWrapper<SampleEntity> wrapper =
                HqlWrapper.create(SampleEntity.class)
                        .eq("status", "PAID")
                        .orderByDesc("createTime");

        when(entityManager.createQuery(
                "select o from SampleEntity o"
                        + " where o.status = :p1"
                        + " order by o.createTime desc",
                SampleEntity.class
        )).thenReturn(dataQuery);

        when(entityManager.createQuery(
                "select count(*) from SampleEntity o"
                        + " where o.status = :p1",
                Long.class
        )).thenReturn(countQuery);

        when(dataQuery.setFirstResult(0)).thenReturn(dataQuery);
        when(dataQuery.setMaxResults(5)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        Page<SampleEntity> page = executor.queryForPage(
                wrapper,
                PageRequest.of(0, 5)
        );

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void shouldQueryListUsingWrapper() {
        TypedQuery<SampleEntity> dataQuery = mockQuery();

        HqlWrapper<SampleEntity> wrapper =
                HqlWrapper.create(SampleEntity.class)
                        .in("id", List.of(1L, 2L))
                        .orderByAsc("id");

        when(entityManager.createQuery(
                "select o from SampleEntity o"
                        + " where o.id in (:p1)"
                        + " order by o.id asc",
                SampleEntity.class
        )).thenReturn(dataQuery);

        when(dataQuery.getResultList()).thenReturn(List.of());

        assertThat(executor.queryForList(wrapper)).isEmpty();

        verify(dataQuery).setParameter("p1", List.of(1L, 2L));
    }

    /**
     * 创建类型化查询的 Mock 对象。
     *
     * @param <T> 查询结果类型
     * @return 查询 Mock 对象
     */
    @SuppressWarnings("unchecked")
    private <T> TypedQuery<T> mockQuery() {
        return mock(TypedQuery.class);
    }

    @Test
    void shouldStreamResultsWithBoundParameters() {
        TypedQuery<String> dataQuery = mockQuery();
        String hql = "select o.orderNo from Order o where o.status = :status";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(dataQuery.setParameter("status", "PAID"))
                .thenReturn(dataQuery);
        when(dataQuery.getResultStream())
                .thenReturn(Stream.of("o-1", "o-2"));

        List<String> result = executor
                .stream(hql, Map.of("status", "PAID"), String.class)
                .toList();

        assertThat(result).containsExactly("o-1", "o-2");
        verify(dataQuery).getResultStream();
    }

    @Test
    void shouldConsumeStreamInBatchesAndClearContext() {
        TypedQuery<String> dataQuery = mockQuery();
        String hql = "select o.orderNo from Order o";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(dataQuery.getResultStream()).thenReturn(
                Stream.of("o-1", "o-2", "o-3", "o-4", "o-5")
        );

        List<List<String>> batches = new ArrayList<>();

        executor.forEachBatch(
                hql,
                null,
                String.class,
                2,
                batches::add
        );

        assertThat(batches)
                .containsExactly(
                        List.of("o-1", "o-2"),
                        List.of("o-3", "o-4"),
                        List.of("o-5")
                );
        verify(entityManager, times(3)).flush();
        verify(entityManager, times(3)).clear();
    }

    @Test
    void shouldNotInvokeConsumerForEmptyResult() {
        TypedQuery<String> dataQuery = mockQuery();
        String hql = "select o.orderNo from Order o";

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(dataQuery.getResultStream()).thenReturn(Stream.empty());

        AtomicInteger calls = new AtomicInteger();

        executor.forEachBatch(
                hql,
                null,
                String.class,
                10,
                batch -> calls.incrementAndGet()
        );

        assertThat(calls).hasValue(0);
        verify(entityManager, never()).flush();
        verify(entityManager, never()).clear();
    }

    @Test
    void shouldCloseStreamWhenConsumerFails() {
        TypedQuery<String> dataQuery = mockQuery();
        String hql = "select o.orderNo from Order o";
        AtomicBoolean closed = new AtomicBoolean();

        when(entityManager.createQuery(hql, String.class))
                .thenReturn(dataQuery);
        when(dataQuery.getResultStream()).thenReturn(
                Stream.of("o-1", "o-2").onClose(
                        () -> closed.set(true)
                )
        );

        assertThatThrownBy(() -> executor.forEachBatch(
                hql,
                null,
                String.class,
                1,
                batch -> {
                    throw new IllegalStateException("boom");
                }
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(closed).isTrue();
        verify(entityManager, never()).flush();
        verify(entityManager, never()).clear();
    }

    @Test
    void shouldRejectInvalidBatchArguments() {
        assertThatThrownBy(() -> executor.forEachBatch(
                "select o from Order o",
                null,
                String.class,
                0,
                batch -> {
                }
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Batch size");

        assertThatThrownBy(() -> executor.forEachBatch(
                "select o from Order o",
                null,
                String.class,
                10,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Consumer");
    }

    /** 测试实体占位。 */
    private static final class SampleEntity {
    }
}
