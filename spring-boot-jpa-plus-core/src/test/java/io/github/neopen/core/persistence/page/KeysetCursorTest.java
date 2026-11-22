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
