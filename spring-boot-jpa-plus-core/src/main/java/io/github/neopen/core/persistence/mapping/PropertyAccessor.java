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
package io.github.neopen.core.persistence.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.core.persistence.audit.CreatedAt;
import io.github.neopen.core.persistence.audit.CreatedBy;
import io.github.neopen.core.persistence.audit.UpdatedAt;
import io.github.neopen.core.persistence.audit.UpdatedBy;
import io.github.neopen.core.persistence.logicdelete.LogicDelete;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 属性访问器。
 *
 * <p>统一封装字段读取与写入。优先使用 {@link MethodHandle} 与
 * {@link VarHandle}，无法建立句柄时回退到传统反射，兼顾性能与兼容性。</p>
 *
 * <p>访问器由 {@link BeanIntrospector} 统一创建并缓存，业务代码无需
 * 直接实例化。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class PropertyAccessor {

    /** 属性名。 */
    private final String name;

    /** 属性类型。 */
    private final Class<?> type;

    /** 属性泛型类型。 */
    private final Type genericType;

    /** 属性字段。 */
    private final Field field;

    /** setter 方法，允许为 {@code null}。 */
    private final Method setter;

    /** 字段读取句柄，允许为 {@code null}。 */
    private final MethodHandle getterHandle;

    /** setter 调用句柄，允许为 {@code null}。 */
    private final MethodHandle setterHandle;

    /** 字段访问句柄，允许为 {@code null}。 */
    private final VarHandle varHandle;

    /**
     * 创建属性访问器。
     *
     * @param name          属性名
     * @param type          属性类型
     * @param genericType   属性泛型类型
     * @param field         属性字段
     * @param setter        setter 方法，允许为 {@code null}
     * @param getterHandle  字段读取句柄，允许为 {@code null}
     * @param setterHandle  setter 调用句柄，允许为 {@code null}
     * @param varHandle     字段访问句柄，允许为 {@code null}
     */
    private PropertyAccessor(
            String name,
            Class<?> type,
            Type genericType,
            Field field,
            Method setter,
            MethodHandle getterHandle,
            MethodHandle setterHandle,
            VarHandle varHandle
    ) {
        this.name = name;
        this.type = type;
        this.genericType = genericType;
        this.field = field;
        this.setter = setter;
        this.getterHandle = getterHandle;
        this.setterHandle = setterHandle;
        this.varHandle = varHandle;
    }

    /**
     * 创建属性访问器。
     *
     * @param field 属性字段
     * @return 属性访问器
     */
    static PropertyAccessor create(Field field) {
        field.trySetAccessible();

        Method setter = findSetter(field);

        return new PropertyAccessor(
                field.getName(),
                field.getType(),
                field.getGenericType(),
                field,
                setter,
                createGetterHandle(field),
                createSetterHandle(setter),
                createVarHandle(field)
        );
    }

    /**
     * 获取属性名。
     *
     * @return 属性名
     */
    public String name() {
        return name;
    }

    /**
     * 获取属性类型。
     *
     * @return 属性类型
     */
    public Class<?> type() {
        return type;
    }

    /**
     * 获取属性泛型类型。
     *
     * @return 属性泛型类型
     */
    public Type genericType() {
        return genericType;
    }

    /**
     * 获取属性字段。
     *
     * @return 属性字段
     */
    public Field field() {
        return field;
    }

    /**
     * 读取属性值。
     *
     * @param target 目标对象
     * @return 属性值
     * @throws JpaPlusException 读取失败时抛出
     */
    public Object read(Object target) {
        try {
            if (getterHandle != null) {
                return getterHandle.invokeExact(target);
            }

            if (varHandle != null) {
                return varHandle.get(target);
            }

            return field.get(target);
        } catch (Throwable ex) {
            throw failure("read", ex);
        }
    }

    /**
     * 写入属性值。
     *
     * @param target 目标对象
     * @param value  属性值
     * @throws JpaPlusException 写入失败时抛出
     */
    public void write(Object target, Object value) {
        try {
            if (setterHandle != null) {
                setterHandle.invokeExact(target, value);
                return;
            }

            if (varHandle != null) {
                varHandle.set(target, value);
                return;
            }

            if (setter != null) {
                setter.invoke(target, value);
                return;
            }

            field.set(target, value);
        } catch (Throwable ex) {
            throw failure("write", ex);
        }
    }

    /**
     * 判断属性是否由 JPA 托管。
     *
     * <p>包括主键、乐观锁版本、自动生成值以及审计字段。参数映射在
     * 启用 {@code ignoreJpaManaged} 时会跳过这些属性。</p>
     *
     * @return JPA 托管属性返回 {@code true}
     */
    public boolean isJpaManaged() {
        return hasAnnotation(Id.class)
                || hasAnnotation(EmbeddedId.class)
                || hasAnnotation(Version.class)
                || hasAnnotation(GeneratedValue.class)
                || hasAnnotation(CreatedDate.class)
                || hasAnnotation(LastModifiedDate.class)
                || hasAnnotation(
                        org.springframework.data.annotation.CreatedBy.class
                )
                || hasAnnotation(
                        org.springframework.data.annotation.LastModifiedBy.class
                )
                || hasAnnotation(CreatedAt.class)
                || hasAnnotation(UpdatedAt.class)
                || hasAnnotation(CreatedBy.class)
                || hasAnnotation(UpdatedBy.class)
                || hasAnnotation(LogicDelete.class);
    }

    /**
     * 获取属性对应的列名别名。
     *
     * <p>读取字段或 setter 上的 {@link Column}、{@link JoinColumn}
     * 注解，使参数中的 {@code order_no} 可以匹配到列名为
     * {@code order_no} 的实体属性。</p>
     *
     * @return 列名别名列表，不含空白值
     */
    public List<String> columnAliases() {
        List<String> aliases = new ArrayList<>(2);

        addAlias(aliases, field.getAnnotation(Column.class));
        addAlias(aliases, field.getAnnotation(JoinColumn.class));

        if (setter != null) {
            addAlias(aliases, setter.getAnnotation(Column.class));
            addAlias(aliases, setter.getAnnotation(JoinColumn.class));
        }

        return aliases;
    }

    /**
     * 判断字段或 setter 是否声明指定注解。
     *
     * @param annotationType 注解类型
     * @return 存在时返回 {@code true}
     */
    private boolean hasAnnotation(
            Class<? extends Annotation> annotationType
    ) {
        return field.isAnnotationPresent(annotationType)
                || (setter != null
                && setter.isAnnotationPresent(annotationType));
    }

    /**
     * 添加列名别名。
     *
     * @param aliases 别名列表
     * @param column  列注解，允许为 {@code null}
     */
    private static void addAlias(
            List<String> aliases,
            Column column
    ) {
        if (column != null && !column.name().isBlank()) {
            aliases.add(column.name());
        }
    }

    /**
     * 添加关联列名别名。
     *
     * @param aliases    别名列表
     * @param joinColumn 关联列注解，允许为 {@code null}
     */
    private static void addAlias(
            List<String> aliases,
            JoinColumn joinColumn
    ) {
        if (joinColumn != null && !joinColumn.name().isBlank()) {
            aliases.add(joinColumn.name());
        }
    }

    /**
     * 创建字段读取句柄。
     *
     * @param field 字段
     * @return 读取句柄；无法创建时返回 {@code null}
     */
    private static MethodHandle createGetterHandle(Field field) {
        try {
            return lookup(field)
                    .unreflectGetter(field)
                    .asType(MethodType.methodType(
                            Object.class,
                            Object.class
                    ));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * 创建 setter 调用句柄。
     *
     * @param setter setter 方法，允许为 {@code null}
     * @return 调用句柄；无法创建时返回 {@code null}
     */
    private static MethodHandle createSetterHandle(Method setter) {
        if (setter == null) {
            return null;
        }

        try {
            return MethodHandles.privateLookupIn(
                            setter.getDeclaringClass(),
                            MethodHandles.lookup()
                    )
                    .unreflect(setter)
                    .asType(MethodType.methodType(
                            void.class,
                            Object.class,
                            Object.class
                    ));
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * 创建字段访问句柄。
     *
     * @param field 字段
     * @return 字段访问句柄；无法创建时返回 {@code null}
     */
    private static VarHandle createVarHandle(Field field) {
        try {
            return lookup(field).unreflectVarHandle(field);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    /**
     * 获取字段所属类的私有访问 Lookup。
     *
     * @param field 字段
     * @return Lookup
     * @throws IllegalAccessException 模块未开放时抛出
     */
    private static MethodHandles.Lookup lookup(Field field)
            throws IllegalAccessException {
        return MethodHandles.privateLookupIn(
                field.getDeclaringClass(),
                MethodHandles.lookup()
        );
    }

    /**
     * 查找 setter 方法。
     *
     * @param field 字段
     * @return setter 方法；不存在时返回 {@code null}
     */
    private static Method findSetter(Field field) {
        String name = "set" + Character.toUpperCase(
                field.getName().charAt(0)
        ) + field.getName().substring(1);

        Method exact = findMethod(field, name, field.getType());

        if (exact != null) {
            return exact;
        }

        for (Class<?> current = field.getDeclaringClass();
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isSynthetic()
                        || Modifier.isStatic(method.getModifiers())
                        || method.getParameterCount() != 1
                        || !method.getName().equals(name)) {
                    continue;
                }

                if (method.getParameterTypes()[0]
                        .isAssignableFrom(field.getType())) {
                    method.trySetAccessible();
                    return method;
                }
            }
        }

        return null;
    }

    /**
     * 按名称与参数类型查找方法。
     *
     * @param field      字段
     * @param name       方法名
     * @param paramType  参数类型
     * @return 方法；不存在时返回 {@code null}
     */
    private static Method findMethod(
            Field field,
            String name,
            Class<?> paramType
    ) {
        for (Class<?> current = field.getDeclaringClass();
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, paramType);

                if (!Modifier.isStatic(method.getModifiers())) {
                    method.trySetAccessible();
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // 继续沿父类查找
            }
        }

        return null;
    }

    /**
     * 创建访问失败异常。
     *
     * @param action 操作名称
     * @param cause  原始异常
     * @return 异常
     */
    private JpaPlusException failure(String action, Throwable cause) {
        if (cause instanceof JpaPlusException exception) {
            return exception;
        }

        if (cause instanceof Error error) {
            throw error;
        }

        return new JpaPlusException(
                "Cannot " + action + " property " + name
                        + " of " + field.getDeclaringClass().getName(),
                cause
        );
    }
}
