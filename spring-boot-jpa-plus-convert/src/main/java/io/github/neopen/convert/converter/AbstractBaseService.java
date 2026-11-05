
package io.github.neopen.convert.converter;

import io.github.neopen.convert.converter.vo.BaseVo;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * JPA Plus 基础转换服务。
 *
 * <p>提供单个及批量实体转换、分组数据转换和 Spring Data JPA
 * {@link Specification} 构建能力。业务实现类只需关注对象转换和
 * 查询条件逻辑。</p>
 *
 * <p>本类不负责 JSON 响应包装、HTTP 请求处理或数据库增删改查。</p>
 *
 * @param <V> 视图对象类型
 * @param <T> 实体类型
 * @param <P> 查询参数类型，允许为 {@code null}
 */
public abstract class AbstractBaseService<
        @NotNull V extends BaseVo,
        @NotNull T,
        @Null P>
        implements BaseVoService<T, V>, BasePredicateService<T, P> {

    /**
     * Entity 集合转换为 VO 集合。
     *
     * <p>
     * 如果实体集合为 {@code null} 或空，返回不可修改的空集合。
     * 如果某个实体转换结果为 {@code null}，则跳过该元素。
     *
     * @param entities Entity 集合
     * @return 视图对象集合
     * @throws Exception 实体转换失败时抛出
     */
    protected final List<V> batchParseVo(
            @Nullable Collection<? extends T> entities) throws Exception {

        return convertCollection(entities, this::parseVo);
    }

    /**
     * 批量转换，并允许控制关联对象是否参与转换。
     *
     * @param entities         Entity 集合
     * @param includeRelations 是否转换关联对象
     * @return 视图对象集合
     * @throws Exception 实体转换失败时抛出
     */
    protected final List<V> batchParseVo(
            @Nullable Collection<? extends T> entities,
            boolean includeRelations) throws Exception {

        return convertCollection(
                entities,
                entity -> parseVo(entity, includeRelations)
        );
    }

    /**
     * 批量转换，并传入额外的 Map 参数。
     *
     * @param entities Entity 集合
     * @param params   附加参数
     * @return 视图对象集合
     * @throws Exception 实体转换失败时抛出
     */
    protected final List<V> batchParseVo(
            @Nullable Collection<? extends T> entities,
            Map<Object, ?> params) throws Exception {

        return convertCollection(
                entities,
                entity -> parseVo(entity, params)
        );
    }

    /**
     * 批量转换，并传入可变参数。
     *
     * @param entities Entity 集合
     * @param params   附加参数
     * @return 视图对象集合
     * @throws Exception 实体转换失败时抛出
     */
    protected final List<V> batchParseVo(
            @Nullable Collection<? extends T> entities,
            Object... params) throws Exception {

        return convertCollection(
                entities,
                entity -> parseVo(entity, params)
        );
    }

    /**
     * Map 分组数据转换。
     *
     * <p>
     * 保留原 Map 的 Key，并将每个分组中的 Entity 集合转换为 VO 集合。
     *
     * @param groups 原始分组数据
     * @param sort   是否按 Key 自然排序
     * @return 转换后的分组数据，空分组不会写入结果
     * @throws Exception 实体转换失败时抛出
     */
    protected final Map<Object, List<V>> batchParseMapsVo(
            @Nullable Map<Object, ? extends List<T>> groups,
            boolean sort) throws Exception {

        if (groups == null || groups.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Object, List<V>> result = sort
                ? new TreeMap<>()
                : new HashMap<>(groups.size());

        for (Map.Entry<Object, ? extends List<T>> entry
                : groups.entrySet()) {

            List<V> vos = batchParseVo(entry.getValue());

            // 保留原来的行为：空分组不写入结果。
            if (!vos.isEmpty()) {
                result.put(entry.getKey(), vos);
            }
        }

        return result;
    }

    /**
     * Map 分组数据转换，支持自定义排序规则。
     *
     * <p>
     * 当 comparator 为 null 时保留输入 Map 的遍历顺序。
     * 当 comparator 不为 null 时，按照指定规则排序。
     *
     * @param groups     原始分组数据
     * @param comparator 键比较器；为 {@code null} 时保留输入遍历顺序
     * @return 转换后的分组数据，空分组不会写入结果
     * @throws Exception 实体转换失败时抛出
     */
    protected final Map<Object, List<V>> batchParseMapsVo(
            @Nullable Map<Object, ? extends List<T>> groups,
            @Nullable Comparator<Object> comparator) throws Exception {

        if (groups == null || groups.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Object, List<V>> result = comparator == null
                ? new LinkedHashMap<>(groups.size())
                : new TreeMap<>(comparator);

        for (Map.Entry<Object, ? extends List<T>> entry
                : groups.entrySet()) {

            List<V> vos = batchParseVo(entry.getValue());

            if (!vos.isEmpty()) {
                result.put(entry.getKey(), vos);
            }
        }

        return result;
    }

    /**
     * Map 的 Value 为单个 Entity 时，批量转换为 VO 列表。
     *
     * @param source 原始 Map
     * @return 视图对象列表
     * @throws Exception 实体转换失败时抛出
     */
    protected final List<V> batchParseMapVo(
            @Nullable Map<Object, ? extends T> source) throws Exception {

        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }

        List<V> result = new ArrayList<>(source.size());

        for (T entity : source.values()) {

            if (entity == null) {
                continue;
            }

            V vo = parseVo(entity);

            if (vo != null) {
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 统一的集合转换实现。
     *
     * <p>
     * 使用转换函数消除多个 batchParseVo 方法中的重复循环。
     * 转换异常直接向上传递，不静默忽略。
     *
     * @param entities  实体集合
     * @param converter 支持受检异常的转换函数
     * @return 视图对象集合
     * @throws Exception 实体转换失败时抛出
     */
    private List<V> convertCollection(
            @Nullable Collection<? extends T> entities,
            ThrowingFunction<T, V> converter) throws Exception {

        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        List<V> result = new ArrayList<>(entities.size());

        for (T entity : entities) {

            if (entity == null) {
                continue;
            }

            V vo = converter.apply(entity);

            if (vo != null) {
                result.add(vo);
            }
        }

        return result;
    }

    /**
     * 构建默认查询条件。
     *
     * @param <Z>   具体查询参数类型
     * @param param 查询参数
     * @return 可交给 Spring Data JPA 执行的查询规范
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(
            @Nullable Z param) {

        return buildSpecification(
                (root, query, cb) ->
                        getPredicates(root, query, cb, param)
        );
    }

    /**
     * 构建带布尔扩展参数的查询条件。
     *
     * @param <Z>          具体查询参数类型
     * @param param        查询参数
     * @param includeExtra 是否启用额外查询条件
     * @return 可交给 Spring Data JPA 执行的查询规范
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(
            @Nullable Z param,
            boolean includeExtra) {

        return buildSpecification(
                (root, query, cb) ->
                        getPredicates(
                                root, query, cb, param, includeExtra
                        )
        );
    }

    /**
     * 构建带 Map 参数的查询条件。
     *
     * @param <Z>    具体查询参数类型
     * @param param  查询参数
     * @param params 附加参数
     * @return 可交给 Spring Data JPA 执行的查询规范
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(
            @Nullable Z param,
            @Nullable Map<Object, ?> params) {

        return buildSpecification(
                (root, query, cb) ->
                        getPredicates(root, query, cb, param, params)
        );
    }

    /**
     * 构建带可变参数的查询条件。
     *
     * @param <Z>    具体查询参数类型
     * @param param  查询参数
     * @param params 附加参数
     * @return 可交给 Spring Data JPA 执行的查询规范
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(
            @Nullable Z param,
            @Nullable Object... params) {

        return buildSpecification(
                (root, query, cb) ->
                        getPredicates(root, query, cb, param, params)
        );
    }

    /**
     * 统一构建 Specification。
     *
     * <p>
     * 处理规则：
     * 1. 没有查询条件时返回 conjunction，即恒为 true；
     * 2. 过滤 null Predicate；
     * 3. 多个 Predicate 默认采用 AND 组合；
     * 4. 条件构建失败时抛出异常，不返回 null。
     *
     * <p>
     * 注意：不要在这里清空 predicates。
     * Criteria API 返回的 Predicate 由 JPA Provider 管理其查询语义。
     *
     * @param resolver 查询谓词解析函数
     * @return 组合业务查询条件的查询规范
     */
    private Specification<T> buildSpecification(
            PredicateResolver<T> resolver) {

        return (root, query, cb) -> {

            try {

                List<Predicate> predicates =
                        resolver.resolve(root, query, cb);

                if (predicates == null || predicates.isEmpty()) {
                    return cb.conjunction();
                }

                Predicate[] conditions = predicates.stream()
                        .filter(Objects::nonNull)
                        .toArray(Predicate[]::new);

                if (conditions.length == 0) {
                    return cb.conjunction();
                }

                return cb.and(conditions);

            } catch (Exception ex) {

                throw new IllegalStateException(
                        "构建 JPA Specification 查询条件失败",
                        ex
                );
            }
        };
    }

    /**
     * JPA 查询谓词构建函数。
     *
     * @param <E> 实体类型
     */
    @FunctionalInterface
    private interface PredicateResolver<E> {

        /**
         * 解析业务查询谓词。
         *
         * @param root  查询根路径
         * @param query Criteria 查询
         * @param cb    Criteria 构建器
         * @return 查询谓词列表
         * @throws Exception 查询条件构建失败时抛出
         */
        List<Predicate> resolve(
                jakarta.persistence.criteria.Root<E> root,
                jakarta.persistence.criteria.CriteriaQuery<?> query,
                jakarta.persistence.criteria.CriteriaBuilder cb
        ) throws Exception;
    }

    /**
     * 带受检异常的转换函数。
     *
     * @param <I> 输入类型
     * @param <O> 输出类型
     */
    @FunctionalInterface
    private interface ThrowingFunction<I, O> {

        /**
         * 转换输入对象。
         *
         * @param input 输入对象
         * @return 转换后的输出对象
         * @throws Exception 转换失败时抛出
         */
        O apply(I input) throws Exception;
    }

}
