package io.github.neopen.core.persistence.spec;

import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification 查询执行器。
 *
 * <p>统一处理 count、分页、切片与 fetch join 分页，并在查询条件中
 * 自动追加逻辑删除过滤。</p>
 *
 * <pre>{@code
 * JpaPlusSpecExecutor<Order> executor =
 *         JpaPlusSpecExecutor.of(entityManager, Order.class, "id");
 *
 * Page<Order> page = executor.findPage(
 *         SpecWrapper.create(Order.class).eq("status", "PAID"),
 *         PageRequest.of(0, 10, Sort.by("createTime").descending())
 * );
 * }</pre>
 *
 * <p>分页查询不传 fetch 属性时走普通 Criteria 查询；传入 fetch 属性时
 * 委托 {@link FetchJoinPaging}，保证分页发生在数据库侧。</p>
 *
 * @param <T> 实体类型
 */
public class JpaPlusSpecExecutor<T> {

    /** 实体管理器。 */
    private final EntityManager entityManager;

    /** 根实体类型。 */
    private final Class<T> entityType;

    /** 主键属性名。 */
    private final String idAttribute;

    /** 是否自动追加逻辑删除过滤。 */
    private boolean logicDelete = true;

    /**
     * 创建查询执行器。
     *
     * @param entityManager 实体管理器
     * @param entityType    根实体类型
     * @param idAttribute   主键属性名
     */
    private JpaPlusSpecExecutor(
            EntityManager entityManager,
            Class<T> entityType,
            String idAttribute
    ) {
        this.entityManager = entityManager;
        this.entityType = entityType;
        this.idAttribute = idAttribute;
    }

    /**
     * 使用默认主键 {@code id} 创建查询执行器。
     *
     * @param entityManager 实体管理器
     * @param entityType    根实体类型
     * @param <T>           实体类型
     * @return 查询执行器
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <T> JpaPlusSpecExecutor<T> of(
            EntityManager entityManager,
            Class<T> entityType
    ) {
        return of(entityManager, entityType, "id");
    }

    /**
     * 创建查询执行器。
     *
     * @param entityManager 实体管理器
     * @param entityType    根实体类型
     * @param idAttribute   主键属性名
     * @param <T>           实体类型
     * @return 查询执行器
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <T> JpaPlusSpecExecutor<T> of(
            EntityManager entityManager,
            Class<T> entityType,
            String idAttribute
    ) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "Entity manager must not be null"
            );
        }

        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        if (idAttribute == null || idAttribute.isBlank()) {
            throw new IllegalArgumentException(
                    "Id attribute must not be blank"
            );
        }

        return new JpaPlusSpecExecutor<>(
                entityManager,
                entityType,
                idAttribute.trim()
        );
    }

    /**
     * 设置是否自动追加逻辑删除过滤。
     *
     * <p>默认开启。实体未标注
     * {@link io.github.neopen.core.persistence.logicdelete.LogicDelete}
     * 时不会产生额外条件。</p>
     *
     * @param enabled 是否追加逻辑删除过滤
     * @return 当前执行器
     */
    public JpaPlusSpecExecutor<T> logicDelete(boolean enabled) {
        this.logicDelete = enabled;
        return this;
    }

    /**
     * 查询满足条件的全部实体。
     *
     * @param specification 查询条件，允许为 {@code null}
     * @return 实体列表
     */
    public List<T> findAll(Specification<T> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);

        query.select(root);
        applyPredicate(query, root, effective(specification), cb);

