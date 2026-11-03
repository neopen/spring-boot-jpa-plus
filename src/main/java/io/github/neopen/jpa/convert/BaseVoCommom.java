package io.github.neopen.jpa.convert;

import io.github.neopen.jpa.bean.vo.BaseVo;
import org.springframework.lang.Nullable;

/**
 * <p>
 * Copyright: Copyright (c) 2019
 * </p>
 * <p>
 * Company: www.citycloud.com.cn
 * </p>
 * <b>设置基本vo接口</b>
 */
@FunctionalInterface
public interface BaseVoCommom<T, V extends BaseVo> {

    /**
     * @param entity
     * @throws Exception
     * @author He.hp
     * @Description 设置 vo基本信息
     * @date 2019年5月28日 下午2:36:59
     */
    @Nullable
    V setCommonVo(T entity);

}
