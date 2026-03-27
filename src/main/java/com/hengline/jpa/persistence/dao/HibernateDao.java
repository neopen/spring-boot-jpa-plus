package com.hengline.jpa.persistence.dao;

import org.springframework.data.domain.PageRequest;

import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;

/**
 * <b> jpa 占位符传参
 * <P>
 * 
 * @author He.hp
 * @date 2020年7月30日 上午10:17:48
 * @version 1.0
 */
public interface HibernateDao extends BaseDao {

	/**
	 * <b> 根据ID或其他值获得唯一数据
	 * <P>
	 * 占位符的参数（务必于sql语句参数位置对应）
	 * 
	 * @author He.hp
	 * @date 2020年7月30日 上午10:30:53
	 * @param sqlStr
	 * @param params
	 *            占位符的多个参数
	 * @return
	 * @throws Exception
	 */
	<T> T find(String sqlStr, Class<T> resultClass, Object... params) throws Exception;

	<T> T find(String sqlStr, Class<T> resultClass, Map<String, Object> params) throws Exception;

	/**
	 * <b> 获得列表
	 * <P>
	 * 占位符的参数（务必于sql语句参数位置对应）
	 * 
	 * @author He.hp
	 * @date 2020年7月30日 上午10:39:59
	 * @param <T>
	 * @param sqlStr
	 * @param resultClass
	 * @param params
	 *            占位符的多个参数
	 * @return
	 * @throws Exception
	 */
	<T> List<T> findAll(String sqlStr, Class<T> resultClass, Object... params) throws Exception;

	<T> List<T> findAll(String sqlStr, Class<T> resultClass, Map<String, Object> params) throws Exception;

	/**
	 * <b> 获得分页列表
	 * <P>
	 * 占位符的参数（务必于sql语句参数位置对应）
	 * 
	 * @author He.hp
	 * @date 2020年7月30日 上午10:40:45
	 * @param <T>
	 * @param sqlStr
	 * @param page
	 * @param resultClass
	 * @param params
	 *            占位符的多个参数
	 * @return
	 * @throws Exception
	 */
	<T> List<T> findPage(String sqlStr, PageRequest page, Class<T> resultClass, Object... params) throws Exception;

	<T> List<T> findPage(String sqlStr, PageRequest page, Class<T> resultClass, Map<String, Object> params) throws Exception;

	/**
	 * @author He.hp
	 * @Description 分页统计
	 * @date 2019年6月20日 下午7:11:35
	 * @param sqlStr
	 * @return
	 */
	Long count(String sqlStr, Object... params) throws Exception;

	Long count(String sqlStr, Map<String, Object> params) throws Exception;

	<T> int insertAll(List<T> params) throws Exception;

	/**
	 * <b> 设置查询参数
	 * <P>
	 * 
	 * @author He.hp
	 * @date 2020年7月29日 下午4:05:27
	 * @param query
	 * @param params
	 *            一定要与sql语句中的参数对应
	 */
	default void setQueryParameter(Query query, Object... params) {
		if (params == null || params.length <= 0)
			return;

		for (int i = 0; i < params.length; i++) {
			// query.setParameter(String.valueOf(i), params[i]);
			query.setParameter(i, params[i]);
		}
	}

	/**
	 * <b> 设置查询参数
	 * <P>
	 * 
	 * @author He.hp
	 * @date 2020年7月29日 下午4:05:27
	 * @param query
	 * @param params
	 *            一定要与sql语句中的参数对应
	 */
	default void setQueryParameter(Query query, Map<String, Object> params) {
		if (params == null || params.isEmpty())
			return;

		for (String paramName : params.keySet()) {
			if (paramName == null || paramName.isEmpty())
				continue;

			query.setParameter(paramName, params.get(paramName));
		}
	}

}
