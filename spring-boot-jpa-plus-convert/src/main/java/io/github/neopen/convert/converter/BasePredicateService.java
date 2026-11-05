package io.github.neopen.convert.converter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JPA Criteria 查询谓词构建服务。
 *
 * <p>业务类可通过重写 {@code buildPredicates} 方法，将查询参数
 * 转换为 Spring Data JPA 所需的查询谓词。</p>
 *
 * @param <T> 实体类型
 * @param <P> 查询参数类型
 */
public interface BasePredicateService<@NotNull T, @NotNull P> extends BaseServiceFactory {

    /**
     * 获取业务定义的查询谓词。
     *
     * @param root  查询根路径
     * @param query Criteria 查询
     * @param cb    Criteria 构建器
     * @param param 查询参数
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> getPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param) throws Exception {

        return buildPredicates(root, query, cb, param);
    }

    /**
     * 获取业务定义的查询谓词。
     *
     * @param root    查询根路径
     * @param query   Criteria 查询
     * @param cb      Criteria 构建器
     * @param param   查询参数
     * @param isParam 是否启用查询参数对应的扩展条件
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> getPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param,
            boolean isParam) throws Exception {

        return buildPredicates(root, query, cb, param, isParam);
    }

    /**
     * 获取业务定义的查询谓词。
     *
     * @param root  查询根路径
     * @param query Criteria 查询
     * @param cb    Criteria 构建器
     * @param param 查询参数
     * @param map   扩展参数
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> getPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param,
            @Nullable Map<Object, ?> map) throws Exception {

        return buildPredicates(root, query, cb, param, map);
    }

    /**
     * 获取业务定义的查询谓词。
     *
     * @param root   查询根路径
     * @param query  Criteria 查询
     * @param cb     Criteria 构建器
     * @param param  查询参数
     * @param params 扩展参数
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> getPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param,
            @Nullable Object... params) throws Exception {

        return buildPredicates(root, query, cb, param, params);
    }

    /**
     * 构建业务查询谓词。
     * <p>
     * 业务类可以重写此方法，实现自定义查询条件。
     *
     * @param root  查询根路径
     * @param query Criteria 查询
     * @param cb    Criteria 构建器
     * @param param 查询参数
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> buildPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param) throws Exception {

        List<Predicate> predicates = new ArrayList<>(16);
        return predicates;
    }

    /**
     * 构建业务查询谓词。
     *
     * @param root    查询根路径
     * @param query   Criteria 查询
     * @param cb      Criteria 构建器
     * @param param   查询参数
     * @param isParam 是否启用查询参数对应的扩展条件
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> buildPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param,
            boolean isParam) throws Exception {

        return buildPredicates(root, query, cb, param);
    }

    /**
     * 构建业务查询谓词。
     *
     * @param root  查询根路径
     * @param query Criteria 查询
     * @param cb    Criteria 构建器
     * @param param 查询参数
     * @param map   扩展参数
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> buildPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param,
            @Nullable Map<Object, ?> map) throws Exception {

        return buildPredicates(root, query, cb, param);
    }

    /**
     * 构建业务查询谓词。
     *
     * @param root   查询根路径
     * @param query  Criteria 查询
     * @param cb     Criteria 构建器
     * @param param  查询参数
     * @param params 扩展参数
     * @return 查询谓词列表
     * @throws Exception 查询条件构建失败时抛出
     */
    default List<Predicate> buildPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param,
            @Nullable Object... params) throws Exception {

        return buildPredicates(root, query, cb, param);
    }


}
