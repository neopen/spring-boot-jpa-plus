package io.github.neopen.jpa.convert;

import io.github.neopen.jpa.bean.Json;
import io.github.neopen.jpa.bean.vo.BaseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 * 基本接口实现，抽象类（供继承，实现了批量转换vo、分页、jpa条件查询方法，无需改动），P参数可以为null
 * </p>
 *
 * @author He.hp
 * @date 2019年5月8日 下午3:58:31
 */
@Slf4j
public abstract class AbstractBaseService<@NotNull V extends BaseVo, @NotNull T, @Null P> implements BaseVoService<T, V>, BasePredicateService<T, P> {

    /**
     * @param entityList 传入的bo实体列表对象
     * @return
     * @throws Exception
     * @author he.hp
     * @time 2018年11月1日 上午10:50:33
     * @do >>> 批量bo转vo列表对象
     */
    protected final List<V> batchParseVo(Collection<? extends T> entityList) throws Exception {
        if (entityList == null || entityList.isEmpty())
            return Collections.emptyList();

        List<V> entityVos = new ArrayList<>(entityList.size());

        for (T entity : entityList) {
            V vo = parseVo(entity);
            if (vo == null)
                continue;

            entityVos.add(vo);
        }

        // entityList.stream().forEach(handlingConsumerWrapper(t ->
        // entityVos.add(parseVo(t)), Exception.class));
        // entityVos.stream().filter(v -> v != null);

        return entityVos;
    }

    /**
     * @param entityList 传入的bo实体列表对象
     * @param isTrue     是否忽略某些对象的转换（如外键对象）
     * @return
     * @throws Exception
     * @author he.hp
     * @time 2018年12月21日 下午1:57:46
     * @do >>> 批量bo转vo列表对象
     */
    protected final List<V> batchParseVo(Collection<? extends T> entityList, boolean isTrue) throws Exception {
        if (entityList == null || entityList.isEmpty())
            return Collections.emptyList();

        List<V> entityVos = new ArrayList<>(entityList.size());

        for (T entity : entityList) {
            V vo = parseVo(entity, isTrue);
            if (vo == null)
                continue;

            entityVos.add(vo);
        }

        return entityVos;

        // return entityList.stream().map(t -> {
        // return parseVo(t, obj);
        // }).filter(v -> v != null).collect(Collectors.toList());
    }

    /**
     * @param entityList 传入的bo实体列表对象
     * @param map        其他map对象
     * @return
     * @throws Exception
     * @author he.hp
     * @Description 批量bo转vo列表对象
     * @date 2019年5月24日 下午3:53:07
     */
    protected final List<V> batchParseVo(Collection<? extends T> entityList, Map<Object, ?> map) throws Exception {
        if (entityList == null || entityList.isEmpty())
            return Collections.emptyList();

        List<V> entityVos = new ArrayList<>(entityList.size());

        for (T entity : entityList) {
            V vo = parseVo(entity, map);
            if (vo == null)
                continue;

            entityVos.add(vo);
        }

        return entityVos;
    }

    /**
     * @param maps 传入的bo实体列表对象，根据指定的某属性kry，分组转为vos
     * @return
     * @throws Exception
     * @author he.hp
     * @Description 批量bo转vo列表对象
     * @date 2019年5月24日 下午3:53:07
     */
    protected final Map<Object, List<V>> batchParseMapsVo(Map<Object, List<T>> maps, boolean sort) throws Exception {
        if (maps == null || maps.isEmpty())
            return Collections.emptyMap();

        Map<Object, List<V>> entityMapsVos = sort? new TreeMap<>() : new HashMap<>(maps.size());

        maps.forEach((k, v) -> {

            if (v == null || v.isEmpty())
                return;

            List<V> vos = new ArrayList<>(v.size());

            v.forEach(t -> {

                V vo = null;
                try {
                    vo = parseVo(t);
                } catch (Exception e) {
                    return;
                }
                if (vo != null)
                    vos.add(vo);
            });

            entityMapsVos.put(k, vos);
        });

        return entityMapsVos;
    }

