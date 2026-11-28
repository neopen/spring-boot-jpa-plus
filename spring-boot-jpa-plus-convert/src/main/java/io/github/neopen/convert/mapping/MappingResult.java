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
package io.github.neopen.convert.mapping;

import java.util.List;

/**
 * 参数映射结果。
 *
 * <p>除映射后的实体外，还记录参数中未匹配的字段以及发生过类型转换的
 * 字段，便于排查“字段为什么没有写进去”一类问题。</p>
 *
 * @param entity          映射后的实体
 * @param unknownFields   参数中未匹配到实体字段的名称
 * @param convertedFields 发生过类型转换或关联解析的参数字段名
 *
 * @param <E> 实体类型
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record MappingResult<E>(
        E entity,
        List<String> unknownFields,
        List<String> convertedFields
) {

    /**
     * 创建映射结果并复制集合，避免外部修改。
     *
     * @param entity          映射后的实体
     * @param unknownFields   参数中未匹配到实体字段的名称
     * @param convertedFields 发生过类型转换或关联解析的参数字段名
     */
    public MappingResult {
        unknownFields = unknownFields == null
                ? List.of()
                : List.copyOf(unknownFields);
        convertedFields = convertedFields == null
                ? List.of()
                : List.copyOf(convertedFields);
    }
}
