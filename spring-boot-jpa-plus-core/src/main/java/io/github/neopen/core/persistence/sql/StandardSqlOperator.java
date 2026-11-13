package io.github.neopen.core.persistence.sql;

import io.github.neopen.core.persistence.condition.ConditionOperator;

import java.util.List;

/**
 * 内置 SQL 运算符。
 *
 * <p>使用枚举实现运算符渲染策略，新增内置运算符只需扩展枚举；
 * 业务自定义运算符实现 {@link SqlOperator} 即可。</p>
 */
public enum StandardSqlOperator implements SqlOperator {

    /** 等于。 */
    EQ(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " = :" + names.get(0);
        }
    },

    /** 不等于。 */
    NE(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " <> :" + names.get(0);
        }
    },

    /** 大于。 */
    GT(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " > :" + names.get(0);
        }
    },

    /** 大于或等于。 */
    GE(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " >= :" + names.get(0);
        }
    },

    /** 小于。 */
    LT(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " < :" + names.get(0);
        }
    },

    /** 小于或等于。 */
    LE(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " <= :" + names.get(0);
        }
    },

    /** 模糊匹配。 */
    LIKE(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " like :" + names.get(0);
        }
    },

    /** 不匹配。 */
    NOT_LIKE(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " not like :" + names.get(0);
        }
    },

    /** 属于集合。 */
    IN(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " in (:" + names.get(0) + ")";
        }
    },

    /** 不属于集合。 */
    NOT_IN(1) {
        @Override
        public String render(String path, List<String> names) {
            return path + " not in (:" + names.get(0) + ")";
        }
    },

    /** 闭区间。 */
    BETWEEN(2) {
        @Override
        public String render(String path, List<String> names) {
            return path + " between :" + names.get(0)
                    + " and :" + names.get(1);
        }
    },

    /** 为空。 */
    IS_NULL(0) {
        @Override
        public String render(String path, List<String> names) {
            return path + " is null";
        }
    },

    /** 不为空。 */
    IS_NOT_NULL(0) {
        @Override
        public String render(String path, List<String> names) {
            return path + " is not null";
        }
    };

    /** 运算符需要的参数数量。 */
    private final int parameterCount;

    /**
     * 创建运算符。
     *
     * @param parameterCount 参数数量
     */
    StandardSqlOperator(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    @Override
    public int parameterCount() {
        return parameterCount;
    }

    /**
     * 将通用条件运算符转换为 SQL 运算符。
     *
     * @param operator 通用条件运算符
     * @return SQL 运算符
     */
    public static StandardSqlOperator of(ConditionOperator operator) {
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
