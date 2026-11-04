package io.github.neopen.convert.convert;

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
 * @param <T> 需要转换为vo的实体对象，一般来自entity包，如：ParkBaseInfo
 * @param <P> param参数对象，一般来自params包，如：ParkBaseInfoParam
 * @Author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @time 2018年11月2日 上午10:34:30
 * @do >>> 基本接口（提供了一些通用的方法，可选择重写自己需要的）
 */
public interface BasePredicateService<@NotNull T, @NotNull P> extends BaseServiceFactory {

    /**
     * 获取查询条件
     *
     * @param root  Root
     * @param query CriteriaQuery
     * @param cb    CriteriaBuilder
     * @param param 查询参数
     * @return Predicate 条件集合
     * @throws Exception 异常
     */
    default List<Predicate> getPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param) throws Exception {

        return buildPredicates(root, query, cb, param);
    }

    /**
     * 获取查询条件
     *
     * @param root    Root
     * @param query   CriteriaQuery
     * @param cb      CriteriaBuilder
     * @param param   查询参数
     * @param isParam 是否使用查询参数
     * @return Predicate 条件集合
     * @throws Exception 异常
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
     * 获取查询条件
     *
     * @param root  Root
     * @param query CriteriaQuery
     * @param cb    CriteriaBuilder
     * @param param 查询参数
     * @param map   扩展参数
     * @return Predicate 条件集合
     * @throws Exception 异常
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
     * 获取查询条件
     *
     * @param root   Root
     * @param query  CriteriaQuery
     * @param cb     CriteriaBuilder
     * @param param  查询参数
     * @param params 扩展参数
     * @return Predicate 条件集合
     * @throws Exception 异常
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
     * 构建查询条件
     * <p>
     * 业务类可以重写此方法，实现自定义查询条件。
     *
     * @param root  Root
     * @param query CriteriaQuery
     * @param cb    CriteriaBuilder
     * @param param 查询参数
     * @return Predicate 条件集合
     * @throws Exception 异常
     */
    default List<Predicate> buildPredicates(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            @Nullable P param) throws Exception {

        List<Predicate> predicates = new ArrayList<>(16);

        // 查询条件示例：
        // if (!StringUtils.isEmpty(param.getUserName())) { // 连表查询
        // Join<SysUser, SysSourceStockIn> joinUser = root.join("sysUser", JoinType.LEFT);
        // predicates.add(cb.like(joinUser.get("userName").as(String.class), "%" + param.getUserName() + "%"));
        // }
        //
        // if (param.getStatus() != null) {
        // predicates.add(cb.equal(root.<String>get("status"), param.getStatus()));
        // }
        //
        // if (!StringUtils.isEmpty(param.getStartDate())) {
        // predicates.add(cb.greaterThanOrEqualTo(root.<String>get("createTime"), param.getStartDate()));
        // }
        // if (!StringUtils.isEmpty(param.getEndDate())) {
        // predicates.add(cb.lessThanOrEqualTo(root.<String>get("createTime"), param.getEndDate()));
        // }

//            predicates.add(cb.isFalse(root.get("disable")));
//
//        if (ObjectUtil.isNotNull(param.getGroupId()))
//            predicates.add(cb.equal(root.<Integer>get("groupId"), param.getGroupId()));
//
//        if (StrUtil.isNotBlank(param.getCode()))
//            predicates.add(cb.like(root.<String>get("code"), "%" + param.getCode() + "%"));
//
//        if (StrUtil.isNotBlank(param.getCodePlate()))
//            predicates.add(cb.or(cb.like(root.<String>get("code"), "%" + param.getCodePlate() + "%"), cb.like(root.<String>get("plateNum"), "%" + param.getCodePlate() + "%")));

        return predicates;
    }

    /**
     * 构建查询条件
     *
     * @param root    Root
     * @param query   CriteriaQuery
     * @param cb      CriteriaBuilder
     * @param param   查询参数
     * @param isParam 是否使用查询参数
     * @return Predicate 条件集合
     * @throws Exception 异常
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
     * 构建查询条件
     *
     * @param root  Root
     * @param query CriteriaQuery
     * @param cb    CriteriaBuilder
     * @param param 查询参数
     * @param map   扩展参数
     * @return Predicate 条件集合
     * @throws Exception 异常
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
     * 构建查询条件
     *
     * @param root   Root
     * @param query  CriteriaQuery
     * @param cb     CriteriaBuilder
     * @param param  查询参数
     * @param params 扩展参数
     * @return Predicate 条件集合
     * @throws Exception 异常
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
