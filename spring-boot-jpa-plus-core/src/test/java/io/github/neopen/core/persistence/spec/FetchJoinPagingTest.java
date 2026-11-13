package io.github.neopen.core.persistence.spec;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link FetchJoinPaging} 单元测试。
 */
@SuppressWarnings("unchecked")
class FetchJoinPagingTest {

    /**
     * 验证无数据时直接返回空页，不再执行加载查询。
     */
    @Test
    void shouldReturnEmptyPageWithoutLoading() {
        EntityManager entityManager = mock(EntityManager.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> countQuery = mock(CriteriaQuery.class);
        Root<Object> root = mock(Root.class);
        Path<Object> idPath = mock(Path.class);
        Expression<Long> countExpression = mock(Expression.class);
        TypedQuery<Long> typedQuery = mock(TypedQuery.class);

        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(Long.class)).thenReturn(countQuery);
        when(countQuery.from(Object.class)).thenReturn(root);
        when(root.get("id")).thenReturn(idPath);
        when(cb.countDistinct(idPath)).thenReturn(countExpression);
        when(countQuery.select(countExpression)).thenReturn(countQuery);
        when(entityManager.createQuery(countQuery)).thenReturn(typedQuery);
        when(typedQuery.getSingleResult()).thenReturn(0L);

        Page<Object> page = FetchJoinPaging.findPage(
                entityManager,
                Object.class,
                "id",
                null,
                List.of("children"),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
        verify(entityManager, times(1)).createQuery(any(CriteriaQuery.class));
    }

    /**
     * 验证非法参数被拒绝。
     */
    @Test
    void shouldRejectInvalidArguments() {
        EntityManager entityManager = mock(EntityManager.class);
        PageRequest pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> FetchJoinPaging.findPage(
                null, Object.class, "id", null, List.of(), pageable
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FetchJoinPaging.findPage(
                entityManager, null, "id", null, List.of(), pageable
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FetchJoinPaging.findPage(
                entityManager, Object.class, " ", null, List.of(), pageable
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FetchJoinPaging.findPage(
                entityManager, Object.class, "id", null, List.of(), null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
