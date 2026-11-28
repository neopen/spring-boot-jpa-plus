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
package io.github.neopen.extension.aspect;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.neopen.extension.annotation.SlowLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SlowLogAspect} 单元测试。
 */
class SlowLogAspectTest {

    /** 切面日志。 */
    private Logger logger;

    /** 日志收集器。 */
    private ListAppender<ILoggingEvent> appender;

    /**
     * 挂载日志收集器。
     */
    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(SlowLogAspect.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    /**
     * 卸载日志收集器。
     */
    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    /**
     * 验证超过阈值时输出慢日志。
     */
    @Test
    void shouldLogSlowMethod() {
        SlowService service = proxy(new SlowService());

        assertThat(service.slow(30L)).isEqualTo("done");

        assertThat(appender.list)
                .anyMatch(event -> event.getFormattedMessage()
                        .contains("Slow method"));
    }

    /**
     * 验证未超过阈值时不输出慢日志。
     */
    @Test
    void shouldNotLogFastMethod() {
        SlowService service = proxy(new SlowService());

        assertThat(service.fast()).isEqualTo("done");

        assertThat(appender.list).isEmpty();
    }

    /**
     * 创建代理。
     *
     * @param target 目标对象
     * @param <T>    目标类型
     * @return 代理对象
     */
    private static <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new SlowLogAspect());
        return factory.getProxy();
    }

    /**
     * 测试服务。
     */
    public static class SlowService {

        /**
         * 慢方法。
         *
         * @param millis 休眠毫秒数
         * @return 完成标记
         */
        @SlowLog(threshold = 10L)
        public String slow(long millis) {
            sleep(millis);
            return "done";
        }

        /**
         * 快方法。
         *
         * @return 完成标记
         */
        @SlowLog(threshold = 10_000L)
        public String fast() {
            return "done";
        }

        /**
         * 休眠。
         *
         * @param millis 毫秒数
         */
        private void sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
