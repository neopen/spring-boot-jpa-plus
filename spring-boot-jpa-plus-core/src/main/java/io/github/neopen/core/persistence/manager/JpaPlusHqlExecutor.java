package io.github.neopen.core.persistence.manager;

import io.github.neopen.core.persistence.hql.HqlQuery;
import io.github.neopen.core.persistence.hql.HqlWrapper;
import io.github.neopen.core.persistence.mapping.ResultMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HQL 执行器。
 *
 * <p>基于 JPA 命名参数执行类型安全查询、分页查询及批量写操作。</p>
 */
public class JpaPlusHqlExecutor {

    /** 定位 HQL 的主 from 子句。 */
    private static final Pattern FROM_PATTERN =
            Pattern.compile("\\bfrom\\b", Pattern.CASE_INSENSITIVE);

    /** 定位 HQL 的 order by 子句。 */
    private static final Pattern ORDER_BY_PATTERN =
            Pattern.compile("\\border\\s+by\\b", Pattern.CASE_INSENSITIVE);

    /** 定位 HQL 的 group by 子句。 */
    private static final Pattern GROUP_BY_PATTERN =
            Pattern.compile("\\bgroup\\s+by\\b", Pattern.CASE_INSENSITIVE);

    /** 定位 join fetch 子句，count 查询中需要降级为普通 join。 */
    private static final Pattern JOIN_FETCH_PATTERN =
            Pattern.compile("\\bjoin\\s+fetch\\b", Pattern.CASE_INSENSITIVE);

    /** 提取根实体的可选别名。 */
    private static final Pattern ROOT_PATTERN = Pattern.compile(
            "\\bfrom\\s+[\\w.]+(?:\\s+(?:as\\s+)?([\\w]+))?",
            Pattern.CASE_INSENSITIVE
    );

    /** from 子句中会被误识别为别名的 HQL 关键字。 */
    private static final Set<String> HQL_KEYWORDS = Set.of(
            "where", "join", "left", "right", "inner", "outer",
            "full", "cross", "order", "group", "having", "fetch",
            "on", "set", "as"
    );

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
     * 查询 HQL 结果并映射为 DTO 列表。
     *
     * <p>HQL 的 select 子句需要为每个投影列指定别名，例如
     * {@code select o.orderNo as orderNo from Order o}。列名会忽略
     * 大小写与下划线后与 DTO 属性匹配。</p>
     *
     * @param <T>        DTO 类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型，必须有无参构造器
     * @return 映射后的 DTO 列表
     */
    public <T> List<T> queryForDtoList(
            String hql,
            Map<String, ?> parameters,
            Class<T> dtoType
    ) {
        TypedQuery<Tuple> query =
                entityManager.createQuery(hql, Tuple.class);

        bindParameters(query, parameters);

        return ResultMapper.mapRows(query.getResultList(), dtoType);
    }

    /**
     * 使用 HQL 构造器查询并映射为 DTO 列表。
     *
     * @param <T>     DTO 类型
     * @param wrapper HQL 构造器
     * @param dtoType DTO 类型
     * @return 映射后的 DTO 列表
     */
    public <T> List<T> queryForDtoList(
            HqlWrapper<?> wrapper,
            Class<T> dtoType
    ) {
        HqlQuery query = wrapper.build();

        return queryForDtoList(
                query.getHql(),
                query.getParameters(),
                dtoType
        );
    }

    /**
     * 查询单条 HQL 结果并映射为 DTO。
     *
     * @param <T>        DTO 类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型
     * @return 第一条结果；无结果时返回 {@code null}
     */
    public <T> T queryForDtoObject(
            String hql,
            Map<String, ?> parameters,
            Class<T> dtoType
    ) {
        TypedQuery<Tuple> query =
                entityManager.createQuery(hql, Tuple.class);

        bindParameters(query, parameters);

        List<Tuple> result = query
                .setMaxResults(1)
                .getResultList();

        return result.isEmpty()
                ? null
                : ResultMapper.mapRow(result.get(0), dtoType);
    }

