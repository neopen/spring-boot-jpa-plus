/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.persistence.spec;

import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.logicdelete.LogicDeletes;
import io.github.neopen.core.persistence.page.KeysetCursor;
import io.github.neopen.core.persistence.page.KeysetPage;
import io.github.neopen.core.persistence.page.KeysetPageParams;
import io.github.neopen.core.persistence.tenant.Tenants;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.IdClass;
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
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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
     * 流式查询满足条件的实体。
     *
     * <p>不会一次性把全部结果读入内存，适合大数据量导出、批处理
     * 等场景。返回的流绑定当前持久化上下文，必须在事务范围内消费
     * 完毕，并优先使用 try-with-resources 关闭：</p>
     *
     * <pre>{@code
     * try (Stream<Order> stream = executor.stream(spec)) {
     *     stream.forEach(this::handle);
     * }
     * }</pre>
     *
     * <p>结果按主键升序排列，保证流的顺序稳定。</p>
     *
     * @param specification 查询条件，允许为 {@code null}
     * @return 实体流，使用完毕后需要关闭
     */
    public Stream<T> stream(Specification<T> specification) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);

        query.select(root);
        applyPredicate(query, root, effective(specification), cb);
        applySort(query, cb, root, Sort.unsorted());

        return entityManager.createQuery(query).getResultStream();
    }

    /**
     * 按批次流式消费满足条件的实体。
     *
     * <p>在 {@link #stream(Specification)} 基础上按 {@code batchSize}
     * 聚合结果，每批回调返回后执行 {@code flush} 与 {@code clear}，
     * 因此一级缓存不会随结果总量增长，适合大数据量导出与批处理。</p>
     *
     * <p>执行器不会提交或回滚事务，事务边界仍由调用方控制；回调抛出
     * 异常或需要提前结束遍历时，流会在异常传播前关闭。每批实体在
     * 回调返回后即被清出持久化上下文，后续批次不再保留已消费实体。</p>
     *
     * @param specification 查询条件，允许为 {@code null}
     * @param batchSize     每批数量，必须大于 0
     * @param consumer      批次回调
     * @throws IllegalArgumentException 批次大小不大于 0 或回调为空
     */
    public void forEachBatch(
            Specification<T> specification,
            int batchSize,
            Consumer<List<T>> consumer
    ) {
        validateBatch(batchSize, consumer);

        consumeBatches(stream(specification), batchSize, consumer);
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
     * 游标（keyset）分页查询。
     *
     * <p>不计算总数，也不使用 offset，而是以 {@link KeysetCursor} 为
     * 起点生成 {@code (排序键, 主键) > (?, ?)} 条件。排序键必须唯一，
     * 执行器会在排序末尾自动追加主键作为 tiebreaker（已包含主键时
     * 不重复追加）。</p>
     *
     * <p>游标值按排序字段的 Java 类型转换，支持字符串、常用数值、
     * {@link Boolean}、{@link UUID}、{@link LocalDate}、
     * {@link LocalDateTime}、{@link LocalTime}、{@link Instant}
     * 与枚举。复合主键实体不支持游标分页。</p>
     *
     * @param specification 查询条件，允许为 {@code null}
     * @param params        游标分页参数
     * @return 游标分页结果
     * @throws IllegalArgumentException 参数非法、游标值个数与排序项不匹配、
     *                                 游标值类型不支持或主键为复合主键时抛出
     */
    public KeysetPage<T> keyset(
            Specification<T> specification,
            KeysetPageParams params
    ) {
        if (params == null) {
            throw new IllegalArgumentException(
                    "Keyset page params must not be null"
            );
        }

        ensureSimplePrimaryKey();

        Sort sort = withTiebreaker(params.getSort());
        List<Sort.Order> orders = new ArrayList<>();
        sort.forEach(orders::add);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);

        query.select(root);

        Predicate predicate = null;
        Specification<T> effective = effective(specification);

        if (effective != null) {
            predicate = effective.toPredicate(root, query, cb);
        }

        Predicate combined = combine(
                cb,
                predicate,
                keysetPredicate(
                        cb,
                        root,
                        orders,
                        cursorValues(root, orders, params.getCursor())
                )
        );

        if (combined != null) {
            query.where(combined);
        }

        applySort(query, cb, root, sort);

        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(params.getSize() + 1);

        List<T> content = typedQuery.getResultList();
        boolean hasNext = content.size() > params.getSize();

        if (hasNext) {
            content = new ArrayList<>(content.subList(0, params.getSize()));
        }

        KeysetCursor nextCursor = hasNext
                ? cursorOf(orders, content.get(content.size() - 1))
                : null;

        return new KeysetPage<>(content, nextCursor);
    }

    /**
     * 按主键查询实体。
     *
     * <p>查询会自动追加逻辑删除过滤，已删除实体返回 {@code null}。</p>
     *
     * @param id 主键值
     * @return 实体；不存在或已删除时返回 {@code null}
     */
    public T findById(Object id) {
        if (id == null) {
            return null;
        }

        List<T> result = findAll(
                (root, query, cb) -> cb.equal(root.get(idAttribute), id)
        );

        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 按主键执行逻辑删除。
     *
     * <p>实体必须标注
     * {@link io.github.neopen.core.persistence.logicdelete.LogicDelete}。</p>
     *
     * @param id 主键值
     * @return 受影响的记录数
     * @throws IllegalArgumentException 实体未标注逻辑删除注解时抛出
     */
    public int deleteById(Object id) {
        return LogicDeletes.deleteById(entityManager, entityType, id);
    }

    /**
     * 按主键批量执行逻辑删除。
     *
     * @param ids 主键集合
     * @return 受影响的记录数
     * @throws IllegalArgumentException 实体未标注逻辑删除注解时抛出
     */
    public int deleteAllById(Collection<?> ids) {
        return LogicDeletes.deleteAllById(
                entityManager,
                entityType,
                ids
        );
    }

    /**
     * 将实体标记为已删除。
     *
     * @param entities 待删除实体
     * @return 标记的实体数量
     * @throws IllegalArgumentException 实体未标注逻辑删除注解时抛出
     */
    public int deleteAll(Collection<T> entities) {
        return LogicDeletes.deleteAll(
                entityManager,
                entityType,
                entities
        );
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
     * 判断是否存在满足条件的记录。
     *
     * <p>与 {@link #count(Specification)} 使用相同的逻辑删除过滤，
     * 但只关心是否存在，便于校验唯一性或做前置判断。</p>
     *
     * @param specification 查询条件，允许为 {@code null}
     * @return 存在记录时返回 {@code true}
     */
    public boolean exists(Specification<T> specification) {
        return countInternal(effective(specification)) > 0L;
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
        Specification<T> effective = logicDelete
                ? LogicDeletes.filter(entityType, specification)
                : specification;

        effective = Tenants.filter(entityType, effective);

        return DataScopes.filter(entityType, effective);
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
     * 在排序末尾追加主键，保证游标条件使用的排序唯一。
     *
     * @param sort 原始排序
     * @return 追加 tiebreaker 后的排序
     */
    private Sort withTiebreaker(Sort sort) {
        for (Sort.Order order : sort) {
            if (idAttribute.equals(order.getProperty())) {
                return sort;
            }
        }

        List<Sort.Order> orders = new ArrayList<>();
        sort.forEach(orders::add);
        orders.add(Sort.Order.asc(idAttribute));

        return Sort.by(orders);
    }

    /**
     * 解析游标值，并按排序路径的 Java 类型转换。
     *
     * @param root   查询根
     * @param orders 排序项
     * @param cursor 游标，允许为 {@code null}
     * @return 转换后的游标值；未携带游标时返回 {@code null}
     */
    private List<Object> cursorValues(
            Root<T> root,
            List<Sort.Order> orders,
            KeysetCursor cursor
    ) {
        if (cursor == null) {
            return null;
        }

        List<Object> values = cursor.getValues();

        if (values.size() != orders.size()) {
            throw new IllegalArgumentException(
                    "Cursor value count must match sort order count"
            );
        }

        List<Object> resolved = new ArrayList<>(values.size());

        for (int i = 0; i < values.size(); i++) {
            Path<?> attribute = path(root, orders.get(i).getProperty());
            resolved.add(convert(attribute, values.get(i)));
        }

        return resolved;
    }

    /**
     * 构建字典序游标条件：{@code (a > v1) OR (a = v1 AND b < v2)}。
     *
     * @param cb     构建器
     * @param root   查询根
     * @param orders 排序项
     * @param values 游标值，允许为 {@code null}
     * @return 游标条件；未携带游标时返回 {@code null}
     */
    private Predicate keysetPredicate(
            CriteriaBuilder cb,
            Root<T> root,
            List<Sort.Order> orders,
            List<Object> values
    ) {
        if (values == null) {
            return null;
        }

        List<Predicate> equals = new ArrayList<>(orders.size());
        List<Predicate> terms = new ArrayList<>(orders.size());

        for (int i = 0; i < orders.size(); i++) {
            Sort.Order order = orders.get(i);
            Path<?> attribute = path(root, order.getProperty());
            Predicate comparison = comparison(
                    cb,
                    attribute,
                    order,
                    values.get(i)
            );

            if (equals.isEmpty()) {
                terms.add(comparison);
            } else {
                List<Predicate> parts = new ArrayList<>(equals.size() + 1);
                parts.addAll(equals);
                parts.add(comparison);
                terms.add(cb.and(parts.toArray(new Predicate[0])));
            }

            equals.add(cb.equal(attribute, values.get(i)));
        }

        return cb.or(terms.toArray(new Predicate[0]));
    }

    /**
     * 按排序方向生成比较条件。
     *
     * @param cb        构建器
     * @param attribute 排序路径
     * @param order     排序项
     * @param value     游标值
     * @return 比较条件
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Predicate comparison(
            CriteriaBuilder cb,
            Path<?> attribute,
            Sort.Order order,
            Object value
    ) {
        Path path = attribute;
        Comparable target = (Comparable) value;

        return order.isAscending()
                ? cb.greaterThan(path, target)
                : cb.lessThan(path, target);
    }

    /**
     * 把解码后的字符串游标转换为排序路径对应的 Java 类型。
     *
     * @param attribute 排序路径
     * @param raw       游标值
     * @return 转换后的值
     * @throws IllegalArgumentException 类型不支持时抛出
     */
    private Object convert(Path<?> attribute, Object raw) {
        if (!(raw instanceof String text)) {
            return raw;
        }

        Class<?> type = attribute.getJavaType();

        if (type == null || type == String.class || type == Object.class) {
            return text;
        }

        if (type == Integer.class || type == int.class) {
            return Integer.valueOf(text);
        }

        if (type == Long.class || type == long.class) {
            return Long.valueOf(text);
        }

        if (type == Short.class || type == short.class) {
            return Short.valueOf(text);
        }

        if (type == Byte.class || type == byte.class) {
            return Byte.valueOf(text);
        }

        if (type == Double.class || type == double.class) {
            return Double.valueOf(text);
        }

        if (type == Float.class || type == float.class) {
            return Float.valueOf(text);
        }

        if (type == BigDecimal.class) {
            return new BigDecimal(text);
        }

        if (type == BigInteger.class) {
            return new BigInteger(text);
        }

        if (type == Boolean.class || type == boolean.class) {
            return Boolean.valueOf(text);
        }

        if (type == UUID.class) {
            return UUID.fromString(text);
        }

        if (type == LocalDate.class) {
            return LocalDate.parse(text);
        }

        if (type == LocalDateTime.class) {
            return LocalDateTime.parse(text);
        }

        if (type == LocalTime.class) {
            return LocalTime.parse(text);
        }

        if (type == Instant.class) {
            return Instant.parse(text);
        }

        if (type.isEnum()) {
            return toEnum(type, text);
        }

        throw new IllegalArgumentException(
                "Unsupported cursor value type: " + type.getName()
        );
    }

    /**
     * 按枚举类型解析游标值。
     *
     * @param type 枚举类型
     * @param text 游标值
     * @return 枚举值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Enum<?> toEnum(Class<?> type, String text) {
        return Enum.valueOf((Class) type, text);
    }

    /**
     * 以最后一行的排序键值生成下一页游标。
     *
     * @param orders 排序项
     * @param last   当前页最后一行
     * @return 下一页游标
     */
    private KeysetCursor cursorOf(List<Sort.Order> orders, T last) {
        Object[] values = new Object[orders.size()];
        DirectFieldAccessFallbackBeanWrapper wrapper =
                new DirectFieldAccessFallbackBeanWrapper(last);

        for (int i = 0; i < orders.size(); i++) {
            values[i] = wrapper.getPropertyValue(
                    orders.get(i).getProperty()
            );
        }

        return KeysetCursor.of(values);
    }

    /**
     * 合并查询条件与游标条件。
     *
     * @param cb    构建器
     * @param left  查询条件，允许为 {@code null}
     * @param right 游标条件，允许为 {@code null}
     * @return 合并后的条件；两者都为空时返回 {@code null}
     */
    private Predicate combine(
            CriteriaBuilder cb,
            Predicate left,
            Predicate right
    ) {
        if (left == null) {
            return right;
        }

        if (right == null) {
            return left;
        }

        return cb.and(left, right);
    }

    /**
     * 校验实体主键为单一主键。
     *
     * @throws IllegalArgumentException 主键为复合主键时抛出
     */
    private void ensureSimplePrimaryKey() {
        if (entityType.isAnnotationPresent(IdClass.class)
                || hasEmbeddedId(entityType)) {
            throw new IllegalArgumentException(
                    "Keyset pagination does not support composite primary keys"
            );
        }
    }

    /**
     * 判断实体是否使用嵌入式主键。
     *
     * @param type 实体类型
     * @return 存在 {@code @EmbeddedId} 字段时返回 {@code true}
     */
    private boolean hasEmbeddedId(Class<?> type) {
        Class<?> current = type;

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(EmbeddedId.class)) {
                    return true;
                }
            }

            current = current.getSuperclass();
        }

        return false;
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

    /**
     * 校验分批消费参数。
     *
     * @param batchSize 每批数量
     * @param consumer  批次回调
     * @throws IllegalArgumentException 批次大小不大于 0 或回调为空
     */
    private void validateBatch(int batchSize, Consumer<?> consumer) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Batch size must be greater than zero"
            );
        }

        if (consumer == null) {
            throw new IllegalArgumentException(
                    "Consumer must not be null"
            );
        }
    }

    /**
     * 按批次消费结果流，并在每批回调后清理持久化上下文。
     *
     * <p>流始终通过 try-with-resources 关闭，保证正常结束、回调异常
     * 与提前退出都不会遗留未关闭的游标。</p>
     *
     * @param <R>       结果类型
     * @param stream    结果流
     * @param batchSize 每批数量
     * @param consumer  批次回调
     */
    private <R> void consumeBatches(
            Stream<R> stream,
            int batchSize,
            Consumer<? super List<R>> consumer
    ) {
        try (Stream<R> source = stream) {
            Iterator<R> iterator = source.iterator();
            List<R> batch = new ArrayList<>(
                    Math.min(batchSize, 1024)
            );

            while (iterator.hasNext()) {
                batch.add(iterator.next());

                if (batch.size() == batchSize) {
                    applyBatch(batch, consumer);
                    batch = new ArrayList<>(batchSize);
                }
            }

            if (!batch.isEmpty()) {
                applyBatch(batch, consumer);
            }
        }
    }

    /**
     * 执行一批回调并清理持久化上下文。
     *
     * @param <R>      结果类型
     * @param batch    当前批次
     * @param consumer 批次回调
     */
    private <R> void applyBatch(
            List<R> batch,
            Consumer<? super List<R>> consumer
    ) {
        consumer.accept(batch);

        entityManager.flush();
        entityManager.clear();
    }
}
