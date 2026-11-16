package io.github.neopen.core.persistence.audit.trail;

import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.scope.JpaPlusScope;
import jakarta.persistence.Id;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 变更历史工具。
 *
 * <p>在实体参与持久化时对比前后快照，生成 {@link AuditEntry} 并交给
 * {@link AuditLogPublisher}。只有标注 {@link Audited} 的实体会被记录，
 * 默认启用，未注册发布者时不做任何处理。</p>
 *
 * <p>快照保存在弱引用映射中，随实体实例回收；同一实例的多次修改
 * 会复用同一份快照。</p>
 *
 * @author neopen
 * @see Audited
 * @see AuditTrailListener
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class AuditTrails {

    /** JDK 日志，避免核心模块依赖具体日志框架。 */
    private static final System.Logger LOG =
            System.getLogger(AuditTrails.class.getName());

    /** 实体类型到可记录字段的缓存。 */
    private static final Map<Class<?>, List<Field>> FIELD_CACHE =
            new ConcurrentHashMap<>();

    /** 实体实例到字段快照的映射，按对象身份比较。 */
    private static final Map<IdentityKey, Map<String, Object>> SNAPSHOTS =
            new ConcurrentHashMap<>();

    /** 已回收实体的引用队列。 */
    private static final ReferenceQueue<Object> QUEUE =
            new ReferenceQueue<>();

    /** 是否启用变更历史，默认启用。 */
    private static volatile boolean enabled = true;

    /** 当前发布者。 */
    private static volatile AuditLogPublisher publisher;

    /** 按数据源作用域配置的开关。 */
    private static final Map<String, Boolean> scopeEnabled =
            new ConcurrentHashMap<>();

    /** 按数据源作用域配置的发布者。 */
    private static final Map<String, AuditLogPublisher> scopePublishers =
            new ConcurrentHashMap<>();

    /** 工具类不允许实例化。 */
    private AuditTrails() {
    }

    /**
     * 全局启用或关闭变更历史。
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        AuditTrails.enabled = enabled;
    }

    /**
     * 为指定作用域设置变更历史开关。
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
     * 判断全局变更历史是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断指定作用域的变更历史是否启用。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled(String scope) {
        return activeEnabled(scope);
    }

    /**
     * 设置变更历史发布者。
     *
     * @param publisher 发布者；传 {@code null} 时不发布
     */
    public static void setPublisher(AuditLogPublisher publisher) {
        AuditTrails.publisher = publisher;
    }

    /**
     * 为指定作用域设置变更历史发布者。
     *
     * @param scope     作用域名称，等于默认作用域时等同于全局设置
     * @param publisher 发布者；传 {@code null} 时该作用域不发布
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setPublisher(
            String scope,
            AuditLogPublisher publisher
    ) {
        if (JpaPlusScope.isDefault(scope)) {
            setPublisher(publisher);
            return;
        }

        scopePublishers.put(JpaPlusScope.requireName(scope), publisher);
    }

    /**
     * 清除指定作用域的变更历史配置。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearScope(String scope) {
        String name = JpaPlusScope.requireName(scope);

        scopeEnabled.remove(name);
        scopePublishers.remove(name);
    }

    /**
     * 判断实体是否需要记录变更历史。
     *
     * @param entity 实体
     * @return 需要记录时返回 {@code true}
     */
    public static boolean isAudited(Object entity) {
        return entity != null
                && entity.getClass().isAnnotationPresent(Audited.class);
    }

    /**
     * 记录实体当前快照。
     *
     * @param entity 实体
     */
    static void capture(Object entity) {
        if (!isAudited(entity)) {
            return;
        }

        store(entity, snapshotOf(entity));
    }

    /**
     * 发布新增记录。
     *
     * @param entity 实体
     */
    static void publishCreate(Object entity) {
        Map<String, Object> snapshot = snapshotOf(entity);

        store(entity, snapshot);

        if (!activeEnabled() || snapshot.isEmpty()) {
            return;
        }

        List<AuditChange> changes = new ArrayList<>();

        snapshot.forEach((field, value) ->
                changes.add(new AuditChange(field, null, value)));

        publish(entity, AuditAction.CREATE, changes);
    }

    /**
     * 发布修改记录。
     *
     * @param entity 实体
     */
    static void publishUpdate(Object entity) {
        Map<String, Object> current = snapshotOf(entity);
        Map<String, Object> previous = replace(entity, current);

        if (previous == null || !activeEnabled()) {
            return;
        }

        List<AuditChange> changes = new ArrayList<>();

        current.forEach((field, value) -> {
            Object before = previous.get(field);

            if (!Objects.equals(before, value)) {
                changes.add(new AuditChange(field, before, value));
            }
        });

        if (changes.isEmpty()) {
            return;
        }

        publish(entity, AuditAction.UPDATE, changes);
    }

    /**
     * 记录删除前的快照。
     *
     * @param entity 实体
     */
    static void captureForDelete(Object entity) {
        capture(entity);
    }

    /**
     * 发布删除记录。
     *
     * @param entity 实体
     */
    static void publishDelete(Object entity) {
        Map<String, Object> snapshot = take(entity);

        if (snapshot == null || !activeEnabled()) {
            return;
        }

        List<AuditChange> changes = new ArrayList<>();

        snapshot.forEach((field, value) ->
                changes.add(new AuditChange(field, value, null)));

        publish(entity, AuditAction.DELETE, changes);
    }

    /**
     * 组装并发布变更历史。
     *
     * @param entity  实体
     * @param action  变更类型
     * @param changes 字段变更
     */
    private static void publish(
            Object entity,
            AuditAction action,
            List<AuditChange> changes
    ) {
        AuditLogPublisher active = activePublisher();

        if (active == null) {
            return;
        }

        AuditEntry entry = new AuditEntry(
                entity.getClass().getSimpleName(),
                entity.getClass(),
                idOf(entity),
                action,
                changes,
                Audits.currentAuditor(),
                Instant.now()
        );

        try {
            active.publish(entry);
        } catch (RuntimeException e) {
            LOG.log(
                    System.Logger.Level.WARNING,
                    "Failed to publish audit entry for "
                            + entity.getClass().getName(),
                    e
            );
        }
    }

    /**
     * 保存实体当前快照。
     *
     * @param entity   实体
     * @param snapshot 字段快照
     */
    private static void store(Object entity, Map<String, Object> snapshot) {
        expunge();

        SNAPSHOTS.put(new IdentityKey(entity), snapshot);
    }

    /**
     * 覆盖实体快照并返回旧快照。
     *
     * @param entity   实体
     * @param snapshot 字段快照
     * @return 旧快照；不存在时返回 {@code null}
     */
    private static Map<String, Object> replace(
            Object entity,
            Map<String, Object> snapshot
    ) {
        expunge();

        return SNAPSHOTS.put(new IdentityKey(entity), snapshot);
    }

    /**
     * 取出并移除实体快照。
     *
     * @param entity 实体
     * @return 字段快照；不存在时返回 {@code null}
     */
    private static Map<String, Object> take(Object entity) {
        expunge();

        return SNAPSHOTS.remove(new IdentityKey(entity));
    }

    /**
     * 清理已被回收的实体快照。
     */
    private static void expunge() {
        Reference<?> reference;

        while ((reference = QUEUE.poll()) != null) {
            SNAPSHOTS.remove(reference);
        }
    }

    /**
     * 按对象身份比较的弱引用键。
     *
     * <p>实体的 {@code equals}/{@code hashCode} 通常由字段生成，
     * 字段变化后无法再定位原快照，因此快照表按引用身份索引。</p>
     */
    private static final class IdentityKey extends WeakReference<Object> {

        /** 创建时的身份哈希，保证键在对象被回收前保持稳定。 */
        private final int hash;

        /**
         * 创建键。
         *
         * @param referent 实体实例
         */
        private IdentityKey(Object referent) {
            super(referent, QUEUE);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof IdentityKey key)) {
                return false;
            }

            Object value = get();

            return value != null && value == key.get();
        }
    }

    /**
     * 读取实体的字段快照。
     *
     * @param entity 实体
     * @return 字段名到值的映射
     */
    private static Map<String, Object> snapshotOf(Object entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        for (Field field : fieldsOf(entity.getClass())) {
            try {
                snapshot.put(field.getName(), field.get(entity));
            } catch (IllegalAccessException e) {
                LOG.log(
                        System.Logger.Level.WARNING,
                        "Cannot read audit field "
                                + entity.getClass().getName()
                                + "#" + field.getName(),
                        e
                );
            }
        }

        return snapshot;
    }

    /**
     * 获取实体主键。
     *
     * @param entity 实体
     * @return 主键值；未找到时返回 {@code null}
     */
    private static Object idOf(Object entity) {
        for (Class<?> current = entity.getClass();
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Id.class)) {
                    continue;
                }

                try {
                    field.setAccessible(true);

                    return field.get(entity);
                } catch (IllegalAccessException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * 获取实体可记录的字段。
     *
     * @param entityType 实体类型
     * @return 字段列表
     */
    private static List<Field> fieldsOf(Class<?> entityType) {
        return FIELD_CACHE.computeIfAbsent(
                entityType,
                AuditTrails::scanFields
        );
    }

    /**
     * 扫描实体字段，过滤掉不可比较或不应记录的字段。
     *
     * @param entityType 实体类型
     * @return 字段列表
     */
    private static List<Field> scanFields(Class<?> entityType) {
        List<Field> fields = new ArrayList<>();

        for (Class<?> current = entityType;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();

                if (Modifier.isStatic(modifiers)
                        || Modifier.isTransient(modifiers)
                        || field.isSynthetic()
                        || field.isAnnotationPresent(
                        jakarta.persistence.Transient.class)
                        || !isSimple(field.getType())) {
                    continue;
                }

                field.setAccessible(true);
                fields.add(field);
            }
        }

        return List.copyOf(fields);
    }

    /**
     * 判断字段类型是否可以直接比较并记录。
     *
     * @param type 字段类型
     * @return 可记录时返回 {@code true}
     */
    private static boolean isSimple(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type == String.class
                || type == Boolean.class
                || type == Character.class
                || type == java.util.UUID.class
                || Number.class.isAssignableFrom(type)
                || java.util.Date.class.isAssignableFrom(type)
                || type.getName().startsWith("java.time.");
    }

    /**
     * 判断当前作用域的变更历史是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    private static boolean activeEnabled() {
        return activeEnabled(JpaPlusScope.currentScopeName());
    }

    /**
     * 解析指定作用域生效的开关。
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
     * 获取当前作用域生效的发布者。
     *
     * @return 发布者
     */
    private static AuditLogPublisher activePublisher() {
        String scope = JpaPlusScope.currentScopeName();

        if (scope == null) {
            return publisher;
        }

        AuditLogPublisher scoped = scopePublishers.get(scope);

        return scoped == null ? publisher : scoped;
    }
}
