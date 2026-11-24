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
package io.github.neopen.core.persistence.batch;

import io.github.neopen.core.scope.JpaPlusScope;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * JPA 批量操作工具。
 *
 * <p>按指定批次大小刷新并清理持久化上下文，避免批量处理时
 * 持久化上下文持续增长。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class BatchOperation {

    /** 默认每批处理的实体数量。 */
    private static volatile int defaultBatchSize = 500;

    /** 按数据源作用域配置的批次大小。 */
    private static final Map<String, Integer> scopeBatchSizes =
            new ConcurrentHashMap<>();

    /** 工具类不允许实例化。 */
    private BatchOperation() {
    }

    /**
     * 设置默认批次大小。
     *
     * <p>未显式传入批次大小的重载方法会使用该值。自动配置会读取
     * {@code neopen.jpa.batch-size} 并调用此方法。</p>
     *
     * @param batchSize 每批处理的实体数量，必须大于 0
     * @throws IllegalArgumentException 批次大小不大于 0
     */
    public static void setDefaultBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Default batchSize must be greater than zero"
            );
        }

        BatchOperation.defaultBatchSize = batchSize;
    }

    /**
     * 为指定数据源作用域设置默认批次大小。
     *
     * @param scope     作用域名称，等于默认作用域时等同于全局设置
     * @param batchSize 每批处理的实体数量，必须大于 0
     * @throws IllegalArgumentException 作用域名称为空或批次大小不大于 0
     */
    public static void setDefaultBatchSize(String scope, int batchSize) {
        if (JpaPlusScope.isDefault(scope)) {
            setDefaultBatchSize(batchSize);
            return;
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "Default batchSize must be greater than zero"
            );
        }

        scopeBatchSizes.put(
                JpaPlusScope.requireName(scope),
                batchSize
        );
    }

    /**
     * 获取全局默认批次大小。
     *
     * @return 每批处理的实体数量
     */
    public static int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    /**
     * 获取指定作用域生效的批次大小。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 每批处理的实体数量
     */
    public static int getDefaultBatchSize(String scope) {
        return activeBatchSize(scope);
    }

    /**
     * 清除指定作用域的批次大小配置。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearScope(String scope) {
        scopeBatchSizes.remove(JpaPlusScope.requireName(scope));
    }

    /**
     * 批量合并实体。
     *
     * <p>注意：返回的实体在对应批次执行 {@code clear} 后可能已经
     * 脱离持久化上下文。</p>
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entities      待合并实体，允许为 {@code null} 或空集合
     * @param batchSize     每批处理的实体数量，必须大于 0
     * @return 合并后的实体列表；输入为空时返回空列表
     * @throws IllegalArgumentException 实体管理器为空或批次大小无效
     */
    public static <T> List<T> mergeAll(
            EntityManager entityManager,
            Collection<T> entities,
            int batchSize
    ) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "EntityManager must not be null"
            );
        }

        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        List<T> result = new ArrayList<>(entities.size());

        int count = 0;

        for (T entity : entities) {
            result.add(entityManager.merge(entity));

            count++;

            if (count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();

        return result;
    }

    /**
     * 使用默认批次大小批量合并实体。
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entities      待合并实体，允许为 {@code null} 或空集合
     * @return 合并后的实体列表；输入为空时返回空列表
     * @throws IllegalArgumentException 实体管理器为空
     */
    public static <T> List<T> mergeAll(
            EntityManager entityManager,
            Collection<T> entities
    ) {
        return mergeAll(
                entityManager,
                entities,
                activeBatchSize()
        );
    }

    /**
     * 批量持久化新实体。
     *
     * <p>只适用于尚未持久化的实体。调用方应确保实体管理器有效，
     * 并在需要时提供事务上下文。</p>
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entities      待持久化实体，允许为 {@code null} 或空集合
     * @param batchSize     每批处理的实体数量，必须大于 0
     * @throws IllegalArgumentException 实体管理器为空或批次大小不大于 0
     */
    public static <T> void persistAll(
            EntityManager entityManager,
            Collection<T> entities,
            int batchSize
    ) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "EntityManager must not be null"
            );
        }

        if (entities == null || entities.isEmpty()) {
            return;
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        int count = 0;

        for (T entity : entities) {
            entityManager.persist(entity);

            count++;

            if (count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 批量删除实体。
     *
     * <p>按批次刷新并清理持久化上下文；实体必须处于托管状态。</p>
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entities      待删除实体，允许为 {@code null} 或空集合
     * @param batchSize     每批处理的实体数量，必须大于 0
     * @throws IllegalArgumentException 实体管理器为空或批次大小不大于 0
     */
    public static <T> void removeAll(
            EntityManager entityManager,
            Collection<T> entities,
            int batchSize
    ) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "EntityManager must not be null"
            );
        }

        if (entities == null || entities.isEmpty()) {
            return;
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        int count = 0;

        for (T entity : entities) {
            entityManager.remove(entity);

            count++;

            if (count % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * 使用默认批次大小批量删除实体。
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entities      待删除实体，允许为 {@code null} 或空集合
     * @throws IllegalArgumentException 实体管理器为空
     */
    public static <T> void removeAll(
            EntityManager entityManager,
            Collection<T> entities
    ) {
        removeAll(
                entityManager,
                entities,
                activeBatchSize()
        );
    }

    /**
     * 按批次回调处理集合元素。
     *
     * <p>适合自定义批量更新等无法通过实体管理器直接完成的场景；
     * 每批只包含当前批次的元素视图。</p>
     *
     * @param <T>       元素类型
     * @param items     待处理元素，允许为 {@code null} 或空集合
     * @param batchSize 每批处理的元素数量，必须大于 0
     * @param consumer  批次处理回调
     * @throws IllegalArgumentException 批次大小不大于 0 或回调为空
     */
    public static <T> void forEachBatch(
            Collection<T> items,
            int batchSize,
            Consumer<List<T>> consumer
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        if (consumer == null) {
            throw new IllegalArgumentException(
                    "Consumer must not be null"
            );
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        List<T> batch = new ArrayList<>(Math.min(batchSize, items.size()));

        for (T item : items) {
            batch.add(item);

            if (batch.size() == batchSize) {
                consumer.accept(batch);
                batch = new ArrayList<>(batchSize);
            }
        }

        if (!batch.isEmpty()) {
            consumer.accept(batch);
        }
    }

    /**
     * 按业务唯一键批量新增或更新实体。
     *
     * <p>按业务键分批查询已存在的实体：不存在的新增，已存在的把
     * 可持久化字段复制到托管实体，由脏检查统一更新。调用方无需
     * 提供主键，但需要保证业务键唯一。</p>
     *
     * <p>每批处理完成后会执行 {@code flush} 与 {@code clear}，
     * 返回后传入的实体均处于游离状态。</p>
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entityType    实体类型，不能为 {@code null}
     * @param entities      待处理实体，允许为 {@code null} 或空集合
     * @param keyExtractor  业务键提取函数，不能为 {@code null}
     * @param keyProperty   业务键字段名，不能为空
     * @param batchSize     每批处理的实体数量，必须大于 0
     * @return 新增与更新数量
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <T> UpsertResult upsertAll(
            EntityManager entityManager,
            Class<T> entityType,
            Collection<T> entities,
            Function<T, Object> keyExtractor,
            String keyProperty,
            int batchSize
    ) {
        if (entityManager == null) {
            throw new IllegalArgumentException(
                    "EntityManager must not be null"
            );
        }

        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        if (keyExtractor == null) {
            throw new IllegalArgumentException(
                    "Key extractor must not be null"
            );
        }

        if (keyProperty == null || keyProperty.isBlank()) {
            throw new IllegalArgumentException(
                    "Key property must not be blank"
            );
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "batchSize must be greater than zero"
            );
        }

        if (entities == null || entities.isEmpty()) {
            return new UpsertResult(0, 0);
        }

        List<T> items = new ArrayList<>(entities);
        int inserted = 0;
        int updated = 0;

        for (int from = 0; from < items.size(); from += batchSize) {
            int to = Math.min(from + batchSize, items.size());
            List<T> chunk = items.subList(from, to);
            Map<Object, T> existing = findExisting(
                    entityManager,
                    entityType,
                    chunk,
                    keyExtractor,
                    keyProperty
            );

            for (T entity : chunk) {
                if (entity == null) {
                    continue;
                }

                Object key = keyExtractor.apply(entity);
                T current = key == null ? null : existing.get(key);

                if (current != null) {
                    copyState(entity, current, keyProperty);
                    updated++;
                } else {
                    entityManager.persist(entity);

                    if (key != null) {
                        existing.put(key, entity);
                    }

                    inserted++;
                }
            }

            entityManager.flush();
            entityManager.clear();
        }

        return new UpsertResult(inserted, updated);
    }

    /**
     * 使用默认批次大小按业务唯一键批量新增或更新实体。
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器，不能为 {@code null}
     * @param entityType    实体类型，不能为 {@code null}
     * @param entities      待处理实体，允许为 {@code null} 或空集合
     * @param keyExtractor  业务键提取函数，不能为 {@code null}
     * @param keyProperty   业务键字段名，不能为空
     * @return 新增与更新数量
     * @throws IllegalArgumentException 参数非法时抛出
     */
    public static <T> UpsertResult upsertAll(
            EntityManager entityManager,
            Class<T> entityType,
            Collection<T> entities,
            Function<T, Object> keyExtractor,
            String keyProperty
    ) {
        return upsertAll(
                entityManager,
                entityType,
                entities,
                keyExtractor,
                keyProperty,
                activeBatchSize()
        );
    }

    /**
     * 分批查询已存在的业务键。
     *
     * @param <T>           实体类型
     * @param entityManager 实体管理器
     * @param entityType    实体类型
     * @param entities      待处理实体
     * @param keyExtractor  业务键提取函数
     * @param keyProperty   业务键字段名
     * @return 业务键到已存在实体的映射
     */
    private static <T> Map<Object, T> findExisting(
            EntityManager entityManager,
            Class<T> entityType,
            Collection<T> entities,
            Function<T, Object> keyExtractor,
            String keyProperty
    ) {
        List<Object> keys = new ArrayList<>(entities.size());

        for (T entity : entities) {
            if (entity == null) {
                continue;
            }

            Object key = keyExtractor.apply(entity);

            if (key != null) {
                keys.add(key);
            }
        }

        Map<Object, T> existing = new java.util.HashMap<>();

        if (keys.isEmpty()) {
            return existing;
        }

        String entityName = entityManager.getMetamodel()
                .entity(entityType)
                .getName();
        String jpql = "select e from " + entityName + " e"
                + " where e." + keyProperty + " in :keys";

        List<T> found = entityManager.createQuery(jpql, entityType)
                .setParameter("keys", keys)
                .getResultList();

        for (T entity : found) {
            existing.put(keyExtractor.apply(entity), entity);
        }

        return existing;
    }

    /**
     * 把源实体的可持久化字段复制到目标实体。
     *
     * <p>跳过主键、版本字段与业务键，避免覆盖身份信息；关联、
     * 集合等复杂字段不参与复制。</p>
     *
     * @param <T>         实体类型
     * @param source      源实体
     * @param target      目标实体
     * @param keyProperty 业务键字段名
     */
    private static <T> void copyState(
            T source,
            T target,
            String keyProperty
    ) {
        for (Class<?> current = source.getClass();
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (java.lang.reflect.Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();

                if (java.lang.reflect.Modifier.isStatic(modifiers)
                        || java.lang.reflect.Modifier.isTransient(modifiers)
                        || field.isSynthetic()
                        || field.getName().equals(keyProperty)
                        || field.isAnnotationPresent(jakarta.persistence.Id.class)
                        || field.isAnnotationPresent(
                        jakarta.persistence.Version.class)
                        || field.isAnnotationPresent(
                        jakarta.persistence.Transient.class)
                        || !isCopyable(field.getType())) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                    field.set(target, field.get(source));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(
                            "Cannot copy field "
                                    + source.getClass().getName()
                                    + "#" + field.getName(),
                            e
                    );
                }
            }
        }
    }

    /**
     * 判断字段类型是否参与复制。
     *
     * @param type 字段类型
     * @return 可以复制时返回 {@code true}
     */
    private static boolean isCopyable(Class<?> type) {
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
     * 批量新增或更新的结果。
     *
     * @param inserted 新增数量
     * @param updated  更新数量
     * @author neopen
     */
    public record UpsertResult(int inserted, int updated) {

        /**
         * 获取总处理数量。
         *
         * @return 新增与更新数量之和
         */
        public int total() {
            return inserted + updated;
        }
    }

    /**
     * 获取当前作用域生效的批次大小。
     *
     * @return 每批处理的实体数量
     */
    private static int activeBatchSize() {
        return activeBatchSize(JpaPlusScope.currentScopeName());
    }

    /**
     * 解析指定作用域生效的批次大小。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 每批处理的实体数量
     */
    private static int activeBatchSize(String scope) {
        if (scope == null) {
            return defaultBatchSize;
        }

        Integer scoped = scopeBatchSizes.get(scope);

        return scoped == null ? defaultBatchSize : scoped;
    }
}
