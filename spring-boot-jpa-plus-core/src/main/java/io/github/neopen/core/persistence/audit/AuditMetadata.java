package io.github.neopen.core.persistence.audit;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计字段元数据。
 *
 * <p>按实体类型缓存 {@link CreatedAt}、{@link UpdatedAt}、
 * {@link CreatedBy} 与 {@link UpdatedBy} 字段信息，并在首次扫描时
 * 校验字段类型。</p>
 */
final class AuditMetadata {

    /** 实体类型到审计元数据的缓存。 */
    private static final Map<Class<?>, Optional<AuditMetadata>> CACHE =
            new ConcurrentHashMap<>();

    /** 创建时间字段。 */
    private final Field createdAt;

    /** 更新时间字段。 */
    private final Field updatedAt;

    /** 创建人字段。 */
    private final Field createdBy;

    /** 更新人字段。 */
    private final Field updatedBy;

    /**
     * 创建审计元数据。
     *
     * @param createdAt 创建时间字段
     * @param updatedAt 更新时间字段
     * @param createdBy 创建人字段
     * @param updatedBy 更新人字段
     */
    private AuditMetadata(
            Field createdAt,
            Field updatedAt,
            Field createdBy,
            Field updatedBy
    ) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    /**
     * 查找实体的审计元数据。
     *
     * @param entityType 实体类型
     * @return 审计元数据；未标注时返回空
     */
    static Optional<AuditMetadata> find(Class<?> entityType) {
        return CACHE.computeIfAbsent(entityType, AuditMetadata::scan);
    }

    /**
     * 扫描实体及其父类的审计字段。
     *
     * @param entityType 实体类型
     * @return 审计元数据
     */
    private static Optional<AuditMetadata> scan(Class<?> entityType) {
        Field createdAt = null;
        Field updatedAt = null;
        Field createdBy = null;
        Field updatedBy = null;

        for (Class<?> current = entityType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(CreatedAt.class)) {
                    validateTimeField(entityType, field);
                    createdAt = createdAt == null ? field : createdAt;
                }

                if (field.isAnnotationPresent(UpdatedAt.class)) {
                    validateTimeField(entityType, field);
                    updatedAt = updatedAt == null ? field : updatedAt;
                }

                if (field.isAnnotationPresent(CreatedBy.class)) {
                    validateAuditorField(entityType, field);
                    createdBy = createdBy == null ? field : createdBy;
                }

                if (field.isAnnotationPresent(UpdatedBy.class)) {
                    validateAuditorField(entityType, field);
                    updatedBy = updatedBy == null ? field : updatedBy;
                }
            }
        }

        if (createdAt == null
                && updatedAt == null
                && createdBy == null
                && updatedBy == null) {
            return Optional.empty();
        }

        return Optional.of(new AuditMetadata(
                createdAt,
                updatedAt,
                createdBy,
                updatedBy
        ));
    }

    /**
     * 校验时间字段类型。
     *
     * @param entityType 实体类型
     * @param field      字段
     */
    private static void validateTimeField(Class<?> entityType, Field field) {
        Class<?> type = field.getType();

        boolean supported = type == Instant.class
                || type == LocalDateTime.class
                || type == LocalDate.class
                || type == OffsetDateTime.class
                || type == ZonedDateTime.class
                || type == Date.class
                || type == Long.class
                || type == long.class;

        if (!supported) {
            throw new IllegalArgumentException(
                    "Unsupported audit time type "
                            + type.getName() + " for "
                            + entityType.getName() + "#" + field.getName()
            );
        }

        field.setAccessible(true);
    }

    /**
     * 校验操作人字段类型。
     *
     * @param entityType 实体类型
     * @param field      字段
     */
    private static void validateAuditorField(
            Class<?> entityType,
            Field field
    ) {
        if (field.getType() != String.class) {
            throw new IllegalArgumentException(
                    "Audit auditor field must be String: "
                            + entityType.getName() + "#" + field.getName()
            );
        }

        field.setAccessible(true);
    }

    /**
     * 获取创建时间字段。
     *
     * @return 创建时间字段；未标注时返回 {@code null}
     */
    Field createdAt() {
        return createdAt;
    }

    /**
     * 获取更新时间字段。
     *
     * @return 更新时间字段；未标注时返回 {@code null}
     */
    Field updatedAt() {
        return updatedAt;
    }

    /**
     * 获取创建人字段。
     *
     * @return 创建人字段；未标注时返回 {@code null}
     */
    Field createdBy() {
        return createdBy;
    }

    /**
     * 获取更新人字段。
     *
     * @return 更新人字段；未标注时返回 {@code null}
     */
    Field updatedBy() {
        return updatedBy;
    }
}
