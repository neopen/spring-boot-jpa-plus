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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link KeysetCursor} 单元测试。
 */
class KeysetCursorTest {

    /**
     * 验证编码与解码可以往返。
     */
    @Test
    void shouldEncodeAndDecodeRoundTrip() {
        KeysetCursor cursor = KeysetCursor.of(100L, "PAID");

        KeysetCursor restored = KeysetCursor.decode(cursor.encode());

        assertThat(restored.getValues()).containsExactly("100", "PAID");
    }

    /**
     * 验证游标值保留空字符串。
     */
    @Test
    void shouldKeepEmptyStringValue() {
        KeysetCursor cursor = KeysetCursor.of("", "A");

        KeysetCursor restored = KeysetCursor.decode(cursor.encode());

        assertThat(restored.getValues()).containsExactly("", "A");
    }

    /**
     * 验证非法游标被拒绝。
     */
    @Test
    void shouldRejectInvalidCursor() {
        assertThatThrownBy(KeysetCursor::of)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");

        assertThatThrownBy(() -> KeysetCursor.of("A", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");

        assertThatThrownBy(() -> KeysetCursor.decode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> KeysetCursor.decode("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");

        assertThatThrownBy(() -> KeysetCursor.decode("***"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cursor");
    }
}
