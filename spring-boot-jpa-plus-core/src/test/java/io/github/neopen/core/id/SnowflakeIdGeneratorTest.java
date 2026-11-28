/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
package io.github.neopen.core.id;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SnowflakeIdGenerator} 单元测试。
 */
class SnowflakeIdGeneratorTest {

    /**
     * 验证生成的 ID 唯一且递增。
     */
    @Test
    void shouldGenerateUniqueAndIncreasingIds() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

        long previous = -1L;
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 10_000; i++) {
            long id = generator.nextId();

            assertThat(id).isGreaterThan(previous);
            assertThat(ids.add(id)).isTrue();

            previous = id;
        }
    }

    /**
     * 验证多线程下不产生重复 ID。
     *
     * @throws InterruptedException 线程等待被中断时抛出
     */
    @Test
    void shouldGenerateUniqueIdsConcurrently()
            throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, 3);
        int threads = 4;
        int perThread = 1_000;

        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    for (int j = 0; j < perThread; j++) {
                        ids.add(generator.nextId());
                    }
                });
            }
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS))
                    .isTrue();
        }

        assertThat(ids).hasSize(threads * perThread);
    }

    /**
     * 验证构造参数范围校验。
     */
    @Test
    void shouldValidateConstructorArguments() {
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(32, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(0, -1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(0, 32))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证自定义纪元与默认纪元生成的 ID 均为正数。
     */
    @Test
    void shouldGeneratePositiveIds() {
        SnowflakeIdGenerator defaults = new SnowflakeIdGenerator(0, 0);
        SnowflakeIdGenerator custom = new SnowflakeIdGenerator(
                31,
                31,
                System.currentTimeMillis() - 1_000L
        );

        assertThat(defaults.nextId()).isPositive();
        assertThat(custom.nextId()).isPositive();
    }
}
