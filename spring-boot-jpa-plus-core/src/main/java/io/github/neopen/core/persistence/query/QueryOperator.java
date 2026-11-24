/*
 * Copyright 2026 neopen
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
package io.github.neopen.core.persistence.query;

/**
 * 动态查询操作符。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public enum QueryOperator {

    /** 等于；操作数为 {@code null} 时匹配空值。 */
    EQ,

    /** 不等于；操作数为 {@code null} 时匹配非空值。 */
    NE,

    /** 大于。 */
    GT,

    /** 大于或等于。 */
    GE,

    /** 小于。 */
    LT,

    /** 小于或等于。 */
    LE,

    /** 包含指定文本，对应两侧模糊匹配。 */
    LIKE,

    /** 以指定文本结尾，对应左侧模糊匹配。 */
    LIKE_LEFT,

    /** 以指定文本开头，对应右侧模糊匹配。 */
    LIKE_RIGHT,

    /** 字段值属于指定集合。 */
    IN,

    /** 字段值不属于指定集合。 */
    NOT_IN,

    /** 字段值位于指定的闭区间内。 */
    BETWEEN,

    /** 字段值为 {@code null}。 */
    IS_NULL,

    /** 字段值不为 {@code null}。 */
    IS_NOT_NULL
}
