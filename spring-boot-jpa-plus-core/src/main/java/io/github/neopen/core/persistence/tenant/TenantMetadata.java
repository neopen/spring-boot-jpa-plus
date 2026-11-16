package io.github.neopen.core.persistence.tenant;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户字段元数据。
 *
 * <p>按实体类型缓存 {@link TenantId} 字段信息，避免重复反射。</p>
 *
 * @author neopen
 * @see Tenants
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
final class TenantMetadata {

    /** 实体类型到租户元数据的缓存。 */
    private static final Map<Class<?>, Optional<TenantMetadata>>
            CACHE = new ConcurrentHashMap<>();

    /** 租户字段名。 */
    private final String fieldName;

    /** 租户字段。 */
    private final Field field;

    /**
     * 创建租户元数据。
     *
     * @param field 租户字段
     */
    private TenantMetadata(Field field) {
        this.fieldName = field.getName();
        this.field = field;
    }

    /**
     * 查找实体及其父类上的租户字段。
     *
     * @param entityType 实体类型
     * @return 租户元数据；未标注时返回空
     */
    static Optional<TenantMetadata> find(Class<?> entityType) {
        return CACHE.computeIfAbsent(
                entityType,
                TenantMetadata::scan
        );
    }

    /**
     * 扫描实体及其父类的租户字段。
     *
     * @param entityType 实体类型
     * @return 租户元数据
     */
    private static Optional<TenantMetadata> scan(Class<?> entityType) {
        for (Class<?> current = entityType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(TenantId.class)) {
                    continue;
                }

                Class<?> fieldType = field.getType();

                if (fieldType != String.class
                        && fieldType != Long.class
                        && fieldType != long.class
                        && fieldType != Integer.class
                        && fieldType != int.class
                        && fieldType != UUID.class) {
                    throw new IllegalArgumentException(
                            "@TenantId field must be String, Long, Integer "
                                    + "or UUID: " + entityType.getName()
                                    + "#" + field.getName()
                    );
                }

                field.setAccessible(true);

                return Optional.of(new TenantMetadata(field));
            }
        }

        return Optional.empty();
    }

    /**
     * 获取租户字段名。
     *
     * @return 租户字段名
     */
    String fieldName() {
        return fieldName;
    }

    /**
     * 获取租户字段。
     *
     * @return 租户字段
     */
    Field field() {
        return field;
    }

    /**
     * 把字符串租户标识转换为字段类型。
     *
     * @param tenant 租户标识
     * @return 字段类型的租户值
     * @throws IllegalArgumentException 无法转换时抛出
     */
    Object convert(String tenant) {
        Class<?> type = field.getType();

        try {
            if (type == String.class) {
                return tenant;
            }

            if (type == Long.class || type == long.class) {
                return Long.valueOf(tenant);
            }

            if (type == Integer.class || type == int.class) {
                return Integer.valueOf(tenant);
            }

            return UUID.fromString(tenant);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "Cannot convert tenant [" + tenant + "] to "
                            + type.getName() + " for field " + fieldName,
                    e
            );
        }
    }
}
