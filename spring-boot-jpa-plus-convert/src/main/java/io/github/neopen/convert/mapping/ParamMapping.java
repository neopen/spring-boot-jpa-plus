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
package io.github.neopen.convert.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.core.persistence.convert.ConversionContext;
import io.github.neopen.core.persistence.convert.Converters;
import io.github.neopen.core.persistence.convert.PropertyConverter;
import io.github.neopen.core.persistence.mapping.BeanIntrospector;
import io.github.neopen.core.persistence.mapping.BeanMetadata;
import io.github.neopen.core.persistence.mapping.PropertyAccessor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.metamodel.EntityType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 参数到实体的可配置映射。
 *
 * <p>在默认字段匹配之外，支持忽略字段、重命名、按字段指定转换器、
 * 空值策略、严格模式和 JPA 托管字段跳过。映射过程会返回
 * {@link MappingResult}，记录未匹配字段与发生过转换的字段。</p>
 *
 * <p>实例通常由 {@link ParamMapper#mapper(Class, Class)} 创建：</p>
 *
 * <pre>{@code
 * MappingResult<Order> result = ParamMapper.mapper(OrderParam.class, Order.class)
 *         .ignore("token")
 *         .rename("note", "remark")
 *         .nullPolicy(NullPolicy.SET_NULL)
 *         .ignoreJpaManaged(true)
 *         .map(param, order);
 * }</pre>
 *
 * @param <P> 参数类型
 * @param <E> 实体类型
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class ParamMapping<P, E> {

    /** 参数类型。 */
    private final Class<P> paramType;

    /** 实体类型。 */
    private final Class<E> entityType;

    /** 忽略的参数字段，使用规范化名称。 */
    private final Set<String> ignored = new HashSet<>();

    /** 参数字段到目标字段路径的重命名。 */
    private final Map<String, String> renames = new LinkedHashMap<>();

    /** 按参数字段指定的转换器。 */
    private final Map<String, PropertyConverter> converters =
            new LinkedHashMap<>();

    /** 空值策略。 */
    private NullPolicy nullPolicy = NullPolicy.IGNORE;

    /** 是否在存在未匹配字段时快速失败。 */
    private boolean strict;

    /** 是否跳过 JPA 托管字段。 */
    private boolean ignoreJpaManaged;

    /** 编译后的映射计划。 */
    private volatile Plan plan;

    /**
     * 创建参数映射。
     *
     * @param paramType  参数类型
     * @param entityType 实体类型
     */
    ParamMapping(Class<P> paramType, Class<E> entityType) {
        this.paramType = Objects.requireNonNull(
                paramType,
                "Parameter type must not be null"
        );
        this.entityType = Objects.requireNonNull(
                entityType,
                "Entity type must not be null"
        );
    }

    /**
     * 忽略指定参数字段。
     *
     * @param fieldNames 参数字段名
     * @return 当前映射
     */
    public synchronized ParamMapping<P, E> ignore(String... fieldNames) {
        if (fieldNames != null) {
            for (String fieldName : fieldNames) {
                if (fieldName != null && !fieldName.isBlank()) {
                    ignored.add(BeanIntrospector.normalize(fieldName));
                }
            }
        }

        invalidate();
        return this;
    }

    /**
     * 重命名参数字段对应的目标字段。
     *
     * <p>目标字段支持点号路径，例如 {@code address.city}。</p>
     *
     * @param paramField   参数字段名
     * @param targetField  目标实体字段路径
     * @return 当前映射
     */
    public synchronized ParamMapping<P, E> rename(
            String paramField,
            String targetField
    ) {
        if (paramField != null && targetField != null
                && !paramField.isBlank() && !targetField.isBlank()) {
            renames.put(
                    BeanIntrospector.normalize(paramField),
                    targetField
            );
            invalidate();
        }

        return this;
    }

    /**
     * 为指定参数字段设置转换器。
     *
     * @param paramField 参数字段名
     * @param converter  转换器
     * @return 当前映射
     */
    public synchronized ParamMapping<P, E> convert(
            String paramField,
            PropertyConverter converter
    ) {
        if (paramField != null && converter != null
                && !paramField.isBlank()) {
            converters.put(
                    BeanIntrospector.normalize(paramField),
                    converter
            );
            invalidate();
        }

        return this;
    }

    /**
     * 设置空值策略。
     *
     * @param policy 空值策略，不能为 {@code null}
     * @return 当前映射
     */
    public synchronized ParamMapping<P, E> nullPolicy(NullPolicy policy) {
        this.nullPolicy = Objects.requireNonNull(
                policy,
                "Null policy must not be null"
        );
        invalidate();
        return this;
    }

    /**
     * 设置严格模式。
     *
     * <p>开启后，参数中存在实体无法匹配的字段时直接抛出异常，
     * 适合尽早发现字段拼写错误。</p>
     *
     * @param strict 是否启用严格模式
     * @return 当前映射
     */
    public synchronized ParamMapping<P, E> strict(boolean strict) {
        this.strict = strict;
        invalidate();
        return this;
    }

    /**
     * 设置是否跳过 JPA 托管字段。
     *
     * <p>开启后主键、{@code @Version}、自动生成值与审计字段不会从
     * 参数写入，避免客户端伪造版本号或覆盖审计信息。</p>
     *
     * @param ignore 是否跳过
     * @return 当前映射
     */
    public synchronized ParamMapping<P, E> ignoreJpaManaged(
            boolean ignore
    ) {
        this.ignoreJpaManaged = ignore;
        invalidate();
        return this;
    }

    /**
     * 创建实体并映射参数。
     *
     * @param param 参数对象，允许为 {@code null}
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public E toEntity(P param) {
        return toEntity(param, () -> newInstance(entityType));
    }

    /**
     * 使用工厂创建实体并映射参数。
     *
     * @param param   参数对象，允许为 {@code null}
     * @param factory 实体工厂，不能为 {@code null}
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public E toEntity(P param, Supplier<E> factory) {
        return toEntity(param, factory, null);
    }

    /**
     * 使用工厂创建实体并映射参数。
     *
     * @param param   参数对象，允许为 {@code null}
     * @param factory 实体工厂，不能为 {@code null}
     * @param manager 实体管理器，用于解析 {@link ParamReference}，允许为 {@code null}
     * @return 新建实体；参数为空时返回 {@code null}
     */
    public E toEntity(
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

        map(param, entity, manager);

        return entity;
    }

    /**
     * 把参数映射到已有实体。
     *
     * @param param  参数对象
     * @param entity 目标实体
     * @return 映射结果
     */
    public MappingResult<E> apply(P param, E entity) {
        return map(param, entity, null);
    }

    /**
     * 把参数映射到已有实体。
     *
     * @param param   参数对象
     * @param entity  目标实体
     * @param manager 实体管理器，用于解析 {@link ParamReference}，允许为 {@code null}
     * @return 映射结果
     */
    public MappingResult<E> apply(
            P param,
            E entity,
            EntityManager manager
    ) {
        return map(param, entity, manager);
    }

    /**
     * 把参数映射到已有实体。
     *
     * @param param  参数对象
     * @param entity 目标实体
     * @return 映射结果
     */
    public MappingResult<E> map(P param, E entity) {
        return map(param, entity, null);
    }

    /**
     * 把参数映射到已有实体。
     *
     * @param param   参数对象
     * @param entity  目标实体
     * @param manager 实体管理器，用于解析 {@link ParamReference}，允许为 {@code null}
     * @return 映射结果
     */
    public MappingResult<E> map(
            P param,
            E entity,
            EntityManager manager
    ) {
        if (param == null || entity == null) {
            return new MappingResult<>(entity, List.of(), List.of());
        }

        Plan compiled = plan();
        List<String> converted = new ArrayList<>();

        for (Binding binding : compiled.bindings()) {
            Object value = binding.source().read(param);

            if (value == null) {
                if (nullPolicy == NullPolicy.SET_NULL) {
                    writeNull(binding.target(), entity);
                }
                continue;
            }

            Object mapped = mapValue(binding, value, manager, converted);
            PropertyAccessor target = binding.target().get(
                    binding.target().size() - 1
            );

            if (mapped == null && target.type().isPrimitive()) {
                continue;
            }

            write(binding.target(), entity, mapped);
        }

        return new MappingResult<>(
                entity,
                compiled.unknownFields(),
                converted
        );
    }

    /**
     * 获取编译后的映射计划。
     *
     * @return 映射计划
     */
    private Plan plan() {
        Plan current = plan;

        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (plan == null) {
                plan = compile();
            }

            return plan;
        }
    }

    /**
     * 编译映射计划。
     *
     * @return 映射计划
     * @throws JpaPlusException 严格模式下存在未匹配字段时抛出
     */
    private Plan compile() {
        BeanMetadata targets = BeanIntrospector.metadata(entityType);
        List<Binding> bindings = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        Set<String> bound = new HashSet<>();

        for (PropertyAccessor source
                : BeanIntrospector.metadata(paramType).properties()) {

            Field field = source.field();

            if (field.isAnnotationPresent(ParamIgnore.class)) {
                continue;
            }

            String normalizedSource = BeanIntrospector.normalize(
                    source.name()
            );

            if (ignored.contains(normalizedSource)
                    || !bound.add(normalizedSource)) {
                continue;
            }

            ParamReference reference =
                    field.getAnnotation(ParamReference.class);

            String targetName = renames.get(normalizedSource);

            if (targetName == null) {
                targetName = targetName(field);
            }

            if (reference != null && !reference.value().isBlank()) {
                targetName = reference.value();
            }

            List<PropertyAccessor> path = resolvePath(targets, targetName);

            if (path == null) {
                unknown.add(source.name());
                continue;
            }

            if (reference != null && path.size() != 1) {
                throw new JpaPlusException(
                        "@ParamReference target must be a direct field: "
                                + targetName
                );
            }

            PropertyAccessor target = path.get(path.size() - 1);

            if (ignoreJpaManaged && target.isJpaManaged()) {
                continue;
            }

            bindings.add(new Binding(
                    source,
                    List.copyOf(path),
                    converters.get(normalizedSource),
                    reference != null
            ));
        }

        if (strict && !unknown.isEmpty()) {
            throw new JpaPlusException(
                    "Unknown parameter fields for " + entityType.getName()
                            + ": " + String.join(", ", unknown)
            );
        }

        return new Plan(List.copyOf(bindings), List.copyOf(unknown));
    }

    /**
     * 解析目标字段路径。
     *
     * @param root       实体元数据
     * @param targetName 目标字段路径
     * @return 属性访问器路径；无法解析时返回 {@code null}
     */
    private static List<PropertyAccessor> resolvePath(
            BeanMetadata root,
            String targetName
    ) {
        if (targetName == null || targetName.isBlank()) {
            return null;
        }

        String[] parts = targetName.split("\\.");
        List<PropertyAccessor> path = new ArrayList<>(parts.length);
        BeanMetadata current = root;

        for (int i = 0; i < parts.length; i++) {
            PropertyAccessor accessor = current.property(parts[i]);

            if (accessor == null) {
                return null;
            }

            if (i < parts.length - 1
                    && (!isNestedCandidate(accessor.type())
                    || isAssociation(accessor))) {
                return null;
            }

            path.add(accessor);
            current = BeanIntrospector.metadata(accessor.type());
        }

        return path;
    }

    /**
     * 判断类型是否适合作为嵌套路径的中间节点。
     *
     * @param type 类型
     * @return 适合时返回 {@code true}
     */
    private static boolean isNestedCandidate(Class<?> type) {
        if (type.isPrimitive() || type.isEnum() || type.isArray()
                || type.isInterface()
                || Modifier.isAbstract(type.getModifiers())) {
            return false;
        }

        String name = type.getName();

        return !name.startsWith("java.")
                && !name.startsWith("javax.")
                && !name.startsWith("jakarta.")
                && !name.startsWith("org.springframework.")
                && !name.startsWith("org.hibernate.");
    }

    /**
     * 判断属性是否为 JPA 关联。
     *
     * @param accessor 属性访问器
     * @return 关联属性返回 {@code true}
     */
    private static boolean isAssociation(PropertyAccessor accessor) {
        Field field = accessor.field();

        return field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    /**
     * 获取参数字段对应的目标名称。
     *
     * @param field 参数字段
     * @return 目标名称
     */
    private static String targetName(Field field) {
        ParamField mapping = field.getAnnotation(ParamField.class);

        return mapping == null || mapping.value().isBlank()
                ? field.getName()
                : mapping.value();
    }

    /**
     * 映射单个字段值。
     *
     * @param binding   字段绑定
     * @param value     原始值
     * @param manager   实体管理器，允许为 {@code null}
     * @param converted 发生转换的字段列表
     * @return 映射后的值
     */
    private Object mapValue(
            Binding binding,
            Object value,
            EntityManager manager,
            List<String> converted
    ) {
        PropertyAccessor target = binding.target().get(
                binding.target().size() - 1
        );

        if (binding.reference()) {
            converted.add(binding.source().name());
            return resolveReference(target, value, manager);
        }

        Object result = binding.converter() == null
                ? Converters.convert(value, context(target))
                : binding.converter().convert(value, context(target));

        if (result != null
                && !result.getClass().equals(value.getClass())) {
            converted.add(binding.source().name());
        }

        return result;
    }

    /**
     * 创建属性转换上下文。
     *
     * @param accessor 属性访问器
     * @return 转换上下文
     */
    private static ConversionContext context(
            PropertyAccessor accessor
    ) {
        return new ConversionContext(
                accessor.name(),
                accessor.type(),
                accessor.genericType(),
                accessor.field()
        );
    }

    /**
     * 解析关联引用。
     *
     * @param target  关联属性
     * @param value   参数值
     * @param manager 实体管理器，允许为 {@code null}
     * @return 关联实体引用
     * @throws JpaPlusException 缺少实体管理器或无法解析引用时抛出
     */
    private static Object resolveReference(
            PropertyAccessor target,
            Object value,
            EntityManager manager
    ) {
        Class<?> associationType = target.type();

        if (associationType.isInstance(value)) {
            return value;
        }

        if (manager == null) {
            throw new JpaPlusException(
                    "EntityManager is required to map @ParamReference "
                            + "field " + target.name()
            );
        }

        Class<?> idType = idType(manager, associationType);
        Object id = idType.isInstance(value)
                ? value
                : Converters.convert(
                        value,
                        new ConversionContext(
                                target.name() + ".id",
                                idType,
                                idType,
                                null
                        )
                );

        try {
            return manager.getReference(associationType, id);
        } catch (RuntimeException ex) {
            throw new JpaPlusException(
                    "Cannot resolve reference " + associationType.getName()
                            + " with id " + id,
                    ex
            );
        }
    }

    /**
     * 获取实体主键类型。
     *
     * @param manager 实体管理器
     * @param type    实体类型
     * @return 主键类型
     * @throws JpaPlusException 类型不是 JPA 实体时抛出
     */
    private static Class<?> idType(
            EntityManager manager,
            Class<?> type
    ) {
        try {
            EntityType<?> entityType = manager.getMetamodel().entity(type);

            return entityType.getIdType().getJavaType();
        } catch (IllegalArgumentException ex) {
            throw new JpaPlusException(
                    "Cannot resolve id type for " + type.getName()
                            + ", ensure it is a JPA entity",
                    ex
            );
        }
    }

    /**
     * 写入字段路径。
     *
     * @param path   属性路径
     * @param entity 目标实体
     * @param value  字段值
     */
    private static void write(
            List<PropertyAccessor> path,
            Object entity,
            Object value
    ) {
        Object current = entity;

        for (int i = 0; i < path.size() - 1; i++) {
            PropertyAccessor accessor = path.get(i);
            Object child = accessor.read(current);

            if (child == null) {
                if (value == null) {
                    return;
                }

                child = newInstance(accessor.type());
                accessor.write(current, child);
            }

            current = child;
        }

        path.get(path.size() - 1).write(current, value);
    }

    /**
     * 写入空值。
     *
     * @param path   属性路径
     * @param entity 目标实体
     */
    private static void writeNull(
            List<PropertyAccessor> path,
            Object entity
    ) {
        Object current = entity;

        for (int i = 0; i < path.size() - 1; i++) {
            current = path.get(i).read(current);

            if (current == null) {
                return;
            }
        }

        PropertyAccessor target = path.get(path.size() - 1);

        if (!target.type().isPrimitive()) {
            target.write(current, null);
        }
    }

    /**
     * 创建实例。
     *
     * @param type 类型
     * @param <T>  类型
     * @return 实例
     * @throws JpaPlusException 无可用无参构造器时抛出
     */
    static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();

            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }

            return constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new JpaPlusException(
                    "Cannot instantiate " + type.getName()
                            + ": a no-arg constructor is required",
                    ex
            );
        }
    }

    /**
     * 清除已编译计划。
     */
    private void invalidate() {
        plan = null;
    }

    /**
     * 映射计划。
     *
     * @param bindings      字段绑定
     * @param unknownFields 未匹配字段
     */
    private record Plan(
            List<Binding> bindings,
            List<String> unknownFields
    ) {
    }

    /**
     * 字段绑定。
     *
     * @param source    参数字段
     * @param target    目标字段路径
     * @param converter 自定义转换器，允许为 {@code null}
     * @param reference 是否为关联引用
     */
    private record Binding(
            PropertyAccessor source,
            List<PropertyAccessor> target,
            PropertyConverter converter,
            boolean reference
    ) {
    }
}
