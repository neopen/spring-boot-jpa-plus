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
package io.github.neopen.extension.response;

import java.util.List;

/**
 * JSON 分页响应工厂。
 *
 * <p>负责创建统一的分页响应对象。业务项目可实现此接口，自定义
 * 成功消息、响应编码、空数据响应及统一响应对象的扩展属性。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public interface JsonPageResponseFactory {

    /**
     * 创建分页成功响应。
     *
     * @param <V>   视图对象类型
     * @param data  当前页数据
     * @param total 总记录数，不能小于 0
     * @return JSON 分页成功响应
     */
    <V> Json<List<V>> create(
            List<V> data,
            long total
    );
}