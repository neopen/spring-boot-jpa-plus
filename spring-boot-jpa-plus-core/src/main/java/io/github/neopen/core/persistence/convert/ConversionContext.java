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
package io.github.neopen.core.persistence.convert;

import org.springframework.lang.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 属性转换上下文。
 *
 * <p>描述目标属性名、目标类型以及字段上声明的注解，转换器可据此读取
 * {@code @DateTimeFormat}、{@code @Convert} 等元数据。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class ConversionContext {

    /** 属性名。 */
    private final String propertyName;

    /** 目标类型。 */
    private final Class<?> targetType;

    /** 目标泛型类型。 */
    private final Type genericType;

    /** 目标字段或方法，允许为 {@code null}。 */
    private final AnnotatedElement annotatedElement;

    /**
     * 创建转换上下文。
     *
     * @param propertyName     属性名
     * @param targetType       目标类型
     * @param genericType      目标泛型类型，允许为 {@code null}
     * @param annotatedElement 目标字段或方法，允许为 {@code null}
     */
    public ConversionContext(
            String propertyName,
            Class<?> targetType,
            @Nullable Type genericType,
            @Nullable AnnotatedElement annotatedElement
    ) {
        this.propertyName = Objects.requireNonNull(
                propertyName,
                "Property name must not be null"
        );
        this.targetType = Objects.requireNonNull(
                targetType,
                "Target type must not be null"
        );
        this.genericType = genericType == null ? targetType : genericType;
        this.annotatedElement = annotatedElement;
    }

    /**
     * 获取属性名。
     *
     * @return 属性名
     */
    public String propertyName() {
        return propertyName;
    }

    /**
     * 获取目标类型。
     *
     * @return 目标类型
     */
    public Class<?> targetType() {
        return targetType;
    }

    /**
     * 获取目标泛型类型。
     *
     * @return 目标泛型类型
     */
    public Type genericType() {
        return genericType;
    }

    /**
     * 获取目标字段或方法。
     *
     * @return 目标字段或方法；不存在时返回 {@code null}
     */
    @Nullable
    public AnnotatedElement annotatedElement() {
        return annotatedElement;
    }

    /**
     * 创建元素级转换上下文。
     *
     * <p>用于集合或数组元素转换，复用当前属性的注解与名称。</p>
     *
     * @param type        元素类型
     * @param generic     元素泛型类型，允许为 {@code null}
     * @return 元素级转换上下文
     */
    public ConversionContext elementContext(
            Class<?> type,
            @Nullable Type generic
    ) {
        return new ConversionContext(
                propertyName + "[]",
                type,
                generic,
                annotatedElement
        );
    }
}
