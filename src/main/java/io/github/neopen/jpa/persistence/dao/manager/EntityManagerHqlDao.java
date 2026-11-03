package io.github.neopen.jpa.persistence.dao.manager;

import io.github.neopen.jpa.persistence.dao.AbstractBaseDao;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Map;

/**
 * <b> jpa hql语句 占位符传参
 * <p>
 *
 * @author He.hp
 * @version 1.0
 * @date 2020年7月30日 上午10:17:48
 */
@SuppressWarnings("all")
@Repository("hqlDao")
public class EntityManagerHqlDao extends AbstractBaseDao {

    @PersistenceContext
    private EntityManager em;

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.jpa.JpaDao#find(java.lang.String, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <T> T find(String hql, Class<T> resultClass, Object... params) throws Exception {
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        if (query == null)
            return null;

        return (T) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.jpa.JpaDao#findAll(java.lang.String, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <T> List<T> findAll(String hql, Class<T> resultClass, Object... params) throws Exception {
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        if (query == null)
            return null;

        return query.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.jpa.JpaDao#findPage(java.lang.String, org.springframework.data.domain.PageRequest, java.lang.Class, java.lang.Object[])
     */
    @Override
    public <T> List<T> findPage(String hql, PageRequest page, Class<T> resultClass, Object... params) throws Exception {
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        if (query == null)
            return null;

        return query.setFirstResult((int) page.getOffset()).setMaxResults((int) page.getPageSize()).getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.jpa.JpaDao#count(java.lang.String, java.lang.Object[])
     */
    @Override
    public Long count(String hql, Object... params) throws Exception {
        TypedQuery<Long> query = getQuery(hql, Long.class, params);
        if (query == null)
            return null;

        return query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#find(java.lang.String, java.lang.Class, java.util.Map)
     */
    @Override
    public <T> T find(String hql, Class<T> resultClass, Map<String, Object> params) throws Exception {
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        if (query == null)
            return null;

        return (T) query.getSingleResult();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#findAll(java.lang.String, java.lang.Class, java.util.Map)
     */
    @Override
    public <T> List<T> findAll(String hql, Class<T> resultClass, Map<String, Object> params) throws Exception {
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        if (query == null)
            return null;

        return query.getResultList();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#findPage(java.lang.String, org.springframework.data.domain.PageRequest, java.lang.Class, java.util.Map)
     */
    @Override
    public <T> List<T> findPage(String hql, PageRequest page, Class<T> resultClass, Map<String, Object> params) throws Exception {
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        if (query == null)
            return null;

        return query.setFirstResult((int) page.getOffset()).setMaxResults((int) page.getPageSize()).getResultList();

    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#count(java.lang.String, java.util.Map)
     */
    @Override
    public Long count(String hql, Map<String, Object> params) throws Exception {
        TypedQuery<Long> query = getQuery(hql, Long.class, params);
        if (query == null)
            return null;

        return query.getSingleResult();
    }

    /**
     * <b>
     * <p>
     *
     * @param <T>
     * @param hql
     * @param resultClass
     * @return
     * @author He.hp
     * @date 2020年7月30日 上午11:08:04
     */
    private <T> TypedQuery<T> getQuery(String hql, Class<T> resultClass, Object... params) {
        if (hql == null || hql.isEmpty())
            return null;

        TypedQuery<T> query = em.createQuery(hql, resultClass);

        setQueryParameter(query, params);

        return query;
    }

    private <T> Query getQuery(String hql, Object... params) {
        if (hql == null || hql.isEmpty())
            return null;

        Query query = em.createQuery(hql);

        setQueryParameter(query, params);

        return query;
    }

    /**
     * <b> 获得查询
     * <p>
     *
     * @param <T>
     * @param hql
     * @param resultClass
     * @return
     * @author He.hp
     * @date 2020年7月30日 上午11:08:04
     */
    private <T> TypedQuery<T> getQuery(String hql, Class<T> resultClass, Map<String, Object> params) {
        if (hql == null || hql.isEmpty())
            return null;

        TypedQuery<T> query = em.createQuery(hql, resultClass);

        setQueryParameter(query, params);
        params = null;

        return query;
    }

    /**
     * <b> 获得查询
     * <P>
     *
     * @param hql
     * @param params
     * @Author He.hp
     * @Email hehp@citycloud.com.cn
     * @Date 2021/4/15 16:47
     * @return javax.persistence.Query
     */
    private <T> Query getQuery(String hql, Map<String, Object> params) {
        if (hql == null || hql.isEmpty())
            return null;

        Query query = em.createQuery(hql);
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

        int j = 0;

        for (T t : params) {
            em.persist(t);
            j++;
            if (j % 50 == 0 || j == params.size()) {
                try {
                    em.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    em.clear();
                }
            }
        }

        return j;
    }

}
