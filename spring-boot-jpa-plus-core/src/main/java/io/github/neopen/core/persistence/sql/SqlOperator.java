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
package io.github.neopen.core.persistence.sql;

import java.util.List;

/**
 * SQL 运算符策略。
 *
 * <p>运算符负责声明参数数量，并将列、参数名渲染为 SQL 条件片段。
 * 业务可以自定义实现以扩展数据库特有语法。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public interface SqlOperator {

    /**
     * 获取运算符需要的参数数量。
     *
     * @return 参数数量
     */
    int parameterCount();

    /**
     * 渲染 SQL 条件片段。
     *
     * @param path           带表别名的列路径
     * @param parameterNames 已分配的命名参数
     * @return SQL 条件片段
     */
    String render(String path, List<String> parameterNames);
}
