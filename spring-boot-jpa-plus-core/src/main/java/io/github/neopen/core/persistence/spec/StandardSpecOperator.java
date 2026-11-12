package io.github.neopen.core.persistence.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Collection;

/**
 * 内置 JPA Criteria 运算符。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public enum StandardSpecOperator implements SpecOperator {

    /** 等于。 */
    EQ(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            Object value = values[0];

            return value == null
                    ? path.isNull()
                    : cb.equal(path, value);
        }
    },

    /** 不等于。 */
    NE(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            Object value = values[0];

            return value == null
                    ? path.isNotNull()
                    : cb.notEqual(path, value);
        }
    },

    /** 大于。 */
    GT(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.greaterThan(
                    (Expression) path,
                    (Comparable) values[0]
            );
        }
    },

    /** 大于或等于。 */
    GE(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.greaterThanOrEqualTo(
                    (Expression) path,
                    (Comparable) values[0]
            );
        }
    },

    /** 小于。 */
    LT(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.lessThan(
                    (Expression) path,
                    (Comparable) values[0]
            );
        }
    },

    /** 小于或等于。 */
    LE(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.lessThanOrEqualTo(
                    (Expression) path,
                    (Comparable) values[0]
            );
        }
    },

    /** 模糊匹配。 */
    LIKE(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.like(
                    (Expression<String>) path,
                    (String) values[0]
            );
        }
    },

    /** 不匹配。 */
    NOT_LIKE(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.notLike(
                    (Expression<String>) path,
                    (String) values[0]
            );
        }
    },

    /** 集合包含。 */
    IN(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return path.in((Collection<?>) values[0]);
        }
    },

    /** 集合不包含。 */
    NOT_IN(1) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.not(path.in((Collection<?>) values[0]));
        }
    },

    /** 闭区间。 */
    BETWEEN(2) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return cb.between(
                    (Expression) path,
                    (Comparable) values[0],
                    (Comparable) values[1]
            );
        }
    },

    /** 为空。 */
    IS_NULL(0) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return path.isNull();
        }
    },

    /** 不为空。 */
    IS_NOT_NULL(0) {
        @Override
        public Predicate toPredicate(
                CriteriaBuilder cb,
                Path<?> path,
                Object... values
        ) {
            return path.isNotNull();
        }
    };

    /** 运算符参数数量。 */
    private final int parameterCount;

    /**
     * 创建内置运算符。
     *
     * @param parameterCount 参数数量
     */
    StandardSpecOperator(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    /** {@inheritDoc} */
    @Override
    public int parameterCount() {
        return parameterCount;
    }
}
