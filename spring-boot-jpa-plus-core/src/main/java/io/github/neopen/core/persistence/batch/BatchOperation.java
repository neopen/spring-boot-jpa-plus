package io.github.neopen.core.persistence.batch;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * JPA 批量操作工具。
 *
 * <p>按指定批次大小刷新并清理持久化上下文，避免批量处理时
 * 持久化上下文持续增长。</p>
 */
public final class BatchOperation {

    /** 默认每批处理的实体数量。 */
    private static volatile int defaultBatchSize = 500;

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
     * 获取默认批次大小。
     *
     * @return 每批处理的实体数量
     */
    public static int getDefaultBatchSize() {
        return defaultBatchSize;
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
                defaultBatchSize
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
                defaultBatchSize
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
}
