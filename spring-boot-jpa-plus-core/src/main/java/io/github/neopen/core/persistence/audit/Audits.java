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
package io.github.neopen.core.persistence.audit;

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.core.scope.JpaPlusScope;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 审计字段填充工具。
 *
 * <p>配合 {@link AuditListener} 使用：在实体上声明
 * {@code @EntityListeners(AuditListener.class)}，插入和更新时自动
 * 维护时间与操作人字段。</p>
 *
 * <pre>{@code
 * @Entity
 * @EntityListeners(AuditListener.class)
 * public class Order {
 *
 *     @CreatedAt
 *     private Instant createdAt;
 *
 *     @UpdatedAt
 *     private Instant updatedAt;
 *
 *     @CreatedBy
 *     private String createdBy;
 *
 *     @UpdatedBy
 *     private String updatedBy;
 * }
 * }</pre>
 *
 * <p>默认使用 UTC 系统时钟；可通过 {@link #setClock(Clock)} 指定
 * 时区，通过 {@link #setAuditorSupplier(Supplier)} 提供当前操作人。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class Audits {

    /** 当前时钟。 */
    private static volatile Clock clock = Clock.systemUTC();

    /** 当前操作人提供者。 */
    private static volatile Supplier<String> auditorSupplier = () -> null;

    /** 是否启用审计字段填充，默认启用。 */
    private static volatile boolean enabled = true;

    /** 按数据源作用域配置的审计开关。 */
    private static final Map<String, Boolean> scopeEnabled =
            new ConcurrentHashMap<>();

    /** 按数据源作用域配置的操作人提供者。 */
    private static final Map<String, Supplier<String>> scopeAuditors =
            new ConcurrentHashMap<>();

    /** 工具类不允许实例化。 */
    private Audits() {
    }

    /**
     * 全局启用或关闭审计字段填充。
     *
     * <p>关闭后 {@link AuditListener} 仍然会被调用，但不再写入
     * 时间与操作人字段，适用于数据迁移等不希望改动审计信息的场景。</p>
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        Audits.enabled = enabled;
    }

    /**
     * 为指定数据源作用域设置审计开关。
     *
     * @param scope   作用域名称，等于默认作用域时等同于全局设置
     * @param enabled 是否启用
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setEnabled(String scope, boolean enabled) {
        if (JpaPlusScope.isDefault(scope)) {
            setEnabled(enabled);
            return;
        }

        scopeEnabled.put(JpaPlusScope.requireName(scope), enabled);
    }

    /**
     * 判断全局审计字段填充是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断指定作用域的审计字段填充是否启用。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled(String scope) {
        return activeEnabled(scope);
    }

    /**
     * 设置审计时钟。
     *
     * @param clock 时钟；传 {@code null} 时恢复为 UTC 系统时钟
     */
    public static void setClock(Clock clock) {
        Audits.clock = clock == null
                ? Clock.systemUTC()
                : clock;
    }

    /**
     * 设置当前操作人提供者。
     *
     * @param supplier 操作人提供者；传 {@code null} 时不写入操作人字段
     */
    public static void setAuditorSupplier(Supplier<String> supplier) {
        Audits.auditorSupplier = supplier == null
                ? () -> null
                : supplier;
    }

    /**
     * 设置当前操作人提供者。
     *
     * <p>Spring Boot 环境通常注册 {@link AuditorProvider} Bean 即可，
     * 自动配置会把该 Bean 接入审计填充。</p>
     *
     * @param provider 操作人提供者；传 {@code null} 时不写入操作人字段
     */
    public static void setAuditorProvider(AuditorProvider provider) {
        setAuditorSupplier(
                provider == null ? null : provider::getAuditor
        );
    }

    /**
     * 为指定数据源作用域设置操作人提供者。
     *
     * @param scope    作用域名称，等于默认作用域时等同于全局设置
     * @param supplier 操作人提供者；传 {@code null} 时该作用域不写入操作人
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setAuditorSupplier(
            String scope,
            Supplier<String> supplier
    ) {
        if (JpaPlusScope.isDefault(scope)) {
            setAuditorSupplier(supplier);
            return;
        }

        scopeAuditors.put(JpaPlusScope.requireName(scope), supplier);
    }

    /**
     * 为指定数据源作用域设置操作人提供者。
     *
     * @param scope    作用域名称，等于默认作用域时等同于全局设置
     * @param provider 操作人提供者；传 {@code null} 时该作用域不写入操作人
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setAuditorProvider(
            String scope,
            AuditorProvider provider
    ) {
        setAuditorSupplier(
                scope,
                provider == null ? null : provider::getAuditor
        );
    }

    /**
     * 清除指定数据源作用域的审计开关与操作人配置。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearScope(String scope) {
        String name = JpaPlusScope.requireName(scope);

        scopeEnabled.remove(name);
        scopeAuditors.remove(name);
    }

    /**
     * 获取当前作用域生效的操作人。
     *
     * @return 操作人；未配置时返回 {@code null}
     */
    public static String currentAuditor() {
        return activeAuditor().get();
    }

    /**
     * 判断实体是否声明了审计字段。
     *
     * @param entityType 实体类型
     * @return 是否声明审计字段
     */
    public static boolean isAudited(Class<?> entityType) {
        return entityType != null
                && AuditMetadata.find(entityType).isPresent();
    }

    /**
     * 填充插入时的审计字段。
     *
     * <p>写入创建时间、更新时间、创建人和更新人。</p>
     *
     * @param entity 实体
     */
    static void applyCreate(Object entity) {
        if (!activeEnabled() || entity == null) {
            return;
        }

        AuditMetadata.find(entity.getClass()).ifPresent(metadata -> {
            writeTime(metadata.createdAt(), entity);
            writeTime(metadata.updatedAt(), entity);
            writeAuditor(metadata.createdBy(), entity);
            writeAuditor(metadata.updatedBy(), entity);
        });
    }

    /**
     * 填充更新时的审计字段。
     *
     * <p>只写入更新时间和更新人，不覆盖创建信息。</p>
     *
     * @param entity 实体
     */
    static void applyUpdate(Object entity) {
        if (!activeEnabled() || entity == null) {
            return;
        }

        AuditMetadata.find(entity.getClass()).ifPresent(metadata -> {
            writeTime(metadata.updatedAt(), entity);
            writeAuditor(metadata.updatedBy(), entity);
        });
    }

    /**
     * 写入时间字段。
     *
     * @param field  时间字段，允许为 {@code null}
     * @param entity 实体
     */
    private static void writeTime(Field field, Object entity) {
        if (field == null) {
            return;
        }

        write(field, entity, currentTime(field.getType()));
    }

    /**
     * 写入操作人字段。
     *
     * @param field  操作人字段，允许为 {@code null}
     * @param entity 实体
     */
    private static void writeAuditor(Field field, Object entity) {
        if (field == null) {
            return;
        }

        String auditor = activeAuditor().get();

        if (auditor != null && !auditor.isBlank()) {
            write(field, entity, auditor);
        }
    }

    /**
     * 获取当前时间并按字段类型转换。
     *
     * @param type 字段类型
     * @return 当前时间
     */
    private static Object currentTime(Class<?> type) {
        Clock current = clock;
        Instant instant = Instant.now(current);

        if (type == Instant.class) {
            return instant;
        }

        if (type == LocalDateTime.class) {
            return LocalDateTime.now(current);
        }

        if (type == LocalDate.class) {
            return LocalDate.now(current);
        }

        if (type == OffsetDateTime.class) {
            return OffsetDateTime.now(current);
        }

        if (type == ZonedDateTime.class) {
            return ZonedDateTime.now(current);
        }

        if (type == Date.class) {
            return Date.from(instant);
        }

        if (type == Long.class || type == long.class) {
            return instant.toEpochMilli();
        }

        throw new JpaPlusException(
                "Unsupported audit time type: " + type.getName()
        );
    }

    /**
     * 写入字段值。
     *
     * @param field  字段
     * @param entity 实体
     * @param value  值
     */
    private static void write(Field field, Object entity, Object value) {
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new JpaPlusException(
                    "Cannot write audit field "
                            + entity.getClass().getName() + "#"
                            + field.getName(),
                    e
            );
        }
    }

    /**
     * 判断当前作用域的审计填充是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    private static boolean activeEnabled() {
        return activeEnabled(JpaPlusScope.currentScopeName());
    }

    /**
     * 解析指定作用域生效的审计开关。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    private static boolean activeEnabled(String scope) {
        if (scope == null) {
            return enabled;
        }

        Boolean scoped = scopeEnabled.get(scope);

        return scoped == null ? enabled : scoped;
    }

    /**
     * 获取当前作用域生效的操作人提供者。
     *
     * @return 操作人提供者
     */
    private static Supplier<String> activeAuditor() {
        String scope = JpaPlusScope.currentScopeName();

        if (scope == null) {
            return auditorSupplier;
        }

        Supplier<String> scoped = scopeAuditors.get(scope);

        return scoped == null ? auditorSupplier : scoped;
    }
}
