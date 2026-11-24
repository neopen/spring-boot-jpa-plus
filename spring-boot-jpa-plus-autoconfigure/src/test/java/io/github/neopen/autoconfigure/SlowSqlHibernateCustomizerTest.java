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
package io.github.neopen.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SlowSqlHibernateCustomizer} 单元测试。
 */
class SlowSqlHibernateCustomizerTest {

    /**
     * 验证慢查询阈值写入 Hibernate 属性。
     */
    @Test
    void shouldConfigureSlowQueryThreshold() {
        Map<String, Object> properties = new HashMap<>();

        new SlowSqlHibernateCustomizer(1500L).customize(properties);

        assertThat(properties).containsEntry(
                "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS",
                1500L
        );
    }

    /**
     * 验证阈值必须大于 0。
     */
    @Test
    void shouldRejectNonPositiveThreshold() {
        assertThatThrownBy(() -> new SlowSqlHibernateCustomizer(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }
}
