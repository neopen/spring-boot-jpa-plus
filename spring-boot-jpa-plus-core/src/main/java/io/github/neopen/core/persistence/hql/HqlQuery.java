package io.github.neopen.core.persistence.hql;

import java.util.Map;

/**
 * 不可变的 HQL 查询描述。
 *
 * <p>由 {@link HqlWrapper#build()} 创建，统一携带数据查询、统计查询、
 * 命名参数和排序信息，便于执行器复用。</p>
 */
public final class HqlQuery {

    /** 不包含排序的查询语句。 */
    private final String baseHql;

    /** 统计总数的查询语句。 */
    private final String countHql;

    /** 命名参数。 */
    private final Map<String, Object> parameters;

    /** 排序片段；没有排序时为 {@code null}。 */
    private final String orderBy;

    /**
     * 创建查询描述。
     *
     * @param baseHql    不包含排序的查询语句
     * @param countHql   统计总数的查询语句
     * @param parameters 命名参数
     * @param orderBy    排序片段；没有排序时可为 {@code null}
     */
    HqlQuery(
            String baseHql,
            String countHql,
            Map<String, Object> parameters,
            String orderBy
    ) {
        this.baseHql = baseHql;
        this.countHql = countHql;
        this.parameters = parameters;
        this.orderBy = orderBy;
    }

    /**
     * 获取包含排序的完整查询语句。
     *
     * @return 可直接执行的 HQL
     */
    public String getHql() {
        return orderBy == null
                ? baseHql
                : baseHql + " order by " + orderBy;
    }

    /**
     * 获取不包含排序的查询语句。
     *
     * <p>分页查询需要由外部排序覆盖时使用。</p>
     *
     * @return 不包含 order by 的 HQL
     */
    public String getBaseHql() {
        return baseHql;
    }

    /**
     * 获取统计总数的查询语句。
     *
     * @return count HQL
     */
    public String getCountHql() {
        return countHql;
    }

    /**
     * 获取命名参数。
     *
     * @return 只读参数映射
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * 获取排序片段。
     *
     * @return 排序片段；没有排序时返回 {@code null}
     */
    public String getOrderBy() {
        return orderBy;
    }
}
