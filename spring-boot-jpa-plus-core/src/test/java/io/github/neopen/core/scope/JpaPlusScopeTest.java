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
package io.github.neopen.core.scope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
     * 验证两个线程各自绑定不同作用域时互不干扰。
     *
     * @throws Exception 线程执行失败
     */
    @Test
    void shouldIsolateConcurrentScopes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        try {
            Future<String> first = executor.submit(() -> JpaPlusScope.call(
                    "first",
                    () -> currentAfterBarrier(barrier)
            ));
            Future<String> second = executor.submit(() -> JpaPlusScope.call(
                    "second",
                    () -> currentAfterBarrier(barrier)
            ));

            assertThat(first.get()).isEqualTo("first");
            assertThat(second.get()).isEqualTo("second");
            assertThat(JpaPlusScope.current()).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 等待两个线程都进入作用域后再读取。
     *
     * @param barrier 同步屏障
     * @return 当前线程绑定的作用域
     */
    private static String currentAfterBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot synchronize scoped threads",
                    e
            );
        }

        return JpaPlusScope.current();
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
