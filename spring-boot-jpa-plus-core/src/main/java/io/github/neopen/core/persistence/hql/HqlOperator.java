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
package io.github.neopen.core.persistence.hql;

import java.util.List;

/**
 * HQL 条件运算符。
 *
 * <p>实现类负责把字段路径和参数名渲染为 HQL 片段。内置运算符见
 * {@link StandardHqlOperator}，业务也可以通过实现该接口扩展
 * 自定义运算符。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public interface HqlOperator {

    /**
     * 获取运算符需要的参数数量。
     *
     * @return 参数数量
     */
    int parameterCount();

    /**
     * 渲染条件片段。
     *
     * @param path           字段路径，例如 {@code o.status}
     * @param parameterNames 按顺序分配的命名参数，数量等于
     *                       {@link #parameterCount()}
     * @return HQL 条件片段
     */
    String render(String path, List<String> parameterNames);
}
