package io.github.neopen.core.persistence.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * HQL 执行器
 */
public class JpaPlusHqlExecutor {

    private final EntityManager entityManager;

    public JpaPlusHqlExecutor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 查询实体列表
     */
    public <T> List<T> queryForList(
            String hql,
            Map<String, ?> parameters,
            Class<T> resultType
    ) {
        TypedQuery<T> query =
                entityManager.createQuery(hql, resultType);

        bindParameters(query, parameters);

        return query.getResultList();
    }

    /**
     * 查询单个实体
     */
    public <T> T queryForObject(
            String hql,
            Map<String, ?> parameters,
            Class<T> resultType
    ) {
        TypedQuery<T> query =
                entityManager.createQuery(hql, resultType);

        bindParameters(query, parameters);

        List<T> result = query
                .setMaxResults(1)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 分页查询
     */
    public <T> List<T> queryForPage(
            String hql,
            Map<String, ?> parameters,
            Class<T> resultType,
            int pageNumber,
            int pageSize
    ) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }

        TypedQuery<T> query =
                entityManager.createQuery(hql, resultType);

        bindParameters(query, parameters);

        return query
                .setFirstResult(
                        Math.multiplyExact(pageNumber, pageSize)
                )
                .setMaxResults(pageSize)
                .getResultList();
    }

    /**
     * 执行 HQL 更新或删除
     */
    @Transactional
    public int executeUpdate(
            String hql,
            Map<String, ?> parameters
    ) {
        var query = entityManager.createQuery(hql);

        bindParameters(query, parameters);

        return query.executeUpdate();
    }

    /**
     * 绑定命名参数
     */
    private void bindParameters(
            jakarta.persistence.Query query,
            Map<String, ?> parameters
    ) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        parameters.forEach(query::setParameter);
    }
}
