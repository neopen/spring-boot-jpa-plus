package io.github.neopen.core.persistence.sql;

import java.util.Map;

/**
 * 不可变的 SQL 查询描述。
 *
 * <p>由 {@link SqlWrapper#build()} 创建，统一携带数据查询、统计查询、
 * 命名参数和排序信息，便于执行器复用。</p>
 */
public final class SqlQuery {

    /** 不包含排序的查询语句。 */
    private final String baseSql;

    /** 统计总数的查询语句。 */
    private final String countSql;

    /** 命名参数。 */
    private final Map<String, Object> parameters;

    /** 排序片段；没有排序时为 {@code null}。 */
    private final String orderBy;

    /**
     * 创建查询描述。
     *
     * @param baseSql    不包含排序的查询语句
     * @param countSql   统计总数的查询语句
     * @param parameters 命名参数
     * @param orderBy    排序片段；没有排序时可为 {@code null}
     */
    SqlQuery(
            String baseSql,
            String countSql,
            Map<String, Object> parameters,
            String orderBy
    ) {
        this.baseSql = baseSql;
        this.countSql = countSql;
        this.parameters = parameters;
        this.orderBy = orderBy;
    }

    /**
     * 获取包含排序的完整查询语句。
     *
     * @return 可直接执行的原生 SQL
     */
    public String getSql() {
        return orderBy == null
                ? baseSql
                : baseSql + " order by " + orderBy;
    }

    /**
     * 获取不包含排序的查询语句。
     *
     * <p>分页查询需要由外部排序覆盖时使用。</p>
     *
     * @return 不包含 order by 的 SQL
     */
    public String getBaseSql() {
        return baseSql;
    }

    /**
     * 获取统计总数的查询语句。
     *
     * @return count SQL
     */
    public String getCountSql() {
        return countSql;
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
