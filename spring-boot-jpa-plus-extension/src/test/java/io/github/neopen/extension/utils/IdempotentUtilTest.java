package io.github.neopen.extension.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IdempotentUtil} 单元测试。
 */
class IdempotentUtilTest {

    /**
     * 验证同一业务键只能占用一次。
     */
    @Test
    void shouldAcquireBusinessKeyOnlyOnce() {
        String key = "order-" + UUID.randomUUID();

        assertThat(IdempotentUtil.acquire(key, 30)).isTrue();
        assertThat(IdempotentUtil.acquire(key, 30)).isFalse();
    }

    /**
     * 验证不同业务键互不影响。
     */
    @Test
    void shouldAllowDifferentBusinessKeys() {
        String prefix = UUID.randomUUID().toString();

        assertThat(IdempotentUtil.acquire(prefix + "-a", 30)).isTrue();
        assertThat(IdempotentUtil.acquire(prefix + "-b", 30)).isTrue();
    }

    /**
     * 验证空业务键被拒绝。
     */
    @Test
    void shouldRejectBlankBusinessKey() {
        assertThatThrownBy(() -> IdempotentUtil.acquire(" ", 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    /**
     * 验证一次性令牌成功校验后立即失效。
     */
    @Test
    void shouldConsumeTokenOnlyOnce() {
        String session = UUID.randomUUID().toString();

        IdempotentUtil.tokenMap.put(session, "token-1");

        assertThat(IdempotentUtil.consume(session, "token-1")).isTrue();
        assertThat(IdempotentUtil.consume(session, "token-1")).isFalse();
    }

    /**
     * 验证令牌不匹配时校验失败。
     */
    @Test
    void shouldRejectMismatchedToken() {
        String session = UUID.randomUUID().toString();

        IdempotentUtil.tokenMap.put(session, "token-1");

        assertThat(IdempotentUtil.consume(session, "token-2")).isFalse();
    }
}
