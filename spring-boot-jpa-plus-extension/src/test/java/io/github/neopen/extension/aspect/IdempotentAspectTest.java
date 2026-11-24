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
package io.github.neopen.extension.aspect;

import io.github.neopen.extension.annotation.Idempotent;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IdempotentAspect} 单元测试。
 */
class IdempotentAspectTest {

    /**
     * 验证同一业务键只允许成功执行一次。
     */
    @Test
    void shouldRejectDuplicateBusinessKey() throws Exception {
        IdempotentService proxy = createProxy();
        String orderNo = UUID.randomUUID().toString();

        proxy.submit(orderNo);

        assertThatThrownBy(() -> proxy.submit(orderNo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重复提交");
    }

    /**
     * 验证不同业务键互不影响。
     */
    @Test
    void shouldAllowDifferentBusinessKeys() {
        IdempotentService proxy = createProxy();

        assertThatCode(() -> {
            proxy.submit(UUID.randomUUID().toString());
            proxy.submit(UUID.randomUUID().toString());
        }).doesNotThrowAnyException();
    }

    /**
     * 验证 SpEL 结果为空的业务键被拒绝。
     */
    @Test
    void shouldRejectBlankBusinessKey() {
        IdempotentService proxy = createProxy();

        assertThatThrownBy(() -> proxy.submit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("幂等键为空");
    }

    /**
     * 验证 required=false 时跳过校验。
     */
    @Test
    void shouldSkipWhenNotRequired() {
        IdempotentService proxy = createProxy();

        assertThatCode(() -> {
            proxy.optional();
            proxy.optional();
        }).doesNotThrowAnyException();
    }

    /**
     * 创建带幂等切面的代理。
     *
     * @return 代理对象
     */
    private IdempotentService createProxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(
                new IdempotentService()
        );

        factory.setProxyTargetClass(true);
        factory.addAspect(new IdempotentAspect());

        return factory.getProxy();
    }

    /** 幂等服务。 */
    static class IdempotentService {

        /**
         * 按订单号幂等提交。
         *
         * @param orderNo 订单号
         */
        @Idempotent(required = true, key = "#orderNo", ttl = 30)
        public void submit(String orderNo) {
        }

        /**
         * 不开启幂等校验的方法。
         */
        @Idempotent(required = false)
        public void optional() {
        }
    }
}
