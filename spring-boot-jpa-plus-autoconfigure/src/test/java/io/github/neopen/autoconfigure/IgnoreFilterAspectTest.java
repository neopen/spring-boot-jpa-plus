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

import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.datascope.IgnoreDataScope;
import io.github.neopen.core.persistence.tenant.IgnoreTenant;
import io.github.neopen.core.persistence.tenant.Tenants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IgnoreFilterAspect} 单元测试。
 */
class IgnoreFilterAspectTest {

    /**
     * 清理线程上的跳过状态。
     */
    @AfterEach
    void reset() {
        while (Tenants.isIgnored()) {
            Tenants.restore();
        }

        while (DataScopes.isIgnored()) {
            DataScopes.restore();
        }
    }

    /**
     * 验证两个注解同时生效。
     */
    @Test
    void shouldIgnoreBothFilters() {
        Probe probe = proxy();

        assertThat(probe.both()).isTrue();
        assertThat(Tenants.isIgnored()).isFalse();
        assertThat(DataScopes.isIgnored()).isFalse();
    }

    /**
     * 验证嵌套调用结束后外层仍然跳过过滤。
     */
    @Test
    void shouldKeepOuterIgnoreAfterNestedCall() {
        Probe probe = proxy();

        assertThat(probe.outer()).isTrue();
        assertThat(Tenants.isIgnored()).isFalse();
    }

    /**
     * 验证未标注的方法不受影响。
     */
    @Test
    void shouldLeavePlainMethodUntouched() {
        Probe probe = proxy();

        assertThat(probe.plain()).isFalse();
    }

    /**
     * 创建测试代理。
     *
     * @return 被切面代理的对象
     */
    private Probe proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Probe());
        factory.addAspect(new IgnoreFilterAspect());

        return factory.getProxy();
    }

    /**
     * 测试目标。
     */
    public static class Probe {

        /**
         * 创建测试目标。
         */
        public Probe() {
        }

        /**
         * 跳过两个过滤。
         *
         * @return 两个过滤都被跳过时返回 {@code true}
         */
        @IgnoreTenant
        @IgnoreDataScope
        public boolean both() {
            return Tenants.isIgnored() && DataScopes.isIgnored();
        }

        /**
         * 外层方法跳过租户过滤并调用内层方法。
         *
         * @return 内层与返回后都保持跳过状态时返回 {@code true}
         */
        @IgnoreTenant
        public boolean outer() {
            boolean before = Tenants.isIgnored();
            boolean nested = inner();

            return before && nested && Tenants.isIgnored();
        }

        /**
         * 内层方法也跳过租户过滤。
         *
         * @return 跳过租户过滤时返回 {@code true}
         */
        @IgnoreTenant
        public boolean inner() {
            return Tenants.isIgnored();
        }

        /**
         * 未标注的方法。
         *
         * @return 任意过滤被跳过时返回 {@code true}
         */
        public boolean plain() {
            return Tenants.isIgnored() || DataScopes.isIgnored();
        }
    }
}
