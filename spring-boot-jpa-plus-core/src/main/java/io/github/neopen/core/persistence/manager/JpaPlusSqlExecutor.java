package io.github.neopen.core.persistence.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 原生 SQL 执行器。
 *
 * <p>基于 JPA 命名参数执行标量查询、实体映射查询和写操作。</p>
 */
public class JpaPlusSqlExecutor {

    /** 执行原生 SQL 的实体管理器。 */
    private final EntityManager entityManager;

    /**
     * 创建原生 SQL 执行器。
     *
     * @param entityManager 实体管理器，不能为 {@code null}
     */
    public JpaPlusSqlExecutor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 查询原生 SQL 结果列表。
     *
     * <p>单行单字段时元素为标量，单行多字段时元素通常为
     * {@code Object[]}。</p>
     *
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @return 原生查询结果列表
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
     * 查询原生 SQL 并映射为实体列表。
     *
     * @param <T>        实体类型
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 实体类型的 {@link Class} 对象
     * @return 映射后的实体列表
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
     * 查询单条原生 SQL 结果。
     *
     * <p>最多读取一条记录，不会因结果不唯一而抛出异常。</p>
     *
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @return 第一条结果；无结果时返回 {@code null}
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
     * 执行原生 SQL 分页查询。
     *
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param pageNumber 从 0 开始的页码
     * @param pageSize   每页记录数，必须大于 0
     * @return 当前页的原生查询结果列表
     * @throws IllegalArgumentException 页码小于 0 或页大小不大于 0
     * @throws ArithmeticException      分页偏移量超出整数范围
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
     * 执行原生 SQL 新增、更新或删除语句。
     *
     * @param sql        原生 SQL 写操作语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @return 受影响的记录数
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
     * 将命名参数绑定到原生查询。
     *
     * @param query      目标查询
     * @param parameters 命名参数；为 {@code null} 或空时不执行绑定
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
