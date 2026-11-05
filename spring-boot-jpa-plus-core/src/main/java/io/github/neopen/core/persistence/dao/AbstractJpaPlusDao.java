package io.github.neopen.core.persistence.dao;

import io.github.neopen.core.persistence.query.JpaPlusQueryBuilder;
import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.query.JpaPlusQueryBuilder;
import io.github.neopen.core.persistence.query.QueryWrapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 通用 JPA 数据访问抽象实现。
 *
 * <p>默认以只读事务执行查询，写操作方法会重新声明事务语义。</p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
@Transactional(readOnly = true)
public abstract class AbstractJpaPlusDao<
        T,
        ID extends Serializable
        > implements JpaPlusDao<T, ID> {

    /** 当前数据访问对象使用的实体管理器。 */
    protected final EntityManager entityManager;

    /** 当前数据访问对象处理的实体类型。 */
    protected final Class<T> entityClass;

    /** 动态 Criteria 查询构建器。 */
    private final JpaPlusQueryBuilder<T> queryBuilder;

    /** 批量写入时每批处理的实体数量。 */
    private final int batchSize;

    /**
     * 使用默认批次大小创建数据访问对象。
     *
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entityClass   实体类型，不能为 {@code null}
     */
    protected AbstractJpaPlusDao(
            EntityManager entityManager,
            Class<T> entityClass
    ) {
        this(entityManager, entityClass, 500);
    }

    /**
     * 使用指定批次大小创建数据访问对象。
     *
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entityClass   实体类型，不能为 {@code null}
     * @param batchSize     每批处理的实体数量，必须大于 0
     * @throws IllegalArgumentException 必要参数为空或批次大小无效
     */
    protected AbstractJpaPlusDao(
            EntityManager entityManager,
            Class<T> entityClass,
            int batchSize
    ) {
        if (entityManager == null || entityClass == null) {
            throw new IllegalArgumentException(
                    "EntityManager and entityClass are required"
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        this.entityManager = entityManager;
        this.entityClass = entityClass;
        this.batchSize = batchSize;

        this.queryBuilder = new JpaPlusQueryBuilder<>(
                entityManager,
                entityClass
        );
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                entityManager.find(entityClass, id)
        );
    }

    @Override
    public List<T> findAll() {
        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<T> query =
                cb.createQuery(entityClass);

        Root<T> root = query.from(entityClass);

        query.select(root);

        return entityManager.createQuery(query)
                .getResultList();
    }

    @Override
    @Transactional
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException(
                    "Entity must not be null"
            );
        }

        return entityManager.merge(entity);
    }

    @Override
    @Transactional
    public List<T> saveAll(Collection<T> entities) {
        return BatchOperation.mergeAll(
                entityManager,
                entities,
                batchSize
        );
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        if (id == null) {
            return;
        }

        T entity = entityManager.find(entityClass, id);

        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Override
    @Transactional
    public void delete(T entity) {
        if (entity == null) {
            return;
        }

        T managed = entityManager.contains(entity)
                ? entity
                : entityManager.merge(entity);

        entityManager.remove(managed);
    }

    @Override
    @Transactional
    public void deleteAll(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        int count = 0;

        for (T entity : entities) {
            if (entity == null) {
                continue;
            }

            T managed = entityManager.contains(entity)
                    ? entity
                    : entityManager.merge(entity);

            entityManager.remove(managed);

            count++;

            if (count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Override
    public boolean existsById(ID id) {
        if (id == null) {
            return false;
        }

        return entityManager.find(entityClass, id) != null;
    }

    @Override
    public long count() {
        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> query =
                cb.createQuery(Long.class);

        Root<T> root = query.from(entityClass);

        query.select(cb.count(root));

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    @Override
    public Page<T> findPage(Pageable pageable) {
        return findPage(null, pageable);
    }

    @Override
    public List<T> findList(QueryWrapper<T> wrapper) {
        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<T> query =
                cb.createQuery(entityClass);

        Root<T> root = query.from(entityClass);

        query.select(root);

        query.where(
                queryBuilder.buildPredicate(
                        root,
                        cb,
                        wrapper
                )
        );

        return entityManager.createQuery(query)
                .getResultList();
    }

    @Override
    public Page<T> findPage(
            QueryWrapper<T> wrapper,
            Pageable pageable
    ) {
        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Pageable must not be null"
            );
        }

        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        // 查询数据
        CriteriaQuery<T> dataQuery =
                cb.createQuery(entityClass);

        Root<T> dataRoot =
                dataQuery.from(entityClass);

        dataQuery.select(dataRoot);

        dataQuery.where(
                queryBuilder.buildPredicate(
                        dataRoot,
                        cb,
                        wrapper
                )
        );

        applySort(
                dataQuery,
                dataRoot,
                cb,
                pageable.getSort()
        );

        List<T> content = entityManager
                .createQuery(dataQuery)
                .setFirstResult(
                        Math.toIntExact(pageable.getOffset())
                )
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // 查询总数
        CriteriaQuery<Long> countQuery =
                cb.createQuery(Long.class);

        Root<T> countRoot =
                countQuery.from(entityClass);

        countQuery.select(cb.count(countRoot));

        countQuery.where(
                queryBuilder.buildPredicate(
                        countRoot,
                        cb,
                        wrapper
                )
        );

        Long total = entityManager
                .createQuery(countQuery)
                .getSingleResult();

        return new PageImpl<>(
                content,
                pageable,
                total
        );
    }

    /**
     * 将 Spring Data 排序规则应用到 Criteria 查询。
     *
     * <p>排序字段会先通过 JPA 元模型校验，避免无效字段进入查询。</p>
     *
     * @param query 目标查询
     * @param root  查询根路径
     * @param cb    Criteria 构建器
     * @param sort  排序规则；为 {@code null} 或未排序时不处理
     * @throws IllegalArgumentException 排序字段不属于当前实体
     */
    private void applySort(
            CriteriaQuery<T> query,
            Root<T> root,
            CriteriaBuilder cb,
            Sort sort
    ) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }

        List<jakarta.persistence.criteria.Order> orders =
                sort.stream()
                        .map(order -> {
                            String property = order.getProperty();

                            // 通过元模型验证排序字段
                            entityManager.getMetamodel()
                                    .managedType(entityClass)
                                    .getAttribute(property);

                            return order.isAscending()
                                    ? cb.asc(root.get(property))
                                    : cb.desc(root.get(property));
                        })
                        .toList();

        query.orderBy(orders);
    }
}
