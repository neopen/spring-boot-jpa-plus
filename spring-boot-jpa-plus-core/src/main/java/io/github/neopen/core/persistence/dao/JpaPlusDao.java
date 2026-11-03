package io.github.neopen.core.persistence.dao;

import io.github.neopen.core.persistence.query.QueryWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA Plus 通用 DAO
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface JpaPlusDao<T, ID extends Serializable> {

    /**
     * 根据主键查询
     */
    Optional<T> findById(ID id);

    /**
     * 查询全部
     */
    List<T> findAll();

    /**
     * 保存实体
     */
    T save(T entity);

    /**
     * 批量保存
     */
    List<T> saveAll(Collection<T> entities);

    /**
     * 根据主键删除
     */
    void deleteById(ID id);

    /**
     * 删除实体
     */
    void delete(T entity);

    /**
     * 批量删除
     */
    void deleteAll(Collection<T> entities);

    /**
     * 判断主键是否存在
     */
    boolean existsById(ID id);

    /**
     * 统计数量
     */
    long count();

    /**
     * 分页查询
     */
    Page<T> findPage(Pageable pageable);

    /**
     * 根据动态条件查询
     */
    List<T> findList(QueryWrapper<T> wrapper);

    /**
     * 根据动态条件分页查询
     */
    Page<T> findPage(
            QueryWrapper<T> wrapper,
            Pageable pageable
    );
}
