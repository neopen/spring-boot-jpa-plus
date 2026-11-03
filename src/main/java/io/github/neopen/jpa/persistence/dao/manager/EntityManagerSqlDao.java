package io.github.neopen.jpa.persistence.dao.manager;

import io.github.neopen.jpa.persistence.dao.AbstractBaseDao;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * <b> jpa 占位符传参
 * <p>
 *
 * @author He.hp
 * @version 1.0
 * @date 2020年7月30日 上午10:17:48
 */
@SuppressWarnings("all")
@Repository("dao")
public class EntityManagerSqlDao extends AbstractBaseDao {

    @PersistenceContext
    private EntityManager em;

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#find(java.lang.String, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <T> T find(String sqlStr, Class<T> resultClass, Object... params) throws Exception {
        Query query = getNativeQuery(sqlStr, resultClass, params);
        if (query == null)
            return null;

        return (T) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#find(java.lang.String, java.lang.Class, java.util.Map)
     */
    @Override
    public <T> T find(String sqlStr, Class<T> resultClass, Map<String, Object> params) throws Exception {
        Query query = getNativeQuery(sqlStr, resultClass, params);
        if (query == null)
            return null;

        return (T) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#findAll(java.lang.String, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <T> List<T> findAll(String sqlStr, Class<T> resultClass, Object... params) throws Exception {
        Query query = getNativeQuery(sqlStr, resultClass, params);
        if (query == null)
            return null;

        return query.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#findAll(java.lang.String, java.lang.Class, java.util.Map)
     */
    @Override
    public <T> List<T> findAll(String sqlStr, Class<T> resultClass, Map<String, Object> params) throws Exception {
        Query query = getNativeQuery(sqlStr, resultClass, params);
        if (query == null)
            return null;

        return query.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#findPage(java.lang.String, org.springframework.data.domain.PageRequest, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <T> List<T> findPage(String sqlStr, PageRequest page, Class<T> resultClass, Object... params) throws Exception {
        Query query = getNativeQuery(sqlStr, resultClass, params);
        if (query == null)
            return null;

        return query.setFirstResult((int) page.getOffset()).setMaxResults((int) page.getPageSize()).getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#findPage(java.lang.String, org.springframework.data.domain.PageRequest, java.lang.Class, java.util.Map)
     */
    @Override
    public <T> List<T> findPage(String sqlStr, PageRequest page, Class<T> resultClass, Map<String, Object> params) throws Exception {
        Query query = getNativeQuery(sqlStr, resultClass, params);
        if (query == null)
            return null;

        return query.setFirstResult((int) page.getOffset()).setMaxResults((int) page.getPageSize()).getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#count(java.lang.String, java.lang.Object[])
     */
    @Override
    public Long count(String sqlStr, Object... params) throws Exception {
        Query query = getNativeQuery(sqlStr, Long.class, params);
        if (query == null)
            return null;

        BigInteger singleResult = (BigInteger) query.getSingleResult();

        return singleResult == null ? 0 : singleResult.longValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#count(java.lang.String, java.util.Map)
     */
    @Override
    public Long count(String sqlStr, Map<String, Object> params) throws Exception {
        Query query = getNativeQuery(sqlStr, Long.class, params);
        if (query == null)
            return null;

        BigInteger singleResult = (BigInteger) query.getSingleResult();

        return singleResult == null ? 0 : singleResult.longValue();
    }

    /**
     * <b> 获得查询
     * <p>
     *
     * @param <T>
     * @param hql
     * @param resultClass
     * @param params
     * @return
     * @author He.hp
     * @date 2020年7月30日 下午2:05:52
     */
    private <T> Query getNativeQuery(String sqlStr, Class<T> resultClass, Object... params) {
        if (sqlStr == null || sqlStr.isEmpty())
            return null;

        Query query = resultClass == null ? em.createNativeQuery(sqlStr) : em.createNativeQuery(sqlStr, resultClass);

        setQueryParameter(query, params);
        params = null;

        return query;
    }

    /**
     * <b> 获得查询
     * <P> 
     * 
     * @param sqlStr 
     * @param resultClass 
     * @param params
     * @Author He.hp  
     * @Email hehp@citycloud.com.cn
     * @Date 2021/4/15 16:46
     * @return javax.persistence.Query
     */
    private <T> Query getNativeQuery(String sqlStr, Class<T> resultClass, Map<String, Object> params) {
        if (sqlStr == null || sqlStr.isEmpty())
            return null;

        Query query = resultClass == null ? em.createNativeQuery(sqlStr) : em.createNativeQuery(sqlStr, resultClass);

        setQueryParameter(query, params);
        params = null;

        return query;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#insertAll(java.util.List)
     */
    @Override
    public <T> int insertAll(List<T> params) throws Exception {
        if (params == null || params.isEmpty())
            return 0;

        int result = 0;
        for (T t : params) {

            result += em.createNativeQuery((String) t)

                    .executeUpdate();
        }
        return result;
    }

}
