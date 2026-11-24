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

import io.github.neopen.core.exception.JpaPlusException;
import jakarta.persistence.AttributeConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 基于 {@link EnumValue} 的枚举转换器基类。
 *
 * <p>子类只需声明具体枚举类型，即可把枚举常量与
 * {@link EnumValue} 声明的数据库值相互转换：</p>
 *
 * <pre>{@code
 * public class OrderStatusConverter
 *         extends EnumValueConverter<OrderStatus> {
 * }
 *
 * @Convert(converter = OrderStatusConverter.class)
 * private OrderStatus status;
 * }</pre>
 *
 * <p>转换器会通过反射解析子类声明的泛型参数，无法解析时在实例化阶段
 * 抛出 {@link JpaPlusException}，避免运行期出现难以定位的错误。</p>
 *
 * @param <E> 枚举类型
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public abstract class EnumValueConverter<E extends Enum<E>>
        implements AttributeConverter<E, String> {

    /** 具体枚举类型。 */
    private final Class<E> enumType;

    /**
     * 创建转换器并解析泛型参数。
     *
     * @throws JpaPlusException 无法解析枚举类型时抛出
     */
    protected EnumValueConverter() {
        this.enumType = resolveEnumType();
    }

    /**
     * 获取具体枚举类型。
     *
     * @return 枚举类型
     */
    public Class<E> getEnumType() {
        return enumType;
    }

    /**
     * 将枚举常量转换为数据库存储值。
     *
     * @param attribute 枚举常量，允许为 {@code null}
     * @return 数据库存储值
     */
    @Override
    public String convertToDatabaseColumn(E attribute) {
        return EnumValues.toValue(attribute);
    }

    /**
     * 将数据库存储值还原为枚举常量。
     *
     * @param dbData 数据库存储值，允许为 {@code null}
     * @return 枚举常量
     */
    @Override
    public E convertToEntityAttribute(String dbData) {
        return EnumValues.fromValue(enumType, dbData);
    }

    /**
     * 解析子类声明的枚举泛型参数。
     *
     * @return 枚举类型
     * @throws JpaPlusException 无法解析时抛出
     */
    @SuppressWarnings("unchecked")
    private Class<E> resolveEnumType() {
        Type type = getClass().getGenericSuperclass();

        while (type != null) {
            if (type instanceof ParameterizedType parameterized) {
                Type[] arguments = parameterized.getActualTypeArguments();

                if (arguments.length == 1
                        && arguments[0] instanceof Class<?> clazz
                        && Enum.class.isAssignableFrom(clazz)) {
                    return (Class<E>) clazz;
                }

                throw new JpaPlusException(
                        "Cannot resolve enum type from "
                                + getClass().getName()
                );
            }

            if (!(type instanceof Class<?> clazz)
                    || clazz == Object.class) {
                break;
            }

            type = clazz.getGenericSuperclass();
        }

        throw new JpaPlusException(
                "Cannot resolve enum type from " + getClass().getName()
                        + ", declare it as EnumValueConverter<MyEnum>"
        );
    }
}
