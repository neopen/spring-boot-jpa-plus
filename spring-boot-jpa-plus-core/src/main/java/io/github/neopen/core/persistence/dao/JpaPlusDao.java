package io.github.neopen.core.persistence.dao;

import io.github.neopen.core.persistence.query.QueryWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA Plus 通用数据访问接口。
 *
 * <p>提供通用增删改查、动态条件查询和分页查询能力。</p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface JpaPlusDao<T, ID extends Serializable> {

    /**
     * 根据主键查询实体。
     *
     * @param id 实体主键
     * @return 包含实体的可选值；未找到时返回空可选值
     */
    Optional<T> findById(ID id);

    /**
     * 查询全部实体。
     *
     * @return 实体列表，不会为 {@code null}
     */
    List<T> findAll();

    /**
     * 保存实体。
     *
     * @param entity 待保存实体，不能为 {@code null}
     * @return 持久化上下文管理的实体
     */
    T save(T entity);

    /**
     * 批量保存实体。
     *
     * @param entities 待保存实体集合
     * @return 保存后的实体列表
     */
    List<T> saveAll(Collection<T> entities);

    /**
     * 根据主键删除实体。
     *
     * @param id 实体主键；为 {@code null} 时不执行操作
     */
    void deleteById(ID id);

    /**
     * 删除实体。
     *
     * @param entity 待删除实体；为 {@code null} 时不执行操作
     */
    void delete(T entity);

    /**
     * 批量删除实体。
     *
     * @param entities 待删除实体集合，空集合不执行操作
     */
    void deleteAll(Collection<T> entities);

    /**
     * 判断指定主键的实体是否存在。
     *
     * @param id 实体主键
     * @return 实体存在时返回 {@code true}
     */
    boolean existsById(ID id);

    /**
     * 统计实体总数量。
     *
     * @return 实体总数量
     */
    long count();

    /**
     * 无附加条件分页查询实体。
     *
     * @param pageable 分页及排序参数，不能为 {@code null}
     * @return 分页查询结果
     */
    Page<T> findPage(Pageable pageable);

    /**
     * 根据动态条件查询实体列表。
     *
     * @param wrapper 动态查询条件；为 {@code null} 时查询全部
     * @return 符合条件的实体列表
     */
    List<T> findList(QueryWrapper<T> wrapper);

    /**
     * 根据动态条件分页查询实体。
     *
     * @param wrapper  动态查询条件；为 {@code null} 时查询全部
     * @param pageable 分页及排序参数，不能为 {@code null}
     * @return 分页查询结果
     */
    Page<T> findPage(
            QueryWrapper<T> wrapper,
            Pageable pageable
    );
}
