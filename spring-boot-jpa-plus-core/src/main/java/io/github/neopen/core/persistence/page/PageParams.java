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
package io.github.neopen.core.persistence.page;

import io.github.neopen.core.persistence.sort.Sorts;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 请求侧分页参数。
 *
 * <p>把前端传入的页码、每页数量与排序表达式封装为经过校验的对象，
 * 并转换为 Spring Data {@link Pageable}。页码从 1 开始，与常见分页
 * 接口保持一致，内部转换时再减 1。</p>
 *
 * <pre>{@code
 * PageParams params = PageParams.builder()
 *         .page(1)
 *         .size(20)
 *         .sort("createTime,desc")
 *         .entityType(Order.class)
 *         .build();
 *
 * Pageable pageable = params.toPageable();
 * }</pre>
 *
 * <p>每页数量默认上限为 {@value #DEFAULT_MAX_SIZE}，超过上限时抛出
 * {@link IllegalArgumentException}，避免外部传入超大分页拖垮数据库。
 * 配置了实体类型、别名映射或允许字段集合时，排序表达式会通过
 * {@link Sorts} 校验，防止任意字段排序；三者都未配置时，忽略排序
 * 表达式而不是放行未校验的字段。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class PageParams {

    /** 默认页码。 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页数量。 */
    public static final int DEFAULT_SIZE = 10;

    /** 默认每页数量上限。 */
    public static final int DEFAULT_MAX_SIZE = 500;

    /** 页码，从 1 开始。 */
    private final int page;

    /** 每页数量。 */
    private final int size;

    /** 排序表达式。 */
    private final String sort;

    /** 每页数量上限。 */
    private final int maxSize;

    /** 排序校验使用的实体类型。 */
    private final Class<?> entityType;

    /** 外部字段名到实体属性路径的映射。 */
    private final Map<String, String> aliases;

    /** 允许排序的字段集合。 */
    private final Set<String> allowedFields;

    /**
     * 使用构建器创建分页参数。
     *
     * @param builder 构建器
     */
    private PageParams(Builder builder) {
        this.page = builder.page;
        this.size = builder.size;
        this.sort = builder.sort;
        this.maxSize = builder.maxSize;
        this.entityType = builder.entityType;
        this.aliases = builder.aliases == null
                ? null
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(builder.aliases)
                );
        this.allowedFields = builder.allowedFields == null
                ? null
                : Set.copyOf(builder.allowedFields);
    }

    /**
     * 使用默认每页数量创建分页参数。
     *
     * @param page 页码，从 1 开始
     * @return 分页参数
     */
    public static PageParams of(int page) {
        return builder().page(page).build();
    }

    /**
     * 创建分页参数。
     *
     * @param page 页码，从 1 开始
     * @param size 每页数量
     * @return 分页参数
     */
    public static PageParams of(int page, int size) {
        return builder().page(page).size(size).build();
    }

    /**
     * 创建分页参数。
     *
     * @param page 页码，从 1 开始
     * @param size 每页数量
     * @param sort 排序表达式，允许为 {@code null}
     * @return 分页参数
     */
    public static PageParams of(int page, int size, String sort) {
        return builder().page(page).size(size).sort(sort).build();
    }

    /**
     * 创建分页参数构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取页码。
     *
     * @return 页码，从 1 开始
     */
    public int getPage() {
        return page;
    }

    /**
     * 获取每页数量。
     *
     * @return 每页数量
     */
    public int getSize() {
        return size;
    }

    /**
     * 获取排序表达式。
     *
     * @return 排序表达式；未设置时返回 {@code null}
     */
    public String getSort() {
        return sort;
    }

    /**
     * 获取查询偏移量。
     *
     * @return 偏移量，从 0 开始
     */
    public long getOffset() {
        return (long) (page - 1) * size;
    }

    /**
     * 判断是否为第一页。
     *
     * @return 第一页返回 {@code true}
     */
    public boolean isFirst() {
        return page == 1;
    }

    /**
     * 转换为 Spring Data 分页对象。
     *
     * @return 分页对象
     * @throws IllegalArgumentException 排序表达式非法或未配置校验依据时抛出
     */
    public Pageable toPageable() {
        return PageRequest.of(page - 1, size, resolveSort());
    }

    /**
     * 解析并校验排序表达式。
     *
     * <p>未配置实体类型、别名映射和允许字段集合时返回未排序，
     * 避免未校验的字段直接进入 SQL。</p>
     *
     * @return 排序规则
     */
    private Sort resolveSort() {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }

        if (allowedFields != null) {
            return Sorts.parse(sort, allowedFields);
        }

        if (entityType != null) {
            return aliases == null
                    ? Sorts.parse(sort, entityType)
                    : Sorts.parse(sort, entityType, aliases);
        }

        return Sort.unsorted();
    }

    /**
     * 分页参数构建器。
     */
    public static final class Builder {

        /** 页码。 */
        private int page = DEFAULT_PAGE;

        /** 每页数量。 */
        private int size = DEFAULT_SIZE;

        /** 排序表达式。 */
        private String sort;

        /** 每页数量上限。 */
        private int maxSize = DEFAULT_MAX_SIZE;

        /** 排序校验使用的实体类型。 */
        private Class<?> entityType;

        /** 外部字段名到实体属性路径的映射。 */
        private Map<String, String> aliases;

        /** 允许排序的字段集合。 */
        private Set<String> allowedFields;

        /** 创建构建器。 */
        private Builder() {
        }

        /**
         * 设置页码。
         *
         * @param page 页码，从 1 开始
         * @return 当前构建器
         */
        public Builder page(int page) {
            this.page = page;
            return this;
        }

        /**
         * 设置每页数量。
         *
         * @param size 每页数量
         * @return 当前构建器
         */
        public Builder size(int size) {
            this.size = size;
            return this;
        }

        /**
         * 设置排序表达式。
         *
         * @param sort 排序表达式，例如 {@code createTime,desc}
         * @return 当前构建器
         */
        public Builder sort(String sort) {
            this.sort = sort;
            return this;
        }

        /**
         * 设置每页数量上限。
         *
         * @param maxSize 上限，必须大于 0
         * @return 当前构建器
         */
        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        /**
         * 设置排序校验使用的实体类型。
         *
         * @param entityType 实体类型
         * @return 当前构建器
         */
        public Builder entityType(Class<?> entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * 设置外部字段名到实体属性路径的映射。
         *
         * @param aliases 别名映射
         * @return 当前构建器
         */
        public Builder aliases(Map<String, String> aliases) {
            this.aliases = aliases;
            return this;
        }

        /**
         * 设置允许排序的字段集合。
         *
         * @param allowedFields 允许排序的字段集合
         * @return 当前构建器
         */
        public Builder allowedFields(Set<String> allowedFields) {
            this.allowedFields = allowedFields;
            return this;
        }

        /**
         * 设置允许排序的字段集合。
         *
         * @param allowedFields 允许排序的字段
         * @return 当前构建器
         */
        public Builder allowedFields(String... allowedFields) {
            this.allowedFields = allowedFields == null
                    ? null
                    : Set.of(allowedFields);
            return this;
        }

        /**
         * 构建分页参数。
         *
         * @return 分页参数
         * @throws IllegalArgumentException 页码、每页数量或上限非法时抛出
         */
        public PageParams build() {
            if (maxSize <= 0) {
                throw new IllegalArgumentException(
                        "Max page size must be greater than zero"
                );
            }

            if (page < 1) {
                throw new IllegalArgumentException(
                        "Page number must be greater than zero"
                );
            }

            if (size < 1) {
                throw new IllegalArgumentException(
                        "Page size must be greater than zero"
                );
            }

            if (size > maxSize) {
                throw new IllegalArgumentException(
                        "Page size must not exceed " + maxSize
                );
            }

            return new PageParams(this);
        }
    }
}
