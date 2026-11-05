package io.github.neopen.convert.converter;

import io.github.neopen.convert.converter.vo.BaseVo;
import org.springframework.lang.Nullable;

/**
 * 基础视图对象转换函数。
 *
 * @param <T> 实体类型
 * @param <V> 视图对象类型
 */
@FunctionalInterface
public interface BaseVoCommom<T, V extends BaseVo> {

    /**
     * 将实体转换为基础视图对象。
     *
     * @param entity 待转换实体
     * @return 转换后的视图对象，允许为 {@code null}
     */
    @Nullable
    V setCommonVo(T entity);

}
