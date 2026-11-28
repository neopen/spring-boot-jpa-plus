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
package io.github.neopen.core.persistence.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.core.persistence.convert.ConversionContext;
import io.github.neopen.core.persistence.convert.Converters;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 查询结果到 DTO 的映射器。
 *
 * <p>支持将 {@link Tuple}、{@code Object[]} 或单列标量结果映射为普通
 * DTO。列名与 DTO 属性匹配时会忽略大小写、下划线和驼峰差异，
 * 例如 {@code order_no}、{@code orderNo}、{@code ORDER_NO} 都能
 * 命中 {@code orderNo} 字段。</p>
 *
 * <p>映射优先使用 setter，其次直接写入字段；常见数值、字符串、
 * 布尔、枚举、时间、集合和 JPA {@code @Convert} 转换由
 * {@link Converters} 统一处理。</p>
 *
 * @param <T> DTO 类型
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class ResultMapper<T> {

    /** DTO 类型。 */
    private final Class<T> targetType;

    /** DTO 属性元数据。 */
    private final BeanMetadata metadata;

    /** 按声明顺序排列的属性访问器。 */
    private final List<PropertyAccessor> orderedAccessors;

    /**
     * 创建结果映射器。
     *
     * @param targetType DTO 类型
     */
    private ResultMapper(Class<T> targetType) {
        this.targetType = targetType;
        this.metadata = BeanIntrospector.metadata(targetType);
        this.orderedAccessors = metadata.properties();
    }

    /**
     * 创建指定 DTO 类型的结果映射器。
     *
     * @param targetType DTO 类型，不能为 {@code null}
     * @param <T>        DTO 类型
     * @return 结果映射器
     * @throws IllegalArgumentException DTO 类型为空或无可用属性时抛出
     */
    public static <T> ResultMapper<T> of(Class<T> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException(
                    "Target type must not be null"
            );
        }

        if (targetType.isInterface()
                || targetType.isPrimitive()
                || targetType.isArray()) {
            throw new IllegalArgumentException(
                    "Unsupported target type: " + targetType.getName()
            );
        }

        ResultMapper<T> mapper = new ResultMapper<>(targetType);

        if (mapper.orderedAccessors.isEmpty()) {
            throw new IllegalArgumentException(
                    "DTO " + targetType.getName()
                            + " declares no writable field"
            );
        }

        return mapper;
    }

    /**
     * 映射单行结果。
     *
     * @param row 查询结果行，允许为 {@code null}
     * @return 映射后的 DTO；结果行为空时返回 {@code null}
     */
    public T map(Object row) {
        if (row == null) {
            return null;
        }

        if (targetType.isInstance(row)) {
            return targetType.cast(row);
        }

        T target = newInstance();

        if (row instanceof Tuple tuple) {
            mapTuple(tuple, target);
            return target;
        }

        if (row instanceof Object[] values) {
            return map(values, null);
        }

        if (orderedAccessors.size() == 1) {
            write(orderedAccessors.get(0), target, row);
            return target;
        }

        throw new JpaPlusException(
                "Cannot map scalar result to " + targetType.getName()
                        + ": the DTO must declare exactly one field"
        );
    }

    /**
     * 按列名映射一行数组结果。
     *
     * <p>列名为空时按 DTO 属性声明顺序映射；此时列数与属性数必须一致。</p>
     *
     * @param values  列值数组
     * @param aliases 列名数组，允许为 {@code null}
     * @return 映射后的 DTO
     * @throws JpaPlusException 列数与属性数不一致时抛出
     */
    public T map(Object[] values, String[] aliases) {
        if (values == null) {
            return null;
        }

        T target = newInstance();

        if (aliases != null && aliases.length == values.length) {
            for (int i = 0; i < values.length; i++) {
                PropertyAccessor accessor = find(aliases[i]);

                if (accessor != null) {
                    write(accessor, target, values[i]);
                }
            }

            return target;
        }

        if (values.length != orderedAccessors.size()) {
            throw new JpaPlusException(
                    "Cannot map result without aliases to "
                            + targetType.getName() + ": expected "
                            + orderedAccessors.size() + " columns but got "
                            + values.length
            );
        }

        for (int i = 0; i < values.length; i++) {
            write(orderedAccessors.get(i), target, values[i]);
        }

        return target;
    }

    /**
     * 映射多行结果。
     *
     * @param rows 查询结果行
     * @return 映射后的 DTO 列表
     */
    public List<T> mapAll(Collection<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<T> result = new ArrayList<>(rows.size());

        for (Object row : rows) {
            result.add(map(row));
        }

        return result;
    }

    /**
     * 映射单行结果。
     *
     * @param row        查询结果行
     * @param targetType DTO 类型
     * @param <T>        DTO 类型
     * @return 映射后的 DTO
     */
    public static <T> T mapRow(Object row, Class<T> targetType) {
        return of(targetType).map(row);
    }

    /**
     * 按列名映射一行数组结果。
     *
     * @param values     列值数组
     * @param aliases    列名数组，允许为 {@code null}
     * @param targetType DTO 类型
     * @param <T>        DTO 类型
     * @return 映射后的 DTO
     */
    public static <T> T mapRow(
            Object[] values,
            String[] aliases,
            Class<T> targetType
    ) {
        return of(targetType).map(values, aliases);
    }

    /**
     * 映射多行结果。
     *
     * @param rows       查询结果行
     * @param targetType DTO 类型
     * @param <T>        DTO 类型
     * @return 映射后的 DTO 列表
     */
    public static <T> List<T> mapRows(
            Collection<?> rows,
            Class<T> targetType
    ) {
        return of(targetType).mapAll(rows);
    }

    /**
     * 映射 Tuple 结果。
     *
     * @param tuple  查询结果
     * @param target 目标对象
     */
    private void mapTuple(Tuple tuple, T target) {
        for (TupleElement<?> element : tuple.getElements()) {
            String alias = element.getAlias();

            if (alias == null || alias.isBlank()) {
                continue;
            }

            PropertyAccessor accessor = find(alias);

            if (accessor != null) {
                write(accessor, target, tuple.get(element));
            }
        }
    }

    /**
     * 创建 DTO 实例。
     *
     * @return DTO 实例
     * @throws JpaPlusException 无可用无参构造器时抛出
     */
    private T newInstance() {
        try {
            Constructor<T> constructor =
                    targetType.getDeclaredConstructor();

            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }

            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new JpaPlusException(
                    "Cannot instantiate DTO " + targetType.getName()
                            + ": a no-arg constructor is required",
                    e
            );
        }
    }

    /**
     * 按列名查找属性访问器。
     *
     * @param alias 列名
     * @return 属性访问器；未匹配时返回 {@code null}
     */
    private PropertyAccessor find(String alias) {
        return alias == null ? null : metadata.property(alias);
    }

    /**
     * 写入属性值。
     *
     * @param accessor 属性访问器
     * @param target   目标对象
     * @param value    原始值
     */
    private void write(
            PropertyAccessor accessor,
            T target,
            Object value
    ) {
        Object converted = Converters.convert(
                value,
                new ConversionContext(
                        accessor.name(),
                        accessor.type(),
                        accessor.genericType(),
                        accessor.field()
                )
        );

        if (converted == null && accessor.type().isPrimitive()) {
            return;
        }

        accessor.write(target, converted);
    }
}
