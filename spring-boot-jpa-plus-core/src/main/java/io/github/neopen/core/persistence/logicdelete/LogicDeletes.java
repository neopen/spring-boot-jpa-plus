package io.github.neopen.core.persistence.logicdelete;

import org.springframework.data.jpa.domain.Specification;

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
}
