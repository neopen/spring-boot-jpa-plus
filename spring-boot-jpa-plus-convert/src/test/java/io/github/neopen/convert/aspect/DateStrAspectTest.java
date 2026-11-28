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
package io.github.neopen.convert.aspect;

import io.github.neopen.convert.annotation.DateStrFormat;
import io.github.neopen.convert.annotation.DateStrValid;
import io.github.neopen.core.exception.JpaPlusException;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DateStrAspect} 单元测试。
 */
class DateStrAspectTest {

    /** 被测切面。 */
    private final DateStrAspect aspect = new DateStrAspect();

    /**
     * 验证合法日期通过校验。
     */
    @Test
    void shouldAcceptValidDate() {
        assertThatCode(() -> aspect.validate(
                new SampleBean("2026-02-28"),
                visited(),
                0
        )).doesNotThrowAnyException();
    }

    /**
     * 验证非法日期被拒绝。
     */
    @Test
    void shouldRejectInvalidDate() {
        assertThatThrownBy(() -> aspect.validate(
                new SampleBean("2026-02-30"),
                visited(),
                0
        )).isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证必填日期为空时失败。
     */
    @Test
    void shouldRejectMissingRequiredDate() {
        assertThatThrownBy(() -> aspect.validate(
                new SampleBean(null),
                visited(),
                0
        )).isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证嵌套对象中的日期也会被校验。
     */
    @Test
    void shouldValidateNestedObject() {
        SampleBean bean = new SampleBean("2026-02-28");
        bean.nested = new NestedBean("2026-02-30");

        assertThatThrownBy(() -> aspect.validate(
                bean,
                visited(),
                0
        )).isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证方法级注解可以触发切面。
     */
    @Test
    void shouldApplyAspectOnAnnotatedMethod() {
        SampleService proxy = proxy();

        assertThatThrownBy(() -> proxy.save(new SampleBean("2026-02-30")))
                .isInstanceOf(JpaPlusException.class);

        assertThatCode(() -> proxy.save(new SampleBean("2026-02-28")))
                .doesNotThrowAnyException();
    }

    /**
     * 验证参数级注解可以触发切面。
     */
    @Test
    void shouldApplyAspectOnAnnotatedParameter() {
        SampleService proxy = proxy();

        assertThatThrownBy(
                () -> proxy.saveParameter(new SampleBean("2026-02-30"))
        ).isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证未标注注解的方法不会被校验。
     */
    @Test
    void shouldIgnoreMethodWithoutAnnotation() {
        SampleService proxy = proxy();

        assertThatCode(() -> proxy.ignore(new SampleBean("2026-02-30")))
                .doesNotThrowAnyException();
    }

    /**
     * 创建代理。
     *
     * @return 代理对象
     */
    private SampleService proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(
                new SampleService()
        );
        factory.setProxyTargetClass(true);
        factory.addAspect(aspect);

        return factory.getProxy();
    }

    /**
     * 创建访问集合。
     *
     * @return 访问集合
     */
    private static Set<Object> visited() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /** 示例服务。 */
    public static class SampleService {

        /**
         * 保存参数。
         *
         * @param bean 参数对象
         */
        @DateStrValid
        public void save(SampleBean bean) {
        }

        /**
         * 保存参数，注解在参数上。
         *
         * @param bean 参数对象
         */
        public void saveParameter(@DateStrValid SampleBean bean) {
        }

        /**
         * 忽略校验。
         *
         * @param bean 参数对象
         */
        public void ignore(SampleBean bean) {
        }
    }

    /** 示例参数。 */
    public static class SampleBean {

        /** 日期。 */
        @DateStrFormat(value = "yyyy-MM-dd", required = true)
        private String date;

        /** 嵌套对象。 */
        private NestedBean nested;

        /**
         * 创建示例参数。
         *
         * @param date 日期
         */
        public SampleBean(String date) {
            this.date = date;
        }
    }

    /** 嵌套参数。 */
    public static class NestedBean {

        /** 日期。 */
        @DateStrFormat(value = "yyyy-MM-dd")
        private String date;

        /**
         * 创建嵌套参数。
         *
         * @param date 日期
         */
        public NestedBean(String date) {
            this.date = date;
        }
    }
}
