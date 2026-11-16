package io.github.neopen.core.persistence.datasource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DataSourceContext} 单元测试。
 */
class DataSourceContextTest {

    /**
     * 清理线程上下文。
     */
    @AfterEach
    void reset() {
        DataSourceContext.clear();
    }

    /**
     * 验证嵌套切换会恢复外层数据源。
     */
    @Test
    void shouldRestoreOuterTargetAfterNestedRun() {
        DataSourceContext.run("write", () -> {
            assertThat(DataSourceContext.current()).isEqualTo("write");

            DataSourceContext.run("read", () ->
                    assertThat(DataSourceContext.current()).isEqualTo("read")
            );

            assertThat(DataSourceContext.current()).isEqualTo("write");
        });

        assertThat(DataSourceContext.current()).isNull();
    }

    /**
     * 验证有返回值时同样恢复上下文。
     */
    @Test
    void shouldReturnValueAndRestoreContext() {
        String target = DataSourceContext.call(
                " archive ",
                DataSourceContext::current
        );

        assertThat(target).isEqualTo("archive");
        assertThat(DataSourceContext.current()).isNull();
    }

    /**
     * 验证异常时仍会弹出上下文。
     */
    @Test
    void shouldRestoreContextWhenActionFails() {
        assertThatThrownBy(() -> DataSourceContext.run("read", () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(DataSourceContext.current()).isNull();
    }

    /**
     * 验证空名称被拒绝。
     */
    @Test
    void shouldRejectBlankTarget() {
        assertThatThrownBy(() -> DataSourceContext.push(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    /**
     * 验证空栈弹出返回 {@code null}。
     */
    @Test
    void shouldReturnNullWhenStackIsEmpty() {
        assertThat(DataSourceContext.pop()).isNull();
    }

    /**
     * 验证不同线程的数据源互不影响。
     *
     * @throws InterruptedException 等待线程结束时抛出
     */
    @Test
    void shouldIsolateTargetsBetweenThreads() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> observed = new AtomicReference<>();

        DataSourceContext.run("write", () -> {
            Thread thread = new Thread(() -> {
                started.countDown();

                try {
                    finished.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                observed.set(DataSourceContext.current());
            });

            thread.start();

            try {
                started.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            finished.countDown();
        });

        assertThat(observed).hasValue(null);
        assertThat(DataSourceContext.current()).isNull();
    }
}
