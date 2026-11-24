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

import java.util.List;

/**
 * 游标（keyset）分页结果。
 *
 * <p>包含当前页数据与下一页游标。没有下一页时
 * {@link #getNextCursor()} 返回 {@code null}，调用方不再需要额外
 * 维护页码或总记录数。</p>
 *
 * <pre>{@code
 * KeysetPage<Order> page = executor.keyset(spec, params);
 *
 * if (page.hasNext()) {
 *     String next = page.getNextCursor().encode();
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 *
 * @author neopen
 * @see KeysetCursor
 * @see KeysetPageParams
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class KeysetPage<T> {

    /** 当前页数据。 */
    private final List<T> content;

    /** 下一页游标。 */
    private final KeysetCursor nextCursor;

    /**
     * 创建游标分页结果。
     *
     * @param content    当前页数据
     * @param nextCursor 下一页游标，没有下一页时传 {@code null}
     * @throws IllegalArgumentException 当前页数据为空时抛出
     */
    public KeysetPage(List<T> content, KeysetCursor nextCursor) {
        if (content == null) {
            throw new IllegalArgumentException(
                    "Content must not be null"
            );
        }

        this.content = List.copyOf(content);
        this.nextCursor = nextCursor;
    }

    /**
     * 获取当前页数据。
     *
     * @return 当前页数据
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 获取下一页游标。
     *
     * @return 下一页游标；没有下一页时返回 {@code null}
     */
    public KeysetCursor getNextCursor() {
        return nextCursor;
    }

    /**
     * 判断是否存在下一页。
     *
     * @return 存在下一页时返回 {@code true}
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
