package io.github.neopen.core.persistence.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * HQL 执行器。
 *
 * <p>基于 JPA 命名参数执行类型安全查询、分页查询及批量写操作。</p>
 */
public class JpaPlusHqlExecutor {

    /** 执行 HQL 的实体管理器。 */
    private final EntityManager entityManager;

    /**
     * 创建 HQL 执行器。
     *
     * @param entityManager 实体管理器，不能为 {@code null}
     */
    public JpaPlusHqlExecutor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * 查询指定类型的结果列表。
     *
     * @param <T>        结果类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 结果类型的 {@link Class} 对象
     * @return 查询结果列表
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
     * 查询单条指定类型的结果。
     *
     * <p>最多读取一条记录，不会因结果不唯一而抛出异常。</p>
     *
     * @param <T>        结果类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 结果类型的 {@link Class} 对象
     * @return 第一条结果；无结果时返回 {@code null}
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
     * 分页查询指定类型的结果。
     *
     * @param <T>        结果类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 结果类型的 {@link Class} 对象
     * @param pageNumber 从 0 开始的页码
     * @param pageSize   每页记录数，必须大于 0
     * @return 当前页的结果列表
     * @throws IllegalArgumentException 页码小于 0 或页大小不大于 0
     * @throws ArithmeticException      分页偏移量超出整数范围
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
     * 执行 HQL 更新或删除语句。
     *
     * @param hql        HQL 更新或删除语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @return 受影响的记录数
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
     * 将命名参数绑定到查询。
     *
     * @param query      目标查询
     * @param parameters 命名参数；为 {@code null} 或空时不执行绑定
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
