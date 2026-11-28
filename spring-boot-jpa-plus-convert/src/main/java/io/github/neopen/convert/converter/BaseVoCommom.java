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
package io.github.neopen.convert.converter;

import org.springframework.lang.Nullable;

/**
 * 基础视图对象转换函数。
 *
 * @param <T> 实体类型
 * @param <V> 视图对象类型
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface BaseVoCommom<T, V> {

    /**
     * 将实体转换为基础视图对象。
     *
     * @param entity 待转换实体
     * @return 转换后的视图对象，允许为 {@code null}
     */
    @Nullable
    V setCommonVo(T entity);

}
