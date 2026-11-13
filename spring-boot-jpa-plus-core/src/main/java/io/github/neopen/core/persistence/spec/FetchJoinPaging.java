package io.github.neopen.core.persistence.spec;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * fetch join 分页查询工具。
 *
 * <p>对集合关联直接使用 fetch join 分页时，JPA Provider 会退化为
 * 内存分页，数据量大时性能很差。本工具采用“三段式”查询解决该问题：</p>
 *
 * <ol>
 *     <li>按条件统计总数；</li>
 *     <li>查询当前页的主键，只查主键，保证分页发生在数据库侧；</li>
 *     <li>按主键批量加载实体，并同时 fetch 关联属性。</li>
 * </ol>
 *
 * <pre>{@code
 * Page<Order> page = FetchJoinPaging.findPage(
 *         entityManager,
 *         Order.class,
 *         "id",
 *         SpecWrapper.create(Order.class)
 *                 .eq("status", "PAID")
 *                 .toSpecification(),
 *         List.of("orderProducts"),
 *         PageRequest.of(0, 10, Sort.by("createTime").descending())
 * );
 * }</pre>
 *
 * <p>传入的 {@link Specification} 请勿再包含 fetch join，关联加载统一由
 * {@code fetchAttributes} 控制；同时 fetch 多个集合关联仍受 JPA
 * Provider 的多重集合加载限制。</p>
 */
public final class FetchJoinPaging {

    private FetchJoinPaging() {
    }

    /**
     * 分页查询并 fetch 关联属性。
     *
     * @param entityManager   实体管理器
     * @param entityType      根实体类型
     * @param idAttribute     主键属性名
     * @param specification   查询条件，允许为 {@code null}
     * @param fetchAttributes 需要 fetch 的根实体关联属性，允许为 {@code null}
     * @param pageable        分页参数
     * @param <T>             实体类型
     * @return 分页结果
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <T> Page<T> findPage(
            EntityManager entityManager,
            Class<T> entityType,
            String idAttribute,
            Specification<T> specification,
            List<String> fetchAttributes,
            Pageable pageable
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

        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Pageable must not be null"
            );
        }

        String id = idAttribute.trim();
        List<String> fetches = normalize(fetchAttributes);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        long total = count(
                entityManager,
                cb,
                entityType,
                id,
                specification
        );

        if (total == 0) {
            return Page.empty(pageable);
        }

        if (pageable.isUnpaged()) {
            List<T> content = load(
                    entityManager,
                    cb,
                    entityType,
                    id,
                    fetches,
                    pageable.getSort(),
                    null
            );

            return new PageImpl<>(content, pageable, total);
        }

        List<?> ids = findIds(
                entityManager,
                cb,
                entityType,
                id,
                specification,
                pageable
        );

        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<T> content = load(
                entityManager,
                cb,
                entityType,
                id,
                fetches,
                pageable.getSort(),
                ids
        );

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 统计满足条件的记录数。
     *
     * @param entityManager 实体管理器
     * @param cb            Criteria 构建器
     * @param entityType    根实体类型
     * @param idAttribute   主键属性名
     * @param specification 查询条件
     * @param <T>           实体类型
     * @return 总记录数
     */
    private static <T> long count(
            EntityManager entityManager,
            CriteriaBuilder cb,
            Class<T> entityType,
            String idAttribute,
            Specification<T> specification
    ) {
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(entityType);
        query.select(cb.countDistinct(root.get(idAttribute)));

        Predicate predicate = predicate(
                specification,
                root,
                query,
                cb
        );

        if (predicate != null) {
            query.where(predicate);
        }

        Long total = entityManager.createQuery(query).getSingleResult();

        return total == null ? 0L : total;
    }

    /**
     * 查询当前页主键。
     *
     * @param entityManager 实体管理器
     * @param cb            Criteria 构建器
     * @param entityType    根实体类型
     * @param idAttribute   主键属性名
     * @param specification 查询条件
     * @param pageable      分页参数
     * @param <T>           实体类型
     * @return 主键列表
     */
    private static <T> List<?> findIds(
            EntityManager entityManager,
            CriteriaBuilder cb,
            Class<T> entityType,
            String idAttribute,
            Specification<T> specification,
            Pageable pageable
    ) {
        CriteriaQuery<Object> query = cb.createQuery(Object.class);
        Root<T> root = query.from(entityType);

        query.select(root.get(idAttribute));

        Predicate predicate = predicate(
                specification,
                root,
                query,
                cb
        );

        if (predicate != null) {
            query.where(predicate);
        }

        orderBy(query, cb, root, pageable.getSort(), idAttribute);

        TypedQuery<Object> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return typedQuery.getResultList();
    }

    /**
     * 按主键批量加载实体并 fetch 关联属性。
     *
     * @param entityManager 实体管理器
     * @param cb            Criteria 构建器
     * @param entityType    根实体类型
     * @param idAttribute   主键属性名
     * @param fetches       需要 fetch 的关联属性
     * @param sort          排序规则
     * @param ids           主键集合；为 {@code null} 时加载全部
     * @param <T>           实体类型
     * @return 实体列表
     */
    private static <T> List<T> load(
            EntityManager entityManager,
            CriteriaBuilder cb,
            Class<T> entityType,
            String idAttribute,
            List<String> fetches,
            Sort sort,
            Collection<?> ids
    ) {
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);

        for (String fetch : fetches) {
            root.fetch(fetch, JoinType.LEFT);
        }

        query.select(root).distinct(true);

        if (ids != null) {
            query.where(root.get(idAttribute).in(ids));
        }

        orderBy(query, cb, root, sort, idAttribute);

        return entityManager.createQuery(query).getResultList();
    }

    /**
     * 构建查询条件。
     *
     * @param specification 查询条件
     * @param root          查询根
     * @param query         Criteria 查询
     * @param cb            Criteria 构建器
     * @param <T>           实体类型
     * @return 查询谓词；没有条件时返回 {@code null}
     */
    private static <T> Predicate predicate(
            Specification<T> specification,
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        return specification == null
                ? null
                : specification.toPredicate(root, query, cb);
    }

    /**
     * 应用排序。
     *
     * <p>没有有效排序时按主键升序排列，保证分页结果稳定。</p>
     *
     * @param query      Criteria 查询
     * @param cb         Criteria 构建器
     * @param root       查询根
     * @param sort       排序规则
     * @param idAttribute 主键属性名
     * @param <T>        实体类型
     */
    private static <T> void orderBy(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<T> root,
            Sort sort,
            String idAttribute
    ) {
        List<Order> orders = new ArrayList<>();

        if (sort != null && sort.isSorted()) {
            for (Sort.Order order : sort) {
                Path<Object> path = root.get(order.getProperty());
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
     * 规范化 fetch 属性列表。
     *
     * @param fetchAttributes 原始属性列表
     * @return 去空去重后的属性列表
     */
    private static List<String> normalize(List<String> fetchAttributes) {
        if (fetchAttributes == null || fetchAttributes.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> fetches = new LinkedHashSet<>();

        for (String attribute : fetchAttributes) {
            if (attribute != null && !attribute.isBlank()) {
                fetches.add(attribute.trim());
            }
        }

        return List.copyOf(fetches);
    }
}
