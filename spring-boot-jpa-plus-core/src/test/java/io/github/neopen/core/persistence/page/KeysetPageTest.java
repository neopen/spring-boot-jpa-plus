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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link KeysetPage} 单元测试。
 */
class KeysetPageTest {

    /**
     * 验证无下一页时游标为空。
     */
    @Test
    void shouldReportNoNextPage() {
        KeysetPage<String> page = new KeysetPage<>(List.of("a"), null);

        assertThat(page.getContent()).containsExactly("a");
        assertThat(page.getNextCursor()).isNull();
        assertThat(page.hasNext()).isFalse();
    }

    /**
     * 验证有下一页时返回游标。
     */
    @Test
    void shouldReportNextPage() {
        KeysetCursor cursor = KeysetCursor.of("a");
        KeysetPage<String> page = new KeysetPage<>(List.of("a"), cursor);

        assertThat(page.getNextCursor()).isSameAs(cursor);
        assertThat(page.hasNext()).isTrue();
    }

    /**
     * 验证数据为空的非法参数。
     */
    @Test
    void shouldRejectNullContent() {
        assertThatThrownBy(() -> new KeysetPage<>(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content");
    }
}
