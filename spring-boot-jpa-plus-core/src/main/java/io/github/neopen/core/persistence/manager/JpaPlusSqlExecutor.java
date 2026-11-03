package io.github.neopen.core.persistence.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 原生 SQL 执行器
 */
public class JpaPlusSqlExecutor {

    private final EntityManager entityManager;

    public JpaPlusSqlExecutor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 查询原生 SQL 列表
     *
     * 返回值可能是标量，也可能是 Object[]。
     */
    public List<?> queryForList(
            String sql,
            Map<String, ?> parameters
    ) {
        Query query = entityManager.createNativeQuery(sql);

        bindParameters(query, parameters);

        return query.getResultList();
    }

    /**
     * 查询实体列表
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> queryForEntities(
            String sql,
            Map<String, ?> parameters,
            Class<T> resultType
    ) {
        Query query = entityManager.createNativeQuery(
                sql,
                resultType
        );

        bindParameters(query, parameters);

        return query.getResultList();
    }

    /**
     * 查询单条原生 SQL 结果
     */
    public Object queryForObject(
            String sql,
            Map<String, ?> parameters
    ) {
        Query query = entityManager.createNativeQuery(sql);

        bindParameters(query, parameters);

        List<?> result = query
                .setMaxResults(1)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 原生 SQL 分页查询
     */
    public List<?> queryForPage(
            String sql,
            Map<String, ?> parameters,
            int pageNumber,
            int pageSize
    ) {
        if (pageNumber < 0 || pageSize <= 0) {
            throw new IllegalArgumentException(
                    "Invalid pagination parameters"
            );
        }

        Query query = entityManager.createNativeQuery(sql);

        bindParameters(query, parameters);

        return query
                .setFirstResult(
                        Math.multiplyExact(pageNumber, pageSize)
                )
                .setMaxResults(pageSize)
                .getResultList();
    }

    /**
     * 执行 INSERT / UPDATE / DELETE
     */
    @Transactional
    public int executeUpdate(
            String sql,
            Map<String, ?> parameters
    ) {
        Query query = entityManager.createNativeQuery(sql);

        bindParameters(query, parameters);

        return query.executeUpdate();
    }

    /**
     * 绑定 SQL 命名参数
     */
    private void bindParameters(
            Query query,
            Map<String, ?> parameters
    ) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        parameters.forEach(query::setParameter);
    }
}