        return entityManager.createQuery(query).getResultList();
    }

    /**
     * 分页查询。
     *
     * @param specification 查询条件，允许为 {@code null}
     * @param pageable      分页参数
     * @return 分页结果
     * @throws IllegalArgumentException 分页参数为空时抛出
     */
    public Page<T> findPage(
            Specification<T> specification,
            Pageable pageable
    ) {
        return findPage(specification, pageable, null);
    }

    /**
     * 分页查询并 fetch 关联属性。
     *
     * <p>{@code fetchAttributes} 非空时委托 {@link FetchJoinPaging}，
     * 先按主键分页再批量 fetch 加载。</p>
     *
     * @param specification   查询条件，允许为 {@code null}
     * @param pageable        分页参数
     * @param fetchAttributes 需要 fetch 的根实体关联属性，允许为
     *                        {@code null}
     * @return 分页结果
     * @throws IllegalArgumentException 分页参数为空时抛出
     */
    public Page<T> findPage(
            Specification<T> specification,
            Pageable pageable,
            List<String> fetchAttributes
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Pageable must not be null"
            );
        }

        Specification<T> effective = effective(specification);

        if (fetchAttributes != null && !fetchAttributes.isEmpty()) {
            return FetchJoinPaging.findPage(
                    entityManager,
                    entityType,
                    idAttribute,
                    effective,
                    fetchAttributes,
                    pageable
            );
        }

        if (pageable.isUnpaged()) {
            return new PageImpl<>(queryAll(effective));
        }

        long total = countInternal(effective);

        if (total == 0) {
            return Page.empty(pageable);
        }

        return new PageImpl<>(
                queryPage(effective, pageable),
                pageable,
                total
        );
    }

    /**
     * 切片查询，不执行 count。
     *
     * <p>按 {@code pageSize + 1} 条记录判断是否存在下一页，
     * 适合滚动加载等不需要总记录数的场景。</p>
     *
     * @param specification 查询条件，允许为 {@code null}
     * @param pageable      分页参数
     * @return 切片结果
     * @throws IllegalArgumentException 分页参数为空时抛出
     */
    public Slice<T> findSlice(
            Specification<T> specification,
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Pageable must not be null"
            );
        }

        if (pageable.isUnpaged()) {
            return new SliceImpl<>(
                    findAll(specification),
                    pageable,
                    false
            );
        }

        int pageSize = pageable.getPageSize();
        List<T> content = queryPage(
                effective(specification),
                pageable,
                pageSize + 1
        );
        boolean hasNext = content.size() > pageSize;

        if (hasNext) {
            content = new ArrayList<>(content.subList(0, pageSize));
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * 统计满足条件的记录数。
     *
     * @param specification 查询条件，允许为 {@code null}
     * @return 总记录数
     */
    public long count(Specification<T> specification) {
        return countInternal(effective(specification));
    }

    /**
     * 统计满足条件的记录数。
     *
     * <p>使用 {@code countDistinct(主键)}，避免连表产生重复计数。</p>
     *
     * @param effective 已合并逻辑删除过滤的查询条件
     * @return 总记录数
     */
    private long countInternal(Specification<T> effective) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityType);

        query.select(cb.countDistinct(root.get(idAttribute)));
        applyPredicate(query, root, effective, cb);

        Long total = entityManager.createQuery(query).getSingleResult();

        return total == null ? 0L : total;
    }

    /**
     * 查询全部实体。
     *
     * @param effective 已合并逻辑删除过滤的查询条件
     * @return 实体列表
     */
    private List<T> queryAll(Specification<T> effective) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);

        query.select(root);
        applyPredicate(query, root, effective, cb);
        applySort(query, cb, root, Sort.unsorted());

        return entityManager.createQuery(query).getResultList();
    }

    /**
     * 查询分页实体。
     *
     * @param effective 已合并逻辑删除过滤的查询条件
     * @param pageable  分页参数
     * @return 当前页实体列表
     */
    private List<T> queryPage(
            Specification<T> effective,
            Pageable pageable
    ) {
        return queryPage(effective, pageable, pageable.getPageSize());
    }

    /**
     * 按指定最大条数查询当前页实体。
     *
     * @param effective 已合并逻辑删除过滤的查询条件
     * @param pageable  分页参数
     * @param maxResults 最大返回条数
     * @return 当前页实体列表
     */
    private List<T> queryPage(
            Specification<T> effective,
            Pageable pageable,
            int maxResults
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);

        query.select(root);
        applyPredicate(query, root, effective, cb);
        applySort(query, cb, root, pageable.getSort());

        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(maxResults);

        return typedQuery.getResultList();
    }

    /**
     * 合并逻辑删除过滤。
     *
     * @param specification 查询条件
     * @return 合并后的查询条件
     */
    private Specification<T> effective(Specification<T> specification) {
        return logicDelete
                ? LogicDeletes.filter(entityType, specification)
                : specification;
    }

    /**
     * 应用查询条件。
     *
     * @param query      Criteria 查询
     * @param root       查询根
     * @param effective  已合并逻辑删除过滤的查询条件
     * @param cb         Criteria 构建器
     */
    private void applyPredicate(
            CriteriaQuery<?> query,
            Root<T> root,
            Specification<T> effective,
            CriteriaBuilder cb
    ) {
        if (effective == null) {
            return;
        }

        Predicate predicate = effective.toPredicate(root, query, cb);

        if (predicate != null) {
            query.where(predicate);
        }
    }

    /**
     * 应用排序。
     *
     * <p>没有有效排序时按主键升序排列，保证分页结果稳定。</p>
     *
     * @param query Criteria 查询
     * @param cb    Criteria 构建器
     * @param root  查询根
     * @param sort  排序规则
     */
    private void applySort(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<T> root,
            Sort sort
    ) {
        List<Order> orders = new ArrayList<>();

        if (sort != null && sort.isSorted()) {
            for (Sort.Order order : sort) {
                Path<?> path = path(root, order.getProperty());
                orders.add(
                        order.isAscending()
                                ? cb.asc(path)
                                : cb.desc(path)
                );
            }
        } else {
            orders.add(cb.asc(root.get(idAttribute)));
        }

        query.orderBy(orders);
    }

    /**
     * 解析属性路径，支持嵌套属性。
     *
     * @param root     查询根
     * @param property 属性路径
     * @return 路径
     */
    private Path<?> path(Root<T> root, String property) {
        Path<?> path = root;

        for (String segment : property.split("\\.")) {
            path = path.get(segment);
        }

        return path;
    }
}
