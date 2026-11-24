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
package io.github.neopen.core.persistence.page;

import org.springframework.data.domain.Sort;

/**
 * 游标（keyset）分页参数。
 *
 * <p>与 {@link PageParams} 的 offset 分页不同，游标分页不记录页码，
 * 而是携带上一页的 {@link KeysetCursor}，由查询执行器生成
 * {@code WHERE (sortKey, id) > (?, ?)} 条件。排序键必须唯一且非空，
 * 执行器会自动追加主键作为 tiebreaker。</p>
 *
 * <pre>{@code
 * KeysetPageParams params = KeysetPageParams.builder()
 *         .size(20)
 *         .sort(Sort.by("createTime").descending())
 *         .cursor(KeysetCursor.decode(request.getCursor()))
 *         .build();
 *
 * KeysetPage<Order> page = executor.keyset(spec, params);
 * }</pre>
 *
 * <p>为避免游标条件无法命中，排序不允许忽略大小写，也不允许把
 * {@code null} 值排在最前或最后；这两类排序都无法用简单的比较
 * 条件稳定表达。</p>
 *
 * @author neopen
 * @see KeysetCursor
 * @see KeysetPage
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class KeysetPageParams {

    /** 默认每页数量。 */
    public static final int DEFAULT_SIZE = 10;

    /** 每页数量上限。 */
    public static final int DEFAULT_MAX_SIZE = 500;

    /** 每页数量。 */
    private final int size;

    /** 排序规则。 */
    private final Sort sort;

    /** 上一页游标，第一页为 {@code null}。 */
    private final KeysetCursor cursor;

    /**
     * 使用构建器创建游标分页参数。
     *
     * @param builder 构建器
     */
    private KeysetPageParams(Builder builder) {
        this.size = builder.size;
        this.sort = builder.sort;
        this.cursor = builder.cursor;
    }

    /**
     * 创建游标分页参数。
     *
     * @param size 每页数量
     * @param sort 排序规则
     * @return 游标分页参数
     */
    public static KeysetPageParams of(int size, Sort sort) {
        return builder().size(size).sort(sort).build();
    }

    /**
     * 创建游标分页参数构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
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
     * 获取排序规则。
     *
     * @return 排序规则
     */
    public Sort getSort() {
        return sort;
    }

    /**
     * 获取上一页游标。
     *
     * @return 游标；第一页返回 {@code null}
     */
    public KeysetCursor getCursor() {
        return cursor;
    }

    /**
     * 判断是否为第一页。
     *
     * @return 未携带游标时返回 {@code true}
     */
    public boolean isFirst() {
        return cursor == null;
    }

    /**
     * 游标分页参数构建器。
     */
    public static final class Builder {

        /** 每页数量。 */
        private int size = DEFAULT_SIZE;

        /** 排序规则。 */
        private Sort sort;

        /** 上一页游标。 */
        private KeysetCursor cursor;

        /** 创建构建器。 */
        private Builder() {
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
         * 设置排序规则。
         *
         * @param sort 排序规则
         * @return 当前构建器
         */
        public Builder sort(Sort sort) {
            this.sort = sort;
            return this;
        }

        /**
         * 设置上一页游标。
         *
         * @param cursor 游标，第一页传入 {@code null}
         * @return 当前构建器
         */
        public Builder cursor(KeysetCursor cursor) {
            this.cursor = cursor;
            return this;
        }

        /**
         * 构建游标分页参数。
         *
         * @return 游标分页参数
         * @throws IllegalArgumentException 每页数量或排序规则非法时抛出
         */
        public KeysetPageParams build() {
            if (size < 1) {
                throw new IllegalArgumentException(
                        "Page size must be greater than zero"
                );
            }

            if (size > DEFAULT_MAX_SIZE) {
                throw new IllegalArgumentException(
                        "Page size must not exceed " + DEFAULT_MAX_SIZE
                );
            }

            if (sort == null || sort.isUnsorted()) {
                throw new IllegalArgumentException(
                        "Sort must not be empty"
                );
            }

            for (Sort.Order order : sort) {
                if (order.isIgnoreCase()) {
                    throw new IllegalArgumentException(
                            "Keyset pagination does not support"
                                    + " case-insensitive sort"
                    );
                }

                if (order.getNullHandling() != Sort.NullHandling.NATIVE) {
                    throw new IllegalArgumentException(
                            "Keyset pagination does not support"
                                    + " null handling in sort"
                    );
                }
            }

            return new KeysetPageParams(this);
        }
    }
}
