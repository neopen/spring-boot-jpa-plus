package io.github.neopen.core.scope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JpaPlusScope} 单元测试。
 */
class JpaPlusScopeTest {

    /**
     * 清理线程绑定，避免测试间互相影响。
     */
    @AfterEach
    void reset() {
        JpaPlusScope.clear();
    }

    /**
     * 验证绑定与清除作用域。
     */
    @Test
    void shouldBindAndClearScope() {
        assertThat(JpaPlusScope.current()).isNull();
        assertThat(JpaPlusScope.currentScopeName()).isNull();

        JpaPlusScope.set(" secondary ");

        assertThat(JpaPlusScope.current()).isEqualTo("secondary");
        assertThat(JpaPlusScope.currentScopeName()).isEqualTo("secondary");

        JpaPlusScope.clear();

        assertThat(JpaPlusScope.current()).isNull();
    }

    /**
     * 验证默认作用域等同于未绑定。
     */
    @Test
    void shouldTreatDefaultScopeAsGlobal() {
        assertThat(JpaPlusScope.isDefault(null)).isTrue();
        assertThat(JpaPlusScope.isDefault(JpaPlusScope.DEFAULT)).isTrue();
        assertThat(JpaPlusScope.isDefault("secondary")).isFalse();

        JpaPlusScope.set(JpaPlusScope.DEFAULT);

        assertThat(JpaPlusScope.current()).isEqualTo(JpaPlusScope.DEFAULT);
        assertThat(JpaPlusScope.currentScopeName()).isNull();
    }

    /**
     * 验证执行结束后恢复外层作用域。
     */
    @Test
    void shouldRestorePreviousScope() {
        JpaPlusScope.set("outer");

        String inner = JpaPlusScope.call("inner", JpaPlusScope::current);

        assertThat(inner).isEqualTo("inner");
        assertThat(JpaPlusScope.current()).isEqualTo("outer");
    }

    /**
     * 验证异常时同样恢复外层作用域。
     */
    @Test
    void shouldRestoreScopeOnException() {
        assertThatThrownBy(() -> JpaPlusScope.run("broken", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(JpaPlusScope.current()).isNull();
    }

    /**
     * 验证作用域绑定不跨线程传播。
     *
     * @throws Exception 线程执行失败
     */
    @Test
    void shouldIsolateScopePerThread() throws Exception {
        JpaPlusScope.set("main");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<String> other = executor.submit(
                    () -> JpaPlusScope.current()
            );

            assertThat(other.get()).isNull();
            assertThat(JpaPlusScope.current()).isEqualTo("main");
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 验证非法参数被拒绝。
     */
    @Test
    void shouldRejectInvalidArguments() {
        assertThatThrownBy(() -> JpaPlusScope.set(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scope name");

        assertThatThrownBy(() -> JpaPlusScope.call("secondary", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scoped action");

        assertThatThrownBy(() -> JpaPlusScope.run(null, () -> {
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scope name");
    }
}
