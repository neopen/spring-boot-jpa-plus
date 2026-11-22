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
