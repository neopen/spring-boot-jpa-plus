package io.github.neopen.convert.converter;

import io.github.neopen.convert.converter.vo.BaseVo;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 实体到视图对象的转换服务。
 *
 * <p>业务类可重写默认方法，以支持基础转换及携带扩展参数的转换。</p>
 *
 * @param <T> 实体类型
 * @param <V> 视图对象类型
 */
public interface BaseVoService<@NotNull T, @NotNull V extends BaseVo> extends BaseServiceFactory {

    /**
     * 创建基础视图对象转换函数。
     *
     * <p>默认实现返回的转换函数产生 {@code null}，业务类通常需要重写
     * 此方法或 {@link #parseVo(Object)}。</p>
     *
     * @param <S>    具体视图对象类型
     * @param entity 待转换实体
     * @return 视图对象转换函数
     * @throws Exception 创建转换函数失败时抛出
     */
    default <S extends V> BaseVoCommom<T, S> baseVo(@NotNull T entity) throws Exception {
        return (t) -> null;
    }

    /**
     * 将实体转换为视图对象。
     *
     * @param <S>    具体实体类型
     * @param entity 待转换实体
     * @return 转换后的视图对象，允许为 {@code null}
     * @throws Exception 转换失败时抛出
     */
    default <S extends T> V parseVo(@NotNull S entity) throws Exception {
        return baseVo(entity).setCommonVo(entity);
    }

    /**
     * 使用映射形式的扩展参数转换实体。
     *
     * <p>默认忽略扩展参数，业务类可按需重写。</p>
     *
     * @param <S>    具体实体类型
     * @param entity 待转换实体
     * @param map    扩展参数映射
     * @return 转换后的视图对象，允许为 {@code null}
     * @throws Exception 转换失败时抛出
     */
    default <S extends T> V parseVo(@NotNull S entity, Map<Object, ?> map) throws Exception {
        return parseVo(entity);
    }

    /**
     * 使用可变扩展参数转换实体。
     *
     * <p>默认忽略扩展参数，业务类可按需重写。</p>
     *
     * @param <S>    具体实体类型
     * @param entity 待转换实体
     * @param params 扩展参数，允许为 {@code null}
     * @return 转换后的视图对象，允许为 {@code null}
     * @throws Exception 转换失败时抛出
     */
    default <S extends T> V parseVo(@NotNull S entity, @Nullable Object... params) throws Exception {
        return parseVo(entity);
    }

}
