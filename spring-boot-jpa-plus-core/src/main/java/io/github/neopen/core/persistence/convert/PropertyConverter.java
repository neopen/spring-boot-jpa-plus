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
package io.github.neopen.core.persistence.convert;

/**
 * 属性值转换器。
 *
 * <p>用于在参数、实体、查询结果与视图对象之间转换字段值。实现类通过
 * {@link #supports(ConversionContext, Object)} 声明适用场景，返回
 * {@code true} 时由 {@link #convert(Object, ConversionContext)} 完成转换。</p>
 *
 * <p>自定义转换器可通过 {@link Converters#register(PropertyConverter)}
 * 全局注册，也可以在参数映射构建器中按字段指定。</p>
 *
 * <pre>{@code
 * Converters.register(new PropertyConverter() {
 *     public boolean supports(ConversionContext context, Object value) {
 *         return context.targetType() == Money.class;
 *     }
 *
 *     public Object convert(Object value, ConversionContext context) {
 *         return Money.of(value.toString());
 *     }
 * });
 * }</pre>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface PropertyConverter {

    /**
     * 转换属性值。
     *
     * @param value   原始值，不会为 {@code null}
     * @param context 转换上下文
     * @return 转换后的值
     */
    Object convert(Object value, ConversionContext context);

    /**
     * 判断是否支持当前转换。
     *
     * <p>默认支持所有转换，通常需要重写为按目标类型判断。</p>
     *
     * @param context 转换上下文
     * @param value   原始值，不会为 {@code null}
     * @return 支持时返回 {@code true}
     */
    default boolean supports(ConversionContext context, Object value) {
        return true;
    }
}
