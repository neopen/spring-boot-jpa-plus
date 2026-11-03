package io.github.neopen.jpa.persistence.dao;


import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * Copyright: Copyright (c) 2019
 * </p>
 * <p>
 * Company: www.citycloud.com.cn
 * </p>
 * <b></b>
 */
public abstract class AbstractBaseDao implements HibernateDao {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.citycloud.utm.sys.multiple.persistence.dao.BaseDao#hqlParamAppendLikeStr(java.lang.String)
	 */
	@Override
	public String paramAppendLikeStr(String str) {
		if (str == null)
			return null;
		return "%" + str + "%";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.citycloud.utm.sys.multiple.persistence.dao.base.BaseStrDao#hqlParamInListToStr(java.util.List)
	 */
	@Override
	public String paramInListToStr(List<Object> list) {
		if (list == null || list.isEmpty())
			return "";

		return list.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.citycloud.sanitation.common.dao.base.BaseStrDao#hqlParamListToStr(java.util.List)
	 */
	@Override
	public String paramInListToStr(List<String> list, boolean quotation) {
		if (list == null || list.isEmpty())
			return "";

		return quotation ? list.stream().collect(Collectors.joining(",", "'", "'")) : list.stream().collect(Collectors.joining(","));
	}

}