    /**
     * @param map 传入的bo实体列表对象，根据指定的某属性kry，分组转为vos
     * @return
     * @throws Exception
     * @author he.hp
     * @Description 批量bo转vo列表对象
     * @date 2019年5月24日 下午3:53:07
     */
    protected final List<V> batchParseMapVo(Map<Object, T> map) throws Exception {
        if (map == null || map.isEmpty())
            return Collections.emptyList();

        List<V> vos = new ArrayList<>(map.size());

        map.forEach((k, v) -> {

            if (v == null)
                return;

            V vo = null;
            try {
                vo = parseVo(v);
            } catch (Exception e) {
                return;
            }

            if (vo != null)
                vos.add(vo);
        });

        return vos;
    }

    /**
     * @param entityList 传入的bo实体列表对象
     * @param params     其他参数对象
     * @return
     * @throws Exception
     * @author he.hp
     * @Description 批量bo转vo列表对象
     * @date 2019年5月24日 下午3:53:07
     */
    protected final List<V> batchParseVo(Collection<? extends T> entityList, @Nullable Object... params) throws Exception {
        if (entityList == null || entityList.isEmpty())
            return Collections.emptyList();

        List<V> entityVos = new ArrayList<>(entityList.size());

        for (T entity : entityList) {
            V vo = parseVo(entity, params);
            if (vo == null)
                continue;

            entityVos.add(vo);
        }

        return entityVos;
    }

    /**
     * @param param param参数对象（从客户端接收数据的对象）
     * @return
     * @throws Exception
     * @author he.hp
     * @time 2018年12月11日 下午3:28:26
     * @do >>> jpa的多条件查询方法(SQL拼接，连表查询搜索)
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(Z param) throws Exception {
        return (root, query, cb) -> {
            List<Predicate> predicates = null;
            try {
                predicates = getPredicates(root, query, cb, param);
                if (predicates == null)
                    predicates = new ArrayList<>(10);

                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            } catch (Exception e) {
                log.error("getPredicates() Exception occured : " + e.getMessage());
            } finally {
                if (predicates != null)
                    predicates.clear();
            }
            return null;
        };
    }

    /**
     * @param param     param参数对象（从客户端接收数据的对象）
     * @param otherProp 其他参数
     * @return
     * @throws Exception
     * @author he.hp
     * @time 2018年12月11日 下午3:28:26
     * @do >>> jpa的多条件查询方法
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(Z param, boolean otherProp) throws Exception {
        return (root, query, cb) -> {
            List<Predicate> predicates = null;
            try {
                predicates = getPredicates(root, query, cb, param, otherProp);
                if (predicates == null)
                    predicates = new ArrayList<>(10);

                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            } catch (Exception e) {
                log.error("getPredicates() Exception occured : " + e.getMessage());
            } finally {
                if (predicates != null)
                    predicates.clear();
            }
            return null;
        };
    }

    /**
     * @param param  param参数对象（从客户端接收数据的对象）
     * @param params 其他参数
     * @return
     * @throws Exception
     * @author he.hp
     * @time 2018年12月11日 下午3:28:26
     * @do >>> jpa的多条件查询方法
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(Z param, @Nullable Object... params) throws Exception {
        return (root, query, cb) -> {
            List<Predicate> predicates = null;
            try {
                predicates = getPredicates(root, query, cb, param, params);
                if (predicates == null)
                    predicates = new ArrayList<>(10);

                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            } catch (Exception e) {
                log.error("getPredicates() Exception occured : " + e.getMessage());
            } finally {
                if (predicates != null)
                    predicates.clear();
            }
            return null;
        };
    }

    /**
     * @param param param参数对象（从客户端接收数据的对象）
     * @param map   其他参数
     * @return
     * @throws Exception
     * @author he.hp
     * @time 2018年12月11日 下午3:28:26
     * @do >>> jpa的多条件查询方法
     */
    protected final <Z extends P> Specification<T> getSpecExecutor(Z param, @Nullable Map<String, Object> map) throws Exception {
        return (root, query, cb) -> {
            List<Predicate> predicates = null;
            try {
                predicates = getPredicates(root, query, cb, param, map);
                if (predicates == null)
                    predicates = new ArrayList<>(10);

                return cb.and(predicates.toArray(new Predicate[predicates.size()]));
            } catch (Exception e) {
                log.error("getPredicates() Exception occured : " + e.getMessage());
            } finally {
                if (predicates != null)
                    predicates.clear();
            }
            return null;
        };
    }

