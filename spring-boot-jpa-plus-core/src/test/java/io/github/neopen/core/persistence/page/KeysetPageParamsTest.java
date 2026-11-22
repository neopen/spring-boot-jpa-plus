package io.github.neopen.core.persistence.page;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link KeysetPageParams} 单元测试。
 */
class KeysetPageParamsTest {

    /**
     * 验证默认值与第一页判断。
     */
    @Test
    void shouldApplyDefaults() {
        KeysetPageParams params = KeysetPageParams.builder()
                .sort(Sort.by("id"))
                .build();

        assertThat(params.getSize())
                .isEqualTo(KeysetPageParams.DEFAULT_SIZE);
        assertThat(params.getSort().isSorted()).isTrue();
        assertThat(params.getCursor()).isNull();
        assertThat(params.isFirst()).isTrue();
    }

    /**
     * 验证便捷工厂方法与游标设置。
     */
    @Test
    void shouldCreateWithCursor() {
        KeysetCursor cursor = KeysetCursor.of(10L);
        KeysetPageParams params = KeysetPageParams.builder()
                .size(20)
                .sort(Sort.by(Sort.Order.desc("id")))
                .cursor(cursor)
                .build();

        assertThat(params.getSize()).isEqualTo(20);
        assertThat(params.getCursor()).isSameAs(cursor);
        assertThat(params.isFirst()).isFalse();
        assertThat(KeysetPageParams.of(5, Sort.by("id")).getSize())
                .isEqualTo(5);
    }

    /**
     * 验证每页数量校验。
     */
    @Test
    void shouldValidateSize() {
        assertThatThrownBy(() -> KeysetPageParams.builder()
                .size(0)
                .sort(Sort.by("id"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        assertThatThrownBy(() -> KeysetPageParams.builder()
                .size(KeysetPageParams.DEFAULT_MAX_SIZE + 1)
                .sort(Sort.by("id"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed");
    }

    /**
     * 验证排序校验。
     */
    @Test
    void shouldValidateSort() {
        assertThatThrownBy(() -> KeysetPageParams.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sort must not be empty");

        assertThatThrownBy(() -> KeysetPageParams.builder()
                .sort(Sort.unsorted())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sort must not be empty");

        assertThatThrownBy(() -> KeysetPageParams.builder()
                .sort(Sort.by(
                        Sort.Order.asc("name").ignoreCase()
                ))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("case-insensitive");

        assertThatThrownBy(() -> KeysetPageParams.builder()
                .sort(Sort.by(
                        Sort.Order.asc("name")
                                .with(Sort.NullHandling.NULLS_FIRST)
                ))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null handling");
    }
}
