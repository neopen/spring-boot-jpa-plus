package io.github.neopen.jpa.persistence.dao.manager;

import io.github.neopen.jpa.persistence.dao.AbstractBaseDao;
import org.springframework.data.domain.PageRequest;

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
 * @author neopen
 * @version 1.0
 * @date 2020年7月30日 上午10:17:48
 */
@SuppressWarnings("all")
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
        if (page == null) throw new IllegalArgumentException("page must not be null");
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        return query.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults((int) page.getPageSize()).getResultList();
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
        if (page == null) throw new IllegalArgumentException("page must not be null");
        Query query = resultClass == null ? getQuery(hql, params) : getQuery(hql, resultClass, params);
        return query.setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults((int) page.getPageSize()).getResultList();

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
     * @author neopen
     * @date 2020年7月30日 上午11:08:04
     */
    private <T> TypedQuery<T> getQuery(String hql, Class<T> resultClass, Object... params) {
        if (hql == null || hql.trim().isEmpty())
            throw new IllegalArgumentException("HQL must not be blank");

        TypedQuery<T> query = em.createQuery(hql, resultClass);

        setQueryParameter(query, params);

        return query;
    }

    private <T> Query getQuery(String hql, Object... params) {
        if (hql == null || hql.trim().isEmpty())
            throw new IllegalArgumentException("HQL must not be blank");

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
     * @author neopen
     * @date 2020年7月30日 上午11:08:04
     */
    private <T> TypedQuery<T> getQuery(String hql, Class<T> resultClass, Map<String, Object> params) {
        if (hql == null || hql.trim().isEmpty())
            throw new IllegalArgumentException("HQL must not be blank");

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
     * @author neopen
     * @Email hehp@citycloud.com.cn
     * @Date 2021/4/15 16:47
     * @return javax.persistence.Query
     */
    private <T> Query getQuery(String hql, Map<String, Object> params) {
        if (hql == null || hql.trim().isEmpty())
            throw new IllegalArgumentException("HQL must not be blank");

        Query query = em.createQuery(hql);
        setQueryParameter(query, params);
        params = null;

        return query;
    }

    @Override
    public int executeUpdate(String hql, Object... params) {
        if (hql == null || hql.trim().isEmpty()) {
            throw new IllegalArgumentException("HQL must not be blank");
        }
        Query query = em.createQuery(hql);
        setQueryParameter(query, params);
        return query.executeUpdate();
    }

    @Override
    public int executeUpdate(String hql, Map<String, Object> params) {
        if (hql == null || hql.trim().isEmpty()) {
            throw new IllegalArgumentException("HQL must not be blank");
        }
        Query query = em.createQuery(hql);
        setQueryParameter(query, params);
        return query.executeUpdate();
    }

    /**
     * Persists entities in chunks and flushes after each chunk.
     * The caller owns the transaction and persistence-context lifecycle.
     */
    public <T> int persistAll(java.util.Collection<T> entities) {
        return persistAll(entities, 50);
    }

    public <T> int persistAll(java.util.Collection<T> entities, int batchSize) {
        if (entities == null || entities.isEmpty()) {
            return 0;
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }

        int persisted = 0;
        for (T entity : entities) {
            em.persist(entity);
            persisted++;
            if (persisted % batchSize == 0 || persisted == entities.size()) {
                em.flush();
            }
        }
        return persisted;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.citycloud.sanitation.common.dao.HibernateDao#insertAll(java.util.List)
     */
    @Override
    @Deprecated
    public <T> int insertAll(List<T> params) {
        return persistAll(params, 50);
    }

}
