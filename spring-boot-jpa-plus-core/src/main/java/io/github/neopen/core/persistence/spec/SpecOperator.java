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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * JPA Criteria 运算符策略。
 *
 * <p>实现类负责把一个字段路径和参数转换为 Criteria 谓词，
 * 便于扩展自定义运算符。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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
