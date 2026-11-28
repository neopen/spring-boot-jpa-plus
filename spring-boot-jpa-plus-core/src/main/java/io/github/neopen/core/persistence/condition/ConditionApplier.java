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
package io.github.neopen.core.persistence.condition;

/**
 * 条件应用回调。
 *
 * <p>由查询构造器实现，把参数注解解析出的条件写入具体查询。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface ConditionApplier {

    /**
     * 应用一个查询条件。
     *
     * @param field    字段名或列名
     * @param operator 条件运算符
     * @param value    条件值；无参运算符为 {@code null}
     * @param second   区间结束值；仅 {@link ConditionOperator#BETWEEN} 使用
     */
    void apply(
            String field,
            ConditionOperator operator,
            Object value,
            Object second
    );
}
