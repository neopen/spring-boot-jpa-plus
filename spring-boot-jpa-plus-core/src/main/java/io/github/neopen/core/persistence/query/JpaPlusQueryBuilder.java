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
 * JPA Criteria 动态查询构建器
 *
 * @param <T> 实体类型
 */
public class JpaPlusQueryBuilder<T> {

    private final EntityManager entityManager;

    private final Class<T> entityClass;

    public JpaPlusQueryBuilder(
            EntityManager entityManager,
            Class<T> entityClass
    ) {
        this.entityManager = entityManager;
        this.entityClass = entityClass;
    }

    /**
     * 构建查询条件
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
     * 构建单个条件
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
     * 构建 IN / NOT IN
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
     * 校验字段是否属于当前实体
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Expression<? extends Comparable> comparable(
            Path<?> path
    ) {
        return (Expression<? extends Comparable>) path;
    }

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
     * 转义 LIKE 中的特殊字符
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
