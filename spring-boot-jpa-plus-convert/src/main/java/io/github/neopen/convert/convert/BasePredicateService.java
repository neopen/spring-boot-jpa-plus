package io.github.neopen.convert.convert;

import org.springframework.lang.Nullable;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @param <T> 需要转换为vo的实体对象，一般来自entity包，如：ParkBaseInfo
 * @param <P> param参数对象，一般来自params包，如：ParkBaseInfoParam
 * @Author neopen
 * @email Haeng2015@163.com
 * @time 2018年11月2日 上午10:34:30
 * @do >>> 基本接口（提供了一些通用的方法，可选择重写自己需要的）
 */
public interface BasePredicateService<@NotNull T, @NotNull P> extends BaseServiceFactory {

    /**
     * @param <S>
     * @param root
     * @param query
     * @param cb
     * @param param param参数对象（从客户端接收数据的对象）
     * @return
     * @throws Exception
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description 获得Predicate列表（jpa的多条件查询方法）
     * @date 2019年5月24日 下午1:27:14
     */
    default <S extends P> List<Predicate> getPredicates(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, @Nullable S param) throws Exception {
        List<Predicate> predicates = new ArrayList<>(16);

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
     * @param <Z>
     * @param root
     * @param query
     * @param cb
     * @param param   param参数对象（从客户端接收数据的对象）
     * @param isParam 其他参数
     * @return
     * @throws Exception
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description jpa的多条件查询方法
     * @date 2019年5月24日 下午1:26:15
     */
    default <Z extends P> List<Predicate> getPredicates(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, @Nullable Z param, boolean isParam) throws Exception {
        return this.getPredicates(root, query, cb, param);
    }

    /**
     * @param <Z>
     * @param root
     * @param query
     * @param cb
     * @param param param参数对象（从客户端接收数据的对象）
     * @param map   其他参数
     * @return
     * @throws Exception
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description jpa的多条件查询方法
     * @date 2019年5月24日 下午1:26:15
     */
    default <Z extends P> List<Predicate> getPredicates(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, @Nullable Z param, Map<Object, ?> map) throws Exception {
        return this.getPredicates(root, query, cb, param);
    }

    /**
     * @param <Z>
     * @param root
     * @param query
     * @param cb
     * @param param  param参数对象（从客户端接收数据的对象）
     * @param params 其他参数
     * @return
     * @throws Exception
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description jpa的多条件查询方法
     * @date 2019年5月24日 下午1:26:15
     */
    default <Z extends P> List<Predicate> getPredicates(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb, @Nullable Z param, @Nullable Object... params) throws Exception {
        return this.getPredicates(root, query, cb, param);
    }

}
