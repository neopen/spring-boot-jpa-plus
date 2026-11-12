package io.github.neopen.core.persistence.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * JPA Criteria 运算符策略。
 *
 * <p>实现类负责把一个字段路径和参数转换为 Criteria 谓词，
 * 便于扩展自定义运算符。</p>
 */
public interface SpecOperator {

    /**
     * 获取运算符需要的参数数量。
     *
     * @return 参数数量
     */
    int parameterCount();

    /**
     * 构建查询谓词。
     *
     * @param cb      Criteria 构建器
     * @param path    字段路径
     * @param values  运算符参数
     * @return 查询谓词
     */
    Predicate toPredicate(
            CriteriaBuilder cb,
            Path<?> path,
            Object... values
    );
}
