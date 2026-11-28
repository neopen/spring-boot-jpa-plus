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
package io.github.neopen.convert.mapping;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 请求参数到实体的映射器。
 *
 * <p>用于写方向的参数转换：把 controller 的 {@code Param} 对象
 * 拷贝到新实体或已有实体上。字段按名称匹配，忽略大小写与下划线，
 * 同时支持 JPA 列名别名，例如 {@code order_no}、{@code orderNo}、
 * {@code ORDER_NO} 都能命中列名为 {@code order_no} 的字段。</p>
 *
 * <p>核心约定：</p>
 *
 * <ul>
 *     <li>默认只拷贝非 {@code null} 的参数值，天然支持部分更新。</li>
 *     <li>优先调用实体的公开 setter，其次直接写入字段。</li>
 *     <li>参数字段可用 {@link ParamField} 指定目标字段名或点号路径，
 *         用 {@link ParamIgnore} 排除，用 {@link ParamReference}
 *         把外键 ID 转为关联实体引用。</li>
 *     <li>常见数值、字符串、布尔、字符、枚举、时间、集合和 JPA
 *         {@code @Convert} 类型自动转换。</li>
 * </ul>
 *
 * <pre>{@code
 * OrderParam param = new OrderParam();
 * param.setNumber("NO-1");
 *
 * Order order = ParamMapper.toEntity(param, Order.class);
 *
 * // 更新场景：只覆盖参数中非空的字段
 * ParamMapper.apply(param, existingOrder);
 *
 * // 需要更多控制时使用构建器
 * ParamMapper.mapper(OrderParam.class, Order.class)
 *         .ignoreJpaManaged(true)
 *         .nullPolicy(NullPolicy.SET_NULL)
 *         .map(param, existingOrder);
 * }</pre>
 *
 * @param <P> 参数类型
 * @param <E> 实体类型
 *
 * @author neopen
 * @see ParamMapping
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class ParamMapper<P, E> {

    /** 默认映射器缓存。 */
    private static final ClassValue<
            Map<Class<?>, ParamMapping<Object, Object>>> CACHE =
            new ClassValue<>() {

                /**
                 * 创建按实体类型索引的映射器缓存。
                 *
                 * @param type 参数类型
                 * @return 映射器缓存
                 */
                @Override
                protected Map<Class<?>, ParamMapping<Object, Object>>
                computeValue(Class<?> type) {
                    return new ConcurrentHashMap<>();
                }
            };

    /** 工具类不允许实例化。 */
    private ParamMapper() {
    }

    /**
     * 创建可配置的参数映射。
     *
     * @param paramType  参数类型
     * @param entityType 实体类型
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 参数映射
     */
    public static <P, E> ParamMapping<P, E> mapper(
            Class<P> paramType,
            Class<E> entityType
    ) {
        return new ParamMapping<>(paramType, entityType);
    }

    /**
     * 创建实体实例并拷贝参数中的非空字段。
     *
     * @param param      参数对象，允许为 {@code null}
     * @param entityType 实体类型，必须提供无参构造器
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public static <P, E> E toEntity(P param, Class<E> entityType) {
        return toEntity(param, entityType, null);
    }

    /**
     * 创建实体实例并拷贝参数中的非空字段。
     *
     * @param param      参数对象，允许为 {@code null}
     * @param entityType 实体类型，必须提供无参构造器
     * @param manager    实体管理器，用于解析 {@link ParamReference}，允许为 {@code null}
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public static <P, E> E toEntity(
            P param,
            Class<E> entityType,
            EntityManager manager
    ) {
        if (param == null) {
            return null;
        }

        Objects.requireNonNull(
                entityType,
                "Entity type must not be null"
        );

        E entity = ParamMapping.newInstance(entityType);

        defaultMapping(param.getClass(), entityType)
                .apply(param, entity, manager);

        return entity;
    }

    /**
     * 使用工厂创建实体并拷贝参数中的非空字段。
     *
     * @param param   参数对象，允许为 {@code null}
     * @param factory 实体工厂，不能为 {@code null}
     * @param <P>     参数类型
     * @param <E>     实体类型
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public static <P, E> E toEntity(P param, Supplier<E> factory) {
        return toEntity(param, factory, null);
    }

    /**
     * 使用工厂创建实体并拷贝参数中的非空字段。
     *
     * @param param   参数对象，允许为 {@code null}
     * @param factory 实体工厂，不能为 {@code null}
     * @param manager 实体管理器，用于解析 {@link ParamReference}，允许为 {@code null}
     * @param <P>     参数类型
     * @param <E>     实体类型
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public static <P, E> E toEntity(
            P param,
            Supplier<E> factory,
            EntityManager manager
    ) {
        if (param == null) {
            return null;
        }

        Objects.requireNonNull(
                factory,
                "Entity factory must not be null"
        );

        E entity = factory.get();

        if (entity == null) {
            throw new IllegalArgumentException(
                    "Entity factory returned null"
            );
        }

        defaultMapping(param.getClass(), entity.getClass())
                .apply(param, entity, manager);

        return entity;
    }

    /**
     * 把参数中的非空字段覆盖到已有实体。
     *
     * <p>参数或实体为空时直接返回，不做任何处理。</p>
     *
     * @param param  参数对象
     * @param entity 目标实体
     * @param <P>    参数类型
     * @param <E>    实体类型
     */
    public static <P, E> void apply(P param, E entity) {
        apply(param, entity, null);
    }

    /**
     * 把参数中的非空字段覆盖到已有实体。
     *
     * @param param   参数对象
     * @param entity  目标实体
     * @param manager 实体管理器，用于解析 {@link ParamReference}，允许为 {@code null}
     * @param <P>     参数类型
     * @param <E>     实体类型
     */
    public static <P, E> void apply(
            P param,
            E entity,
            EntityManager manager
    ) {
        if (param == null || entity == null) {
            return;
        }

        defaultMapping(param.getClass(), entity.getClass())
                .apply(param, entity, manager);
    }

    /**
     * 批量转换参数为实体。
     *
     * @param params     参数集合，允许为 {@code null} 或空集合
     * @param entityType 实体类型
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 实体列表；空参数元素会被跳过
     */
    public static <P, E> List<E> toEntities(
            Collection<P> params,
            Class<E> entityType
    ) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }

        List<E> result = new ArrayList<>(params.size());

        for (P param : params) {
            if (param != null) {
                result.add(toEntity(param, entityType));
            }
        }

        return result;
    }

    /**
     * 批量转换参数为实体。
     *
     * @param params  参数集合，允许为 {@code null} 或空集合
     * @param factory 实体工厂
     * @param <P>     参数类型
     * @param <E>     实体类型
     * @return 实体列表；空参数元素会被跳过
     */
    public static <P, E> List<E> toEntities(
            Collection<P> params,
            Supplier<E> factory
    ) {
        if (params == null || params.isEmpty()) {
            return List.of();
        }

        List<E> result = new ArrayList<>(params.size());

        for (P param : params) {
            if (param != null) {
                result.add(toEntity(param, factory));
            }
        }

        return result;
    }

    /**
     * 获取默认映射器。
     *
     * @param paramType  参数类型
     * @param entityType 实体类型
     * @param <P>        参数类型
     * @param <E>        实体类型
     * @return 默认映射器
     */
    @SuppressWarnings("unchecked")
    private static <P, E> ParamMapping<P, E> defaultMapping(
            Class<?> paramType,
            Class<?> entityType
    ) {
        return (ParamMapping<P, E>) CACHE.get(paramType)
                .computeIfAbsent(
                        entityType,
                        type -> new ParamMapping<>(
                                (Class<Object>) paramType,
                                (Class<Object>) type
                        )
                );
    }

}