    /**
     * 使用 HQL 构造器查询单条结果并映射为 DTO。
     *
     * @param <T>     DTO 类型
     * @param wrapper HQL 构造器
     * @param dtoType DTO 类型
     * @return 第一条结果；无结果时返回 {@code null}
     */
    public <T> T queryForDtoObject(
            HqlWrapper<?> wrapper,
            Class<T> dtoType
    ) {
        HqlQuery query = wrapper.build();

        return queryForDtoObject(
                query.getHql(),
                query.getParameters(),
                dtoType
        );
    }

    /**
     * 分页查询 HQL 结果并映射为 DTO，自动派生 count 语句。
     *
     * @param <T>        DTO 类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型
     * @param pageable   分页及排序参数，必须为分页状态
     * @return DTO 分页结果
     */
    public <T> Page<T> queryForDtoPage(
            String hql,
            Map<String, ?> parameters,
            Class<T> dtoType,
            Pageable pageable
    ) {
        return queryForDtoPage(
                hql,
                buildCountHql(hql),
                parameters,
                dtoType,
                pageable
        );
    }

    /**
     * 使用指定 count 语句分页查询 HQL 结果并映射为 DTO。
     *
     * @param <T>        DTO 类型
     * @param hql        HQL 查询语句
     * @param countHql   HQL 统计语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param dtoType    DTO 类型
     * @param pageable   分页及排序参数，必须为分页状态
     * @return DTO 分页结果
     */
    public <T> Page<T> queryForDtoPage(
            String hql,
            String countHql,
            Map<String, ?> parameters,
            Class<T> dtoType,
            Pageable pageable
    ) {
        if (hql == null || hql.isBlank()) {
            throw new IllegalArgumentException(
                    "HQL must not be blank"
            );
        }

        if (countHql == null || countHql.isBlank()) {
            throw new IllegalArgumentException(
                    "Count HQL must not be blank"
            );
        }

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Pageable must be paged"
            );
        }

        TypedQuery<Tuple> query = entityManager.createQuery(
                applySort(hql, pageable.getSort()),
                Tuple.class
        );

        bindParameters(query, parameters);

        List<Tuple> content = query
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        TypedQuery<Long> countQuery =
                entityManager.createQuery(countHql, Long.class);

        bindParameters(countQuery, parameters);

        return new PageImpl<>(
                ResultMapper.mapRows(content, dtoType),
                pageable,
                countQuery.getSingleResult()
        );
    }

    /**
     * 使用 HQL 构造器分页查询并映射为 DTO。
     *
     * @param <T>      DTO 类型
     * @param wrapper  HQL 构造器
     * @param dtoType  DTO 类型
     * @param pageable 分页及排序参数，必须为分页状态
     * @return DTO 分页结果
     */
    public <T> Page<T> queryForDtoPage(
            HqlWrapper<?> wrapper,
            Class<T> dtoType,
            Pageable pageable
    ) {
        HqlQuery query = wrapper.build();
        String hql = query.getBaseHql();

        if (pageable.getSort().isSorted()) {
            hql = applySort(hql, pageable.getSort());
        } else if (query.getOrderBy() != null) {
            hql = hql + " order by " + query.getOrderBy();
        }

        return queryForDtoPage(
                hql,
                query.getCountHql(),
                query.getParameters(),
                dtoType,
                pageable
        );
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
     * 分页查询并返回 Spring Data 分页结果。
     *
     * <p>总数由 HQL 自动派生的 count 语句统计，适用于单根实体的
     * 简单查询；包含 group by、distinct 或集合关联时应使用带
     * {@code countHql} 的重载显式指定统计语句。</p>
     *
     * <p>分页请求中的排序会追加到 HQL 末尾，字段名默认按实体根别名
     * 解析；如果 HQL 已经包含 order by，请传入未排序的分页请求。</p>
     *
     * @param <T>        结果类型
     * @param hql        HQL 查询语句
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 结果类型的 {@link Class} 对象
     * @param pageable   分页及排序参数，必须为分页状态
     * @return Spring Data 分页结果
     * @throws IllegalArgumentException 分页参数无效或无法派生 count 语句
     * @throws ArithmeticException      分页偏移量超出整数范围
     */
    public <T> Page<T> queryForPage(
            String hql,
            Map<String, ?> parameters,
            Class<T> resultType,
            Pageable pageable
    ) {
        return queryForPage(
                hql,
                buildCountHql(hql),
                parameters,
                resultType,
                pageable
        );
    }

    /**
     * 使用指定 count 语句分页查询并返回 Spring Data 分页结果。
     *
     * <p>适用于 group by、distinct、多根实体或集合关联等无法自动
     * 统计总数的场景，count 语句与数据语句共用同一组命名参数。</p>
     *
     * @param <T>        结果类型
     * @param hql        HQL 查询语句
     * @param countHql   HQL 统计语句，必须返回单个数值
     * @param parameters 命名参数；无参数时可为 {@code null}
     * @param resultType 结果类型的 {@link Class} 对象
     * @param pageable   分页及排序参数，必须为分页状态
     * @return Spring Data 分页结果
     * @throws IllegalArgumentException 必要参数为空或分页参数无效
     * @throws ArithmeticException      分页偏移量超出整数范围
     */
    public <T> Page<T> queryForPage(
            String hql,
            String countHql,
            Map<String, ?> parameters,
            Class<T> resultType,
            Pageable pageable
    ) {
        if (hql == null || hql.isBlank()) {
            throw new IllegalArgumentException(
                    "HQL must not be blank"
            );
        }

        if (countHql == null || countHql.isBlank()) {
            throw new IllegalArgumentException(
                    "Count HQL must not be blank"
            );
        }

        if (resultType == null) {
            throw new IllegalArgumentException(
                    "Result type must not be null"
            );
        }

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Pageable must be paged"
            );
        }

        return doQueryForPage(
                applySort(hql, pageable.getSort()),
                countHql,
                parameters,
                resultType,
                pageable
        );
    }

    /**
     * 使用 HQL 构造器查询全部结果。
     *
     * @param <T>     实体类型
     * @param wrapper HQL 构造器
     * @return 查询结果
     */
    public <T> List<T> queryForList(HqlWrapper<T> wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException(
                    "HqlWrapper must not be null"
            );
        }

        HqlQuery query = wrapper.build();

        return queryForList(
                query.getHql(),
                query.getParameters(),
                wrapper.getEntityType()
        );
    }

    /**
     * 使用 HQL 构造器统计总数。
     *
     * @param <T>     实体类型
     * @param wrapper HQL 构造器
     * @return 记录总数
     */
    public <T> long queryForCount(HqlWrapper<T> wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException(
                    "HqlWrapper must not be null"
            );
        }

        HqlQuery query = wrapper.build();

        TypedQuery<Long> countQuery = entityManager.createQuery(
                query.getCountHql(),
                Long.class
        );

        bindParameters(countQuery, query.getParameters());

        return countQuery.getSingleResult();
    }

    /**
     * 使用 HQL 构造器分页查询。
     *
     * <p>分页参数携带排序时优先使用分页排序，否则使用构造器中
     * 定义的排序。</p>
     *
     * @param <T>      实体类型
     * @param wrapper  HQL 构造器
     * @param pageable 分页参数，必须为分页状态
     * @return Spring Data 分页结果
     * @throws IllegalArgumentException 构造器或分页参数无效
     */
    public <T> Page<T> queryForPage(
            HqlWrapper<T> wrapper,
            Pageable pageable
    ) {
        if (wrapper == null) {
            throw new IllegalArgumentException(
                    "HqlWrapper must not be null"
            );
        }

        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException(
                    "Pageable must be paged"
            );
        }

        HqlQuery query = wrapper.build();
        String hql = query.getBaseHql();

        if (pageable.getSort().isSorted()) {
            hql = applySort(hql, pageable.getSort());
        } else if (query.getOrderBy() != null) {
            hql = hql + " order by " + query.getOrderBy();
        }

        return doQueryForPage(
                hql,
                query.getCountHql(),
                query.getParameters(),
                wrapper.getEntityType(),
                pageable
        );
    }

    /**
     * 执行分页查询。
     *
     * @param <T>        结果类型
     * @param hql        最终查询语句
     * @param countHql   统计语句
     * @param parameters 命名参数
     * @param resultType 结果类型
     * @param pageable   分页参数
     * @return Spring Data 分页结果
     */
    private <T> Page<T> doQueryForPage(
            String hql,
            String countHql,
            Map<String, ?> parameters,
            Class<T> resultType,
            Pageable pageable
    ) {
        TypedQuery<T> query = entityManager.createQuery(
                hql,
                resultType
        );

        bindParameters(query, parameters);

        List<T> content = query
                .setFirstResult(
                        Math.toIntExact(pageable.getOffset())
                )
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        TypedQuery<Long> countQuery =
                entityManager.createQuery(countHql, Long.class);

        bindParameters(countQuery, parameters);

        return new PageImpl<>(
                content,
                pageable,
                countQuery.getSingleResult()
        );
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
     * 根据查询语句派生 count 语句。
     *
     * <p>派生规则：去掉 order by 子句，将 join fetch 降级为 join，
     * 并在 from 子句前拼接 {@code select count(*)}。</p>
     *
     * @param hql 原始 HQL 查询语句
     * @return 统计总数的 HQL
     * @throws IllegalArgumentException HQL 为空、包含 group by 或缺少 from 子句
     */
    private String buildCountHql(String hql) {
        if (hql == null || hql.isBlank()) {
            throw new IllegalArgumentException(
                    "HQL must not be blank"
            );
        }

        String query = hql.trim();

        if (GROUP_BY_PATTERN.matcher(query).find()) {
            throw new IllegalArgumentException(
                    "HQL with group by requires an explicit countHql"
            );
        }

        Matcher orderBy = ORDER_BY_PATTERN.matcher(query);

        if (orderBy.find()) {
            query = query.substring(0, orderBy.start());
        }

        Matcher from = FROM_PATTERN.matcher(query);

        if (!from.find()) {
            throw new IllegalArgumentException(
                    "Cannot derive count HQL without a from clause"
            );
        }

        String fromClause = JOIN_FETCH_PATTERN.matcher(
                query.substring(from.start())
        ).replaceAll("join").trim();

        return "select count(*) " + fromClause;
    }

    /**
     * 将分页排序规则追加到 HQL。
     *
     * <p>排序字段默认挂到 from 子句的根实体别名上；HQL 没有别名时
     * 直接使用实体属性名。</p>
     *
     * @param hql  原始 HQL 查询语句
     * @param sort 排序规则；为 {@code null} 或未排序时不处理
     * @return 追加排序后的 HQL
     * @throws IllegalArgumentException HQL 已包含 order by
     */
    private String applySort(String hql, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return hql;
        }

        if (ORDER_BY_PATTERN.matcher(hql).find()) {
            throw new IllegalArgumentException(
                    "HQL already contains order by; "
                            + "pass Pageable with Sort.unsorted()"
            );
        }

        String alias = resolveRootAlias(hql);

        List<String> orders = new ArrayList<>();

        for (Sort.Order order : sort) {
            String path = alias.isEmpty()
                    ? order.getProperty()
                    : alias + "." + order.getProperty();

            orders.add(
                    path + (order.isAscending() ? " asc" : " desc")
            );
        }

        return hql + " order by " + String.join(", ", orders);
    }

    /**
     * 解析 from 子句中根实体的别名。
     *
     * @param hql HQL 查询语句
     * @return 根实体别名；没有别名或解析失败时返回空字符串
     */
    private String resolveRootAlias(String hql) {
        Matcher matcher = ROOT_PATTERN.matcher(hql);

        if (!matcher.find()) {
            return "";
        }

        String alias = matcher.group(1);

        if (alias == null
                || HQL_KEYWORDS.contains(alias.toLowerCase(Locale.ROOT))) {
            return "";
        }

        return alias;
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
