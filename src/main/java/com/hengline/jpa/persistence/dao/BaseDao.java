package com.hengline.jpa.persistence.dao;

import java.util.List;

/**
 * BaseDao
 * 
 * @author He.hp
 * @date 2019年6月21日 上午10:37:19
 * @version 1.0
 * @description 基本dao接口
 */
public interface BaseDao {

	/**
	 * @author He.hp
	 * @Description hql模糊匹配参数拼接
	 * @date 2019年6月22日 下午2:41:16
	 * @param str
	 * @return
	 */
	String paramAppendLikeStr(String str);

	/**
	 * <b> in方法，list转字符串，并添加逗号
	 * <P>
	 * 
	 * @author He.hp
	 * @date 2019年9月30日 上午10:27:31
	 * @param list
	 * @return
	 */
	String paramInListToStr(List<Object> list);

	/**
	 * <b> list转字符串，并添加逗号
	 * <P>
	 * 
	 * @author He.hp
	 * @date 2020年7月30日 上午10:06:42
	 * @param list
	 * @param quotation
	 *            是否加单引号
	 * @return
	 */
	String paramInListToStr(List<String> list, boolean quotation);
}