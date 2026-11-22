package io.github.neopen.core.context;

import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.datasource.DataSourceContext;
import io.github.neopen.core.persistence.tenant.Tenants;
import io.github.neopen.core.scope.JpaPlusScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JpaPlusContext} 单元测试。
 */
class JpaPlusContextTest {

    /**
     * 清理测试线程上的上下文。
     */
    @AfterEach
    void tearDown() {
        JpaPlusContext.clear();
    }

    /**
     * 验证快照完整捕获并恢复作用域、跳过深度与数据源栈。
     */
    @Test
    void shouldCaptureAndApplyAllContextValues() {
        JpaPlusScope.set("secondary");
        DataScopes.ignore();
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");
        DataSourceContext.push("secondary");

        JpaPlusContext snapshot = JpaPlusContext.capture();

        assertThat(snapshot.getScope()).isEqualTo("secondary");
        assertThat(snapshot.getDataScopeIgnoreDepth()).isEqualTo(2);
        assertThat(snapshot.getTenantIgnoreDepth()).isEqualTo(1);
        assertThat(snapshot.getDataSourceTargets())
                .containsExactly("secondary", "read");

        JpaPlusContext.clear();

        assertThat(JpaPlusScope.current()).isNull();
        assertThat(DataScopes.ignoreDepth()).isZero();
        assertThat(Tenants.ignoreDepth()).isZero();
        assertThat(DataSourceContext.current()).isNull();

        snapshot.apply();

        assertThat(JpaPlusScope.current()).isEqualTo("secondary");
        assertThat(DataScopes.ignoreDepth()).isEqualTo(2);
        assertThat(Tenants.ignoreDepth()).isEqualTo(1);
        assertThat(DataSourceContext.current()).isEqualTo("secondary");
        assertThat(DataSourceContext.snapshot())
                .containsExactly("secondary", "read");
    }

    /**
     * 验证应用快照时把多余的深度降低到快照值。
     */
    @Test
    void shouldLowerDepthWhenApplyingEmptierSnapshot() {
        JpaPlusContext empty = JpaPlusContext.capture();

        JpaPlusScope.set("secondary");
        DataScopes.ignore();
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");

        empty.apply();

        assertThat(JpaPlusScope.current()).isNull();
        assertThat(DataScopes.ignoreDepth()).isZero();
        assertThat(Tenants.ignoreDepth()).isZero();
        assertThat(DataSourceContext.snapshot()).isEmpty();
    }

    /**
     * 验证 run 执行结束后恢复执行线程原有上下文。
     */
    @Test
    void shouldRestorePreviousContextAfterRun() {
        JpaPlusContext outer = JpaPlusContext.capture();

        JpaPlusScope.set("inner");
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");
        JpaPlusContext inner = JpaPlusContext.capture();

        outer.apply();

        List<String> observed = new ArrayList<>();

        inner.run(() -> {
            observed.add(JpaPlusScope.current());
            observed.add(String.valueOf(DataScopes.isIgnored()));
            observed.add(String.valueOf(Tenants.isIgnored()));
            observed.add(DataSourceContext.current());
        });

        assertThat(observed)
                .containsExactly("inner", "true", "true", "read");
        assertThat(JpaPlusScope.current()).isNull();
        assertThat(DataScopes.ignoreDepth()).isZero();
        assertThat(Tenants.ignoreDepth()).isZero();
        assertThat(DataSourceContext.snapshot()).isEmpty();
    }

    /**
     * 验证 call 执行异常时同样恢复原有上下文。
     */
    @Test
    void shouldRestorePreviousContextWhenCallFails() {
        JpaPlusContext empty = JpaPlusContext.capture();

        JpaPlusScope.set("async");
        DataScopes.ignore();
        JpaPlusContext snapshot = JpaPlusContext.capture();

        empty.apply();

        assertThatThrownBy(() -> snapshot.call(() -> {
            throw new IllegalStateException("boom");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(JpaPlusScope.current()).isNull();
        assertThat(DataScopes.ignoreDepth()).isZero();
    }

    /**
     * 验证快照可跨线程使用，且任务结束后工作线程上下文被清理。
     */
    @Test
    void shouldPropagateContextToAnotherThread() {
        JpaPlusScope.set("async");
        DataScopes.ignore();
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");

        JpaPlusContext snapshot = JpaPlusContext.capture();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            String observed = executor.submit(() -> snapshot.call(
                    () -> JpaPlusScope.current()
                            + "|" + DataScopes.ignoreDepth()
                            + "|" + Tenants.ignoreDepth()
                            + "|" + DataSourceContext.current()
            )).get();

            assertThat(observed).isEqualTo("async|2|1|read");

            JpaPlusContext remaining = executor.submit(
                    JpaPlusContext::capture
            ).get();

            assertThat(remaining.getScope()).isNull();
            assertThat(remaining.getDataScopeIgnoreDepth()).isZero();
            assertThat(remaining.getTenantIgnoreDepth()).isZero();
            assertThat(remaining.getDataSourceTargets()).isEmpty();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 验证 clear 清理全部上下文。
     */
    @Test
    void shouldClearAllContext() {
        JpaPlusScope.set("async");
        DataScopes.ignore();
        Tenants.ignore();
        DataSourceContext.push("read");

        JpaPlusContext.clear();

        assertThat(JpaPlusScope.current()).isNull();
        assertThat(DataScopes.ignoreDepth()).isZero();
        assertThat(Tenants.ignoreDepth()).isZero();
        assertThat(DataSourceContext.snapshot()).isEmpty();
    }

    /**
     * 验证空执行逻辑被拒绝。
     */
    @Test
    void shouldRejectNullAction() {
        JpaPlusContext snapshot = JpaPlusContext.capture();

        assertThatThrownBy(() -> snapshot.run(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");

        assertThatThrownBy(() -> snapshot.call(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action");
    }

    /**
     * 验证快照列表不可变。
     */
    @Test
    void shouldExposeImmutableDataSourceTargets() {
        DataSourceContext.push("read");
        JpaPlusContext snapshot = JpaPlusContext.capture();

        DataSourceContext.push("secondary");

        assertThat(snapshot.getDataSourceTargets())
                .containsExactly("read");
        assertThatThrownBy(
                () -> snapshot.getDataSourceTargets().add("other")
        ).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 验证并发捕获互不影响。
     */
    @Test
    void shouldIsolateConcurrentSnapshots() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            CompletableFuture<String> first = CompletableFuture.supplyAsync(
                    () -> JpaPlusScope.call("first", () -> {
                        DataScopes.ignore();
                        return JpaPlusContext.capture().getScope();
                    }),
                    executor
            );
            CompletableFuture<String> second = CompletableFuture.supplyAsync(
                    () -> JpaPlusScope.call("second", () ->
                            JpaPlusContext.capture().getScope()),
                    executor
            );

            assertThat(first.get()).isEqualTo("first");
            assertThat(second.get()).isEqualTo("second");
        } finally {
            executor.shutdownNow();
        }
    }
}
