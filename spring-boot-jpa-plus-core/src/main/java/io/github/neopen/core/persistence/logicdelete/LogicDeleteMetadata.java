package io.github.neopen.core.persistence.logicdelete;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 逻辑删除元数据。
 *
 * <p>按实体类型缓存 {@link LogicDelete} 字段信息，避免重复反射。
 * 字段类型必须是 {@link Boolean} 或 {@code boolean}，否则在首次
 * 扫描时快速失败。</p>
 */
final class LogicDeleteMetadata {

    /** 实体类型到逻辑删除元数据的缓存。 */
    private static final Map<Class<?>, Optional<LogicDeleteMetadata>>
            CACHE = new ConcurrentHashMap<>();

    /** 逻辑删除字段名。 */
    private final String fieldName;

    /** 逻辑删除字段。 */
    private final Field field;

    /**
     * 创建逻辑删除元数据。
     *
     * @param field 逻辑删除字段
     */
    private LogicDeleteMetadata(Field field) {
        this.fieldName = field.getName();
        this.field = field;
    }

    /**
     * 查找实体及其父类上的逻辑删除元数据。
     *
     * @param entityType 实体类型
     * @return 逻辑删除元数据；未标注时返回空
     */
    static Optional<LogicDeleteMetadata> find(Class<?> entityType) {
        return CACHE.computeIfAbsent(
                entityType,
                LogicDeleteMetadata::scan
        );
    }

    /**
     * 扫描实体及其父类的逻辑删除字段。
     *
     * @param entityType 实体类型
     * @return 逻辑删除元数据
     */
    private static Optional<LogicDeleteMetadata> scan(
            Class<?> entityType
    ) {
        for (Class<?> current = entityType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(LogicDelete.class)) {
                    continue;
                }

                Class<?> fieldType = field.getType();

                if (fieldType != Boolean.class && fieldType != boolean.class) {
                    throw new IllegalArgumentException(
                            "@LogicDelete field must be Boolean: "
                                    + entityType.getName() + "#"
                                    + field.getName()
                    );
                }

                field.setAccessible(true);

                return Optional.of(new LogicDeleteMetadata(field));
            }
        }

        return Optional.empty();
    }

    /**
     * 获取逻辑删除字段名。
     *
     * @return 逻辑删除字段名
     */
    String fieldName() {
        return fieldName;
    }

    /**
     * 获取逻辑删除字段。
     *
     * @return 逻辑删除字段
     */
    Field field() {
        return field;
    }
}
