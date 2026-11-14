package io.github.neopen.core.persistence.manager;

import io.github.neopen.core.persistence.mapping.ResultMapper;
import io.github.neopen.core.persistence.sql.SqlQuery;
import io.github.neopen.core.persistence.sql.SqlWrapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 原生 SQL 执行器。
 *
 * <p>基于 JPA 命名参数执行标量查询、实体映射查询、分页查询和写操作。
 * 分页查询可自动派生 count SQL，也支持显式传入 count SQL。</p>
 */
public class JpaPlusSqlExecutor {

    /** count 子查询别名。 */
    private static final String COUNT_ALIAS = "jpa_plus_count";

    /** 顶层 order by 匹配。 */
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
            "\\border\\s+by\\b",
            Pattern.CASE_INSENSITIVE
    );

    /** 可用于自动补全表别名的简单列名。 */
    private static final Pattern SIMPLE_COLUMN = Pattern.compile(
            "[A-Za-z_][A-Za-z0-9_$]*"
    );

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
     * 使用 SQL 构造器查询结果列表。
     *
     * @param wrapper    SQL 构造器
     * @param parameters 命名参数
     * @return 原生查询结果列表
     */
    public List<?> queryForList(
            SqlWrapper wrapper,
            Map<String, ?> parameters
    ) {
        return queryForList(wrapper.build().getSql(), parameters);
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
     * 使用 SQL 构造器查询并映射为实体列表。
     *
     * @param <T>        实体类型
     * @param wrapper    SQL 构造器
     * @param resultType 实体类型的 {@link Class} 对象
     * @return 映射后的实体列表
     */
    public <T> List<T> queryForList(
            SqlWrapper wrapper,
            Class<T> resultType
    ) {
        SqlQuery query = wrapper.build();

        return queryForEntities(
                query.getSql(),
                query.getParameters(),
                resultType
        );
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
     * 查询单条原生 SQL 结果并映射为实体。
     *
     * @param <T>        实体类型
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 实体类型的 {@link Class} 对象
     * @return 第一条结果；无结果时返回 {@code null}
     */
    public <T> T queryForObject(
            String sql,
            Map<String, ?> parameters,
            Class<T> resultType
    ) {
        Query query = entityManager.createNativeQuery(sql, resultType);

        bindParameters(query, parameters);

        List<T> result = query
                .setMaxResults(1)
                .getResultList();

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 查询原生 SQL 并映射为 DTO 列表。
     *
     * <p>列名会忽略大小写与下划线后与 DTO 属性匹配，例如
     * {@code order_no} 可映射到 {@code orderNo}。</p>
     *
     * @param <T>        DTO 类型
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型，必须有无参构造器
     * @return 映射后的 DTO 列表
     */
    public <T> List<T> queryForDtoList(
            String sql,
            Map<String, ?> parameters,
            Class<T> dtoType
    ) {
        Query query = entityManager.createNativeQuery(sql, Tuple.class);

        bindParameters(query, parameters);

        return ResultMapper.mapRows(query.getResultList(), dtoType);
    }

    /**
     * 使用 SQL 构造器查询并映射为 DTO 列表。
     *
     * @param <T>     DTO 类型
     * @param wrapper SQL 构造器
     * @param dtoType DTO 类型
     * @return 映射后的 DTO 列表
     */
    public <T> List<T> queryForDtoList(
            SqlWrapper wrapper,
            Class<T> dtoType
    ) {
        SqlQuery query = wrapper.build();

        return queryForDtoList(
                query.getSql(),
                query.getParameters(),
                dtoType
        );
    }

    /**
     * 查询单条原生 SQL 结果并映射为 DTO。
     *
     * @param <T>        DTO 类型
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型
     * @return 第一条结果；无结果时返回 {@code null}
     */
    public <T> T queryForDtoObject(
            String sql,
            Map<String, ?> parameters,
            Class<T> dtoType
    ) {
        Query query = entityManager.createNativeQuery(sql, Tuple.class);

        bindParameters(query, parameters);

        List<?> result = query
                .setMaxResults(1)
                .getResultList();

        return result.isEmpty()
                ? null
                : ResultMapper.mapRow(result.get(0), dtoType);
    }

    /**
     * 使用 SQL 构造器查询单条结果并映射为 DTO。
     *
     * @param <T>     DTO 类型
     * @param wrapper SQL 构造器
     * @param dtoType DTO 类型
     * @return 第一条结果；无结果时返回 {@code null}
     */
    public <T> T queryForDtoObject(
            SqlWrapper wrapper,
            Class<T> dtoType
    ) {
        SqlQuery query = wrapper.build();

        return queryForDtoObject(
                query.getSql(),
                query.getParameters(),
                dtoType
        );
    }

    /**
     * 执行原生 SQL 分页查询并映射为 DTO，自动派生 count SQL。
     *
     * @param <T>        DTO 类型
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型
     * @param pageable   分页与排序参数
     * @return DTO 分页结果
     */
    public <T> Page<T> queryForDtoPage(
            String sql,
            Map<String, ?> parameters,
            Class<T> dtoType,
            Pageable pageable
    ) {
        return queryForDtoPage(
                sql,
                buildCountSql(sql),
                parameters,
                dtoType,
                pageable
        );
    }

    /**
     * 执行原生 SQL 分页查询并映射为 DTO，使用显式 count SQL。
     *
     * @param <T>        DTO 类型
     * @param sql        原生 SQL 查询语句
     * @param countSql   统计总数的 SQL
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型
     * @param pageable   分页与排序参数
     * @return DTO 分页结果
     */
    public <T> Page<T> queryForDtoPage(
            String sql,
            String countSql,
            Map<String, ?> parameters,
            Class<T> dtoType,
            Pageable pageable
    ) {
        validatePageable(pageable);

        String pagedSql = applySort(sql, pageable.getSort(), null);

        long total = queryCount(normalize(countSql), parameters);
        List<?> content = queryTuplePage(pagedSql, parameters, pageable);

        return new PageImpl<>(
                ResultMapper.mapRows(content, dtoType),
                pageable,
                total
        );
    }

    /**
     * 使用 SQL 构造器执行分页查询并映射为 DTO。
     *
     * @param <T>     DTO 类型
     * @param wrapper SQL 构造器
     * @param dtoType DTO 类型
     * @param pageable 分页与排序参数
     * @return DTO 分页结果
     */
    public <T> Page<T> queryForDtoPage(
            SqlWrapper wrapper,
            Class<T> dtoType,
            Pageable pageable
    ) {
        validatePageable(pageable);

        SqlQuery query = wrapper.build();
        String sql = query.getBaseSql();

        if (pageable.getSort().isSorted()) {
            sql = applySort(sql, pageable.getSort(), wrapper.getAlias());
        } else if (query.getOrderBy() != null) {
            sql = sql + " order by " + query.getOrderBy();
        }

        long total = queryCount(
                query.getCountSql(),
                query.getParameters()
        );
        List<?> content = queryTuplePage(
                sql,
                query.getParameters(),
                pageable
        );

        return new PageImpl<>(
                ResultMapper.mapRows(content, dtoType),
                pageable,
                total
        );
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
     * 执行原生 SQL 分页查询，并自动派生 count SQL。
     *
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param pageable   分页与排序参数
     * @return 分页结果
     */
    public Page<Object> queryForPage(
            String sql,
            Map<String, ?> parameters,
            Pageable pageable
    ) {
        return queryForPage(sql, buildCountSql(sql), parameters, pageable);
    }

    /**
     * 执行原生 SQL 分页查询，使用显式 count SQL。
     *
     * @param sql        原生 SQL 查询语句
     * @param countSql   统计总数的 SQL
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param pageable   分页与排序参数
     * @return 分页结果
     */
    public Page<Object> queryForPage(
            String sql,
            String countSql,
            Map<String, ?> parameters,
            Pageable pageable
    ) {
        validatePageable(pageable);

        String pagedSql = applySort(sql, pageable.getSort(), null);

        return doQueryForPage(
                pagedSql,
                normalize(countSql),
                parameters,
                pageable
        );
    }

    /**
     * 执行原生 SQL 分页查询并映射为实体，自动派生 count SQL。
     *
     * @param <T>        实体类型
     * @param sql        原生 SQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 实体类型的 {@link Class} 对象
     * @param pageable   分页与排序参数
     * @return 实体分页结果
     */
    public <T> Page<T> queryForPage(
            String sql,
            Map<String, ?> parameters,
            Class<T> resultType,
            Pageable pageable
    ) {
        return queryForPage(
                sql,
                buildCountSql(sql),
                parameters,
                resultType,
                pageable
        );
    }

    /**
     * 执行原生 SQL 分页查询并映射为实体，使用显式 count SQL。
     *
     * @param <T>        实体类型
     * @param sql        原生 SQL 查询语句
     * @param countSql   统计总数的 SQL
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 实体类型的 {@link Class} 对象
     * @param pageable   分页与排序参数
     * @return 实体分页结果
     */
    public <T> Page<T> queryForPage(
            String sql,
            String countSql,
            Map<String, ?> parameters,
            Class<T> resultType,
            Pageable pageable
    ) {
        validatePageable(pageable);

        String pagedSql = applySort(sql, pageable.getSort(), null);

        return doQueryForPage(
                pagedSql,
                normalize(countSql),
                parameters,
                resultType,
                pageable
        );
    }

    /**
     * 使用 SQL 构造器执行分页查询。
     *
     * @param wrapper  SQL 构造器
     * @param pageable 分页与排序参数
     * @return 分页结果
     */
    public Page<Object> queryForPage(
            SqlWrapper wrapper,
            Pageable pageable
    ) {
        validatePageable(pageable);

        SqlQuery query = wrapper.build();
        String sql = query.getBaseSql();

        if (pageable.getSort().isSorted()) {
            sql = applySort(sql, pageable.getSort(), wrapper.getAlias());
        } else if (query.getOrderBy() != null) {
            sql = sql + " order by " + query.getOrderBy();
        }

        return doQueryForPage(
                sql,
                query.getCountSql(),
                query.getParameters(),
                pageable
        );
    }

    /**
     * 使用 SQL 构造器执行分页查询并映射为实体。
     *
     * @param <T>        实体类型
     * @param wrapper    SQL 构造器
     * @param resultType 实体类型的 {@link Class} 对象
     * @param pageable   分页与排序参数
     * @return 实体分页结果
     */
    public <T> Page<T> queryForPage(
            SqlWrapper wrapper,
            Class<T> resultType,
            Pageable pageable
    ) {
        validatePageable(pageable);

        SqlQuery query = wrapper.build();
        String sql = query.getBaseSql();

        if (pageable.getSort().isSorted()) {
            sql = applySort(sql, pageable.getSort(), wrapper.getAlias());
        } else if (query.getOrderBy() != null) {
            sql = sql + " order by " + query.getOrderBy();
        }

        return doQueryForPage(
                sql,
                query.getCountSql(),
                query.getParameters(),
                resultType,
                pageable
        );
    }

    /**
     * 使用 SQL 构造器统计总数。
     *
     * @param wrapper SQL 构造器
     * @return 总记录数
     */
    public long queryForCount(SqlWrapper wrapper) {
        SqlQuery query = wrapper.build();

        return queryCount(query.getCountSql(), query.getParameters());
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
     * 执行分页数据查询并保留列名，用于 DTO 映射。
     *
     * @param sql        已包含排序的查询语句
     * @param parameters 命名参数
     * @param pageable   分页参数
     * @return 元组结果列表
     */
    private List<?> queryTuplePage(
            String sql,
            Map<String, ?> parameters,
            Pageable pageable
    ) {
        Query query = entityManager.createNativeQuery(sql, Tuple.class);

        bindParameters(query, parameters);

        return query
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    /**
     * 执行分页数据查询。
     *
     * @param sql       已包含排序的查询语句
     * @param countSql  统计查询
     * @param parameters 命名参数
     * @param pageable  分页参数
     * @return 分页结果
     */
    @SuppressWarnings("unchecked")
    private Page<Object> doQueryForPage(
            String sql,
            String countSql,
            Map<String, ?> parameters,
            Pageable pageable
    ) {
        Query query = entityManager.createNativeQuery(sql);

        bindParameters(query, parameters);

        List<Object> content = query
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(
                content,
                pageable,
                queryCount(countSql, parameters)
        );
    }

    /**
     * 执行分页实体查询。
     *
     * @param <T>        实体类型
     * @param sql        已包含排序的查询语句
     * @param countSql   统计查询
     * @param parameters 命名参数
     * @param resultType 实体类型
     * @param pageable   分页参数
     * @return 实体分页结果
     */
    @SuppressWarnings("unchecked")
    private <T> Page<T> doQueryForPage(
            String sql,
            String countSql,
            Map<String, ?> parameters,
            Class<T> resultType,
            Pageable pageable
    ) {
        Query query = entityManager.createNativeQuery(sql, resultType);

        bindParameters(query, parameters);

        List<T> content = query
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(
                content,
                pageable,
                queryCount(countSql, parameters)
        );
    }

    /**
     * 执行 count 查询。
     *
     * @param countSql   统计 SQL
     * @param parameters 命名参数
     * @return 总记录数
     */
    private long queryCount(String countSql, Map<String, ?> parameters) {
        Query query = entityManager.createNativeQuery(countSql);

        bindParameters(query, parameters);

        Object result = query.getSingleResult();

        return result instanceof Number number
                ? number.longValue()
                : Long.parseLong(String.valueOf(result));
    }

    /**
     * 校验分页参数。
     *
     * @param pageable 分页参数
     */
    private void validatePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Pageable must not be unpaged"
            );
        }
    }

    /**
     * 派生 count SQL。
     *
     * <p>使用子查询包裹原 SQL，自动兼容 group by；派生前会移除顶层
     * order by，避免部分数据库不接受子查询排序。</p>
     *
     * @param sql 原生 SQL 查询语句
     * @return count SQL
     */
    private String buildCountSql(String sql) {
        String query = normalize(sql);
        Matcher matcher = ORDER_BY_PATTERN.matcher(query);

        if (matcher.find()) {
            query = query.substring(0, matcher.start()).trim();
        }

        return "select count(*) from ("
                + query + ") " + COUNT_ALIAS;
    }

    /**
     * 追加 Spring Data 排序。
     *
     * @param sql   查询语句
     * @param sort  排序规则
     * @param alias 表别名；为 {@code null} 时不补全列名
     * @return 追加排序后的 SQL
     */
    private String applySort(String sql, Sort sort, String alias) {
        if (sort == null || sort.isUnsorted()) {
            return normalize(sql);
        }

        String query = normalize(sql);

        if (ORDER_BY_PATTERN.matcher(query).find()) {
            throw new IllegalArgumentException(
                    "SQL already contains order by, cannot apply Sort"
            );
        }

        List<String> parts = new ArrayList<>();

        for (Sort.Order order : sort) {
            String property = order.getProperty();

            if (property == null || property.isBlank()) {
                continue;
            }

            String path = alias != null
                    && SIMPLE_COLUMN.matcher(property).matches()
                    ? alias + "." + property
                    : property.trim();

            parts.add(path + (order.isAscending() ? " asc" : " desc"));
        }

        return parts.isEmpty()
                ? query
                : query + " order by " + String.join(", ", parts);
    }

    /**
     * 规范化 SQL，移除首尾空白和结尾分号。
     *
     * @param sql 原始 SQL
     * @return 规范化后的 SQL
     */
    private String normalize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(
                    "SQL must not be blank"
            );
        }

        String query = sql.trim();

        while (query.endsWith(";")) {
            query = query.substring(0, query.length() - 1).trim();
        }

        if (query.isEmpty()) {
            throw new IllegalArgumentException(
                    "SQL must not be blank"
            );
        }

        return query;
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
