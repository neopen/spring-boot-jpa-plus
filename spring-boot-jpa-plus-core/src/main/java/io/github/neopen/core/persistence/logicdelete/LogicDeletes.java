package io.github.neopen.core.persistence.logicdelete;

import io.github.neopen.core.exception.JpaPlusException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;

/**
 * 逻辑删除过滤工具。
 *
 * <p>为标注了 {@link LogicDelete} 的实体生成 {@code 字段 = false}
 * 查询条件。未标注的实体不会追加任何限制。</p>
 *
 * <pre>{@code
 * Specification<Order> spec = LogicDeletes.filter(
 *         Order.class,
 *         SpecWrapper.create(Order.class).eq("status", "PAID")
 * );
 * }</pre>
 *
 * <p>注意：过滤只作用于 JPA Criteria 查询，原生 SQL 需要自行拼接
 * 逻辑删除条件。</p>
 */
public final class LogicDeletes {

    /** 工具类不允许实例化。 */
    private LogicDeletes() {
    }

    /**
     * 判断实体是否声明了逻辑删除字段。
     *
     * @param entityType 实体类型
     * @return 是否声明逻辑删除字段
     * @throws IllegalArgumentException 实体类型为空或字段类型非法时抛出
     */
    public static boolean isLogicDeleted(Class<?> entityType) {
        return LogicDeleteMetadata.find(requireType(entityType))
                .isPresent();
    }

    /**
     * 创建逻辑删除过滤条件。
     *
     * <p>实体未标注 {@link LogicDelete} 时返回恒成立条件。</p>
     *
     * @param entityType 实体类型
     * @param <T>        实体类型
     * @return 查询规范
     * @throws IllegalArgumentException 实体类型为空或字段类型非法时抛出
     */
    public static <T> Specification<T> filter(Class<T> entityType) {
        return LogicDeleteMetadata.find(requireType(entityType))
                .<Specification<T>>map(metadata -> (root, query, cb) ->
                        cb.equal(root.get(metadata.fieldName()), false))
                .orElseGet(() -> (root, query, cb) -> cb.conjunction());
    }

    /**
     * 把逻辑删除过滤条件与业务条件合并。
     *
     * <p>实体未标注 {@link LogicDelete} 时原样返回业务条件。</p>
     *
     * @param entityType    实体类型
     * @param specification 业务条件，允许为 {@code null}
     * @param <T>           实体类型
     * @return 合并后的查询规范
     * @throws IllegalArgumentException 实体类型为空或字段类型非法时抛出
     */
    public static <T> Specification<T> filter(
            Class<T> entityType,
            Specification<T> specification
    ) {
        requireType(entityType);

        if (!isLogicDeleted(entityType)) {
            return specification;
        }

        Specification<T> logicDelete = filter(entityType);

        return specification == null
                ? logicDelete
                : logicDelete.and(specification);
    }

    /**
     * 按主键执行逻辑删除。
     *
     * <p>直接执行 JPQL 更新语句，将逻辑删除字段置为 {@code true}，
     * 不触发实体生命周期回调。</p>
     *
     * @param entityManager 实体管理器
     * @param entityType    实体类型，必须标注 {@link LogicDelete}
     * @param id            主键值
     * @param <T>           实体类型
     * @return 受影响的记录数
     * @throws IllegalArgumentException 参数为空或实体未标注注解时抛出
     */
    public static <T> int deleteById(
            EntityManager entityManager,
            Class<T> entityType,
            Object id
    ) {
        if (id == null) {
            return 0;
        }

        return deleteAllById(entityManager, entityType, List.of(id));
    }

    /**
     * 按主键批量执行逻辑删除。
     *
     * @param entityManager 实体管理器
     * @param entityType    实体类型，必须标注 {@link LogicDelete}
     * @param ids           主键集合；为空时返回 0
     * @param <T>           实体类型
     * @return 受影响的记录数
     * @throws IllegalArgumentException 参数为空或实体未标注注解时抛出
     */
    public static <T> int deleteAllById(
            EntityManager entityManager,
            Class<T> entityType,
            Collection<?> ids
    ) {
        LogicDeleteMetadata metadata = requireMetadata(
                entityManager,
                entityType
        );

        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String jpql = "update " + entityName(entityManager, entityType)
                + " e set e." + metadata.fieldName() + " = true"
                + " where e." + idAttributeName(entityManager, entityType)
                + " in :ids";

        Query query = entityManager.createQuery(jpql);
        query.setParameter("ids", ids);

        return query.executeUpdate();
    }

    /**
     * 将实体标记为已删除。
     *
     * <p>要求实体处于托管状态，字段变更由持久化上下文在事务提交时
     * 自动同步。</p>
     *
     * @param entityManager 实体管理器
     * @param entityType    实体类型，必须标注 {@link LogicDelete}
     * @param entities      待删除实体；为空时返回 0
     * @param <T>           实体类型
     * @return 标记的实体数量
     * @throws IllegalArgumentException 参数为空或实体未标注注解时抛出
     * @throws JpaPlusException         字段写入失败时抛出
     */
    public static <T> int deleteAll(
            EntityManager entityManager,
            Class<T> entityType,
            Collection<T> entities
    ) {
        LogicDeleteMetadata metadata = requireMetadata(
                entityManager,
                entityType
        );

        if (entities == null || entities.isEmpty()) {
            return 0;
        }

        int count = 0;

        for (T entity : entities) {
            if (entity == null) {
                continue;
            }

            try {
                metadata.field().set(entity, Boolean.TRUE);
            } catch (IllegalAccessException e) {
                throw new JpaPlusException(
                        "Cannot mark entity as deleted",
                        e
                );
            }

            count++;
        }

        return count;
    }

    /**
     * 校验实体类型。
     *
     * @param entityType 实体类型
     * @param <T>        实体类型
     * @return 实体类型
     */
    private static <T> Class<T> requireType(Class<T> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        return entityType;
    }

    /**
     * 要求实体声明逻辑删除字段。
     *
     * @param entityManager 实体管理器
     * @param entityType    实体类型
     * @return 逻辑删除元数据
     * @throws IllegalArgumentException 实体管理器为空或实体未标注注解时抛出
     */
    private static LogicDeleteMetadata requireMetadata(
            EntityManager entityManager,
            Class<?> entityType
    ) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "Entity manager must not be null"
            );
        }

        requireType(entityType);

        return LogicDeleteMetadata.find(entityType).orElseThrow(
                () -> new IllegalArgumentException(
                        "@LogicDelete is required for logical deletion: "
                                + entityType.getName()
                )
        );
    }

    /**
     * 获取实体名称。
     *
     * @param entityManager 实体管理器
     * @param entityType    实体类型
     * @return 实体名称
     */
    private static String entityName(
            EntityManager entityManager,
            Class<?> entityType
    ) {
        return entityManager.getMetamodel()
                .entity(entityType)
                .getName();
    }

    /**
     * 解析实体主键属性名。
     *
     * @param entityManager 实体管理器
     * @param entityType    实体类型
     * @return 主键属性名
     * @throws IllegalArgumentException 无法解析主键时抛出
     */
    private static String idAttributeName(
            EntityManager entityManager,
            Class<?> entityType
    ) {
        EntityType<?> type = entityManager.getMetamodel()
                .entity(entityType);

        for (SingularAttribute<?, ?> attribute
                : type.getSingularAttributes()) {
            if (attribute.isId()) {
                return attribute.getName();
            }
        }

        throw new IllegalArgumentException(
                "Cannot resolve id attribute for " + entityType.getName()
        );
    }
}
