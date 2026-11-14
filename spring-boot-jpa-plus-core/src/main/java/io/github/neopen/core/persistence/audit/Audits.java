package io.github.neopen.core.persistence.audit;

import io.github.neopen.core.exception.JpaPlusException;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
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
 */
public final class Audits {

    /** 当前时钟。 */
    private static volatile Clock clock = Clock.systemUTC();

    /** 当前操作人提供者。 */
    private static volatile Supplier<String> auditorSupplier = () -> null;

    /** 工具类不允许实例化。 */
    private Audits() {
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
        if (entity == null) {
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
        if (entity == null) {
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

        String auditor = auditorSupplier.get();

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
}
