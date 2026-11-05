package io.github.neopen.core.persistence.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.ManagedType;

import java.util.Collection;
import java.util.List;

/**
 * JPA Criteria 动态查询构建器。
 *
 * <p>将 {@link QueryWrapper} 中的条件转换为使用逻辑与组合的谓词。</p>
 *
 * @param <T> 实体类型
 */
public class JpaPlusQueryBuilder<T> {

    /** 用于读取实体元数据的实体管理器。 */
    private final EntityManager entityManager;

    /** 查询目标实体类型。 */
    private final Class<T> entityClass;

    /**
     * 创建动态查询构建器。
     *
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entityClass   查询目标实体类型，不能为 {@code null}
     */
    public JpaPlusQueryBuilder(
            EntityManager entityManager,
            Class<T> entityClass
    ) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    /**
     * 构建完整查询谓词。
     *
     * @param root    查询根路径
     * @param cb      Criteria 构建器
     * @param wrapper 动态查询条件；为 {@code null} 或空时匹配全部
     * @return 所有条件通过逻辑与组合后的谓词
     * @throws IllegalArgumentException 字段或条件操作数无效
     */
    public Predicate buildPredicate(
            Root<T> root,
            CriteriaBuilder cb,
            QueryWrapper<T> wrapper
    ) {
        if (wrapper == null || wrapper.isEmpty()) {
            return cb.conjunction();
        }

        List<QueryCondition> conditions =
                wrapper.getConditions();

        Predicate[] predicates = conditions.stream()
                .map(condition ->
                        buildCondition(root, cb, condition)
                )
                .toArray(Predicate[]::new);

        return cb.and(predicates);
    }

    /**
     * 构建单个查询条件对应的谓词。
     *
     * @param root      查询根路径
     * @param cb        Criteria 构建器
     * @param condition 单个动态查询条件
     * @return 条件对应的谓词
     */
    private Predicate buildCondition(
            Root<T> root,
            CriteriaBuilder cb,
            QueryCondition condition
    ) {
        validateField(condition.getField());

        Path<Object> path = root.get(condition.getField());

        Object value = condition.getValue();

        return switch (condition.getOperator()) {

            case EQ -> value == null
                    ? cb.isNull(path)
                    : cb.equal(path, value);

            case NE -> value == null
                    ? cb.isNotNull(path)
                    : cb.notEqual(path, value);

            case GT -> cb.greaterThan(
                    comparable(path),
                    comparableValue(value)
            );

            case GE -> cb.greaterThanOrEqualTo(
                    comparable(path),
                    comparableValue(value)
            );

            case LT -> cb.lessThan(
                    comparable(path),
                    comparableValue(value)
            );

            case LE -> cb.lessThanOrEqualTo(
                    comparable(path),
                    comparableValue(value)
            );

            case LIKE -> cb.like(
                    path.as(String.class),
                    "%" + escapeLike(value) + "%",
                    '\\'
            );

            case LIKE_LEFT -> cb.like(
                    path.as(String.class),
                    "%" + escapeLike(value),
                    '\\'
            );

            case LIKE_RIGHT -> cb.like(
                    path.as(String.class),
                    escapeLike(value) + "%",
                    '\\'
            );

            case IN -> buildIn(path, cb, value, false);

            case NOT_IN -> buildIn(path, cb, value, true);

            case BETWEEN -> cb.between(
                    comparable(path),
                    comparableValue(value),
                    comparableValue(condition.getSecondValue())
            );

            case IS_NULL -> cb.isNull(path);

            case IS_NOT_NULL -> cb.isNotNull(path);
        };
    }

    /**
     * 构建 {@code IN} 或 {@code NOT IN} 谓词。
     *
     * <p>空集合在 {@code IN} 中恒为假，在 {@code NOT IN} 中恒为真。</p>
     *
     * @param path  字段路径
     * @param cb    Criteria 构建器
     * @param value 条件值，必须是集合
     * @param not   是否构建 {@code NOT IN}
     * @return 集合条件谓词
     * @throws IllegalArgumentException 条件值不是集合
     */
    private Predicate buildIn(
            Path<Object> path,
            CriteriaBuilder cb,
            Object value,
            boolean not
    ) {
        if (!(value instanceof Collection<?> values)) {
            throw new IllegalArgumentException(
                    "IN operator requires a Collection"
            );
        }

        if (values.isEmpty()) {
            return not ? cb.conjunction() : cb.disjunction();
        }

        CriteriaBuilder.In<Object> in = cb.in(path);

        values.forEach(in::value);

        return not ? cb.not(in) : in;
    }

    /**
     * 校验字段是否属于当前实体。
     *
     * @param field 待校验的实体字段名
     * @throws IllegalArgumentException 字段不属于当前实体
     */
    private void validateField(String field) {
        try {
            ManagedType<T> managedType =
                    entityManager.getMetamodel()
                            .managedType(entityClass);

            managedType.getAttribute(field);

        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid entity field: "
                            + entityClass.getSimpleName()
                            + "."
                            + field,
                    ex
            );
        }
    }

    /**
     * 将字段路径转换为可比较表达式。
     *
     * @param path 字段路径
     * @return 可比较表达式
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Comparable> comparable(
            Path<?> path
    ) {
        return (Expression<? extends Comparable>) path;
    }

    /**
     * 校验并返回可比较的条件值。
     *
     * @param value 原始条件值
     * @return 可比较的条件值
     * @throws IllegalArgumentException 条件值未实现 {@link Comparable}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Comparable comparableValue(Object value) {
        if (!(value instanceof Comparable comparable)) {
            throw new IllegalArgumentException(
                    "Query value must implement Comparable"
            );
        }

        return comparable;
    }

    /**
     * 转义 {@code LIKE} 条件中的特殊字符。
     *
     * @param value 原始匹配值，不能为 {@code null}
     * @return 转义反斜杠、百分号和下划线后的匹配值
     * @throws IllegalArgumentException 匹配值为 {@code null}
     */
    private String escapeLike(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "LIKE value must not be null"
            );
        }

        return value.toString()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
