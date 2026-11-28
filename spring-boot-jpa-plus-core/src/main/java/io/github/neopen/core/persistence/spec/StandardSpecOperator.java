/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.persistence.spec;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.Collection;

/**
 * 内置 JPA Criteria 运算符。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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

    /**
     * 将通用条件运算符转换为 Criteria 运算符。
     *
     * @param operator 通用条件运算符
     * @return Criteria 运算符
     */
    public static StandardSpecOperator of(ConditionOperator operator) {
        return switch (operator) {
            case EQ -> EQ;
            case NE -> NE;
            case GT -> GT;
            case GE -> GE;
            case LT -> LT;
            case LE -> LE;
            case LIKE, LIKE_LEFT, LIKE_RIGHT -> LIKE;
            case NOT_LIKE -> NOT_LIKE;
            case IN -> IN;
            case NOT_IN -> NOT_IN;
            case BETWEEN -> BETWEEN;
            case IS_NULL -> IS_NULL;
            case IS_NOT_NULL -> IS_NOT_NULL;
        };
    }
}
