package io.github.neopen.convert.converter.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 基础视图对象。
 *
 * <p>提供通用标识属性并支持序列化。不需要序列化的字段可使用
 * {@code transient} 修饰。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseVo implements Serializable {

    /** 视图对象的通用标识。 */
    private Object id;

    /** 序列化版本号。 */
	private static final long serialVersionUID = 1053219950568557994L;
}
