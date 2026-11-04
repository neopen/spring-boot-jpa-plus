package io.github.neopen.convert.convert;

import io.github.neopen.convert.convert.vo.BaseVo;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * @param <V> 需要返回的vo对象，一般来自vo包，如：ParkBaseInfoVo
 * @param <T> 需要转换为vo的实体对象，一般来自entity包，如：ParkBaseInfo
 * @Author neopen
 * @email Haeng2015@163.com
 * @time 2018年11月2日 上午10:34:30
 * @do >>> 基本接口（提供了一些通用的方法，可选择重写自己需要的）
 */
public interface BaseVoService<@NotNull T, @NotNull V extends BaseVo> extends BaseServiceFactory {

    /**
     * @param entity
     * @return
     * @
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description 基本vo设置
     * @date 2019年5月28日 下午3:15:08
     */
    default <S extends V> BaseVoCommom<T, S> baseVo(@NotNull T entity) throws Exception {
        return (t) -> null;
    }

    /**
     * @param entity 传入的bo实体对象
     * @return
     * @
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @time 2018年11月1日 上午10:50:33
     * @do >>> bo转vo对象
     */
    default <S extends T> V parseVo(@NotNull S entity) throws Exception {
        return baseVo(entity).setCommonVo(entity);
    }

    /**
     * @param <S>
     * @param entity 传入的bo实体对象
     * @param map    其他map对象
     * @return
     * @
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description bo转vo对象
     * @date 2019年5月24日 下午3:51:23
     */
    default <S extends T> V parseVo(@NotNull S entity, Map<Object, ?> map) throws Exception {
        return parseVo(entity);
    }

    /**
     * @param <S>
     * @param entity 传入的bo实体对象
     * @param params 其他参数对象
     * @return
     * @
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Description bo转vo对象
     * @date 2019年5月24日 下午3:51:23
     */
    default <S extends T> V parseVo(@NotNull S entity, @Nullable Object... params) throws Exception {
        return parseVo(entity);
    }

}