    /**
     * <b> vo基本分页函数
     * <P> 不需要转换了
     *
     * @param vos
     * @param count
     * @return com.citycloud.sanitation.common.bean.model.Json<java.util.List < V>>
     * @Author He.hp
     * @Email hehp@citycloud.com.cn
     * @Date 2021/4/15 14:59
     */
    protected final Json<List<V>> getVoPage(List<V> vos, Long count) throws Exception {
        if (vos == null)
            return getEmptyPage();

        return Json.getSucc(vos, count);
    }

    /**
     * <b> 基本分页函数
     * <p>
     *
     * @param list
     * @param count
     * @return com.citycloud.sanitation.common.bean.model.Json<java.util.List < V>>
     * @Author He.hp
     * @Email hehp@citycloud.com.cn
     * @Date 2021/4/15 14:59
     */
    protected final Json<List<V>> getCommonPage(Collection<T> list, Long count) throws Exception {
        if (list == null || list.isEmpty())
            return getEmptyPage();

        return Json.getSucc(batchParseVo(list), count);
    }

    /**
     * @param commonPage
     * @return
     * @throws Exception
     * @author He.hp
     * @Description 获得分页数据对象
     * @date 2019年6月6日 下午4:52:29
     */
    protected final Json<List<V>> getCommonPage(Page<T> commonPage) throws Exception {
        if (commonPage == null)
            return getEmptyPage();

        return Json.getSucc(batchParseVo(commonPage.getContent()), commonPage.getTotalElements());
    }

    /**
     * @param commonPage
     * @return
     * @throws Exception
     * @author He.hp
     * @Description 获得分页数据对象
     * @date 2019年6月6日 下午4:52:29
     */
    protected final Json<List<V>> getCommonPage(Page<T> commonPage, boolean param) throws Exception {
        if (commonPage == null)
            return getEmptyPage();

        return Json.getSucc(batchParseVo(commonPage.getContent(), param), commonPage.getTotalElements());
    }

    /**
     * @param commonPage
     * @return
     * @throws Exception
     * @author He.hp
     * @Description 获得分页数据对象
     * @date 2019年6月6日 下午4:52:29
     */
    protected final Json<List<V>> getCommonPage(Page<T> commonPage, Map<Object, ?> map) throws Exception {
        if (commonPage == null)
            return getEmptyPage();

        return Json.getSucc(batchParseVo(commonPage.getContent(), map), commonPage.getTotalElements());
    }

    /**
     * @param commonPage
     * @return
     * @throws Exception
     * @author He.hp
     * @Description 获得分页数据对象
     * @date 2019年6月6日 下午4:52:29
     */
    protected final Json<List<V>> getCommonPage(Page<T> commonPage, Object... params) throws Exception {
        if (commonPage == null)
            return getEmptyPage();

        return Json.getSucc(batchParseVo(commonPage.getContent(), params), commonPage.getTotalElements());
    }

    /**
     * <b> 获得空分页对象
     * <p>
     *
     * @return com.citycloud.sanitation.common.bean.model.Json<java.util.List < V>>
     * @Author He.hp
     * @Date 2021/4/13 14:41
     */
    protected final Json<List<V>> getEmptyPage() throws Exception {
        return Json.getSucc(Collections.emptyList(), 0l);

    }

    /**
     * @param consumer
     * @param clazz
     * @return
     * @author He.hp
     * @Description >>> 异常解决方法
     * @date 2019年4月2日 下午2:06:38
     */
    protected static <T, E extends Exception> Consumer<T> consumerWrapper(Consumer<T> consumer, Class<E> clazz) {
        return i -> {
            try {
                consumer.accept(i);
            } catch (Exception ex) {
                try {
                    E exCast = clazz.cast(ex);
                    log.error("Exception occured : " + exCast.getMessage());
                } catch (ClassCastException ccEx) {
                    throw ex;
                }
            }
        };
    }

    /**
     * @param throwingConsumer
     * @param exceptionClass
     * @return
     * @author He.hp
     * @email Haeng2015@163.com
     * @Description >>> 异常解决方法(自定义接口方法)
     * @date 2019年4月2日 下午2:15:11
     */
    protected static <T, E extends Exception> Consumer<T> handlingConsumerWrapper(ThrowingConsumer<T, E> throwingConsumer, Class<E> exceptionClass) {
        return i -> {
            try {
                throwingConsumer.acceptException(i);
            } catch (Exception ex) {
                try {
                    E exCast = exceptionClass.cast(ex);
                    log.error("Exception occured : " + exCast.getMessage());
                } catch (ClassCastException ccEx) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

}
