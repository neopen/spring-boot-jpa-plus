package com.hengline.jpa.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <b> 基础vo
 * <P>
 * 如果不需要序列化，用transient修饰即可
 * 
 * @author He.hp
 * @date 2020年5月18日 下午1:59:59
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseVo implements Serializable {

	private Object id;

	/** serialVersionUID */
	private static final long serialVersionUID = 1053219950568557994L;
}
