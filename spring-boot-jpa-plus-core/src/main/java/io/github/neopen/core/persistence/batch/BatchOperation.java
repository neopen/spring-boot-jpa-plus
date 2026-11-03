package io.github.neopen.core.persistence.batch;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JPA 批量操作工具
 */
public final class BatchOperation {

    private static final int DEFAULT_BATCH_SIZE = 500;

    private BatchOperation() {
    }

    /**
     * 批量合并实体
     *
     * 注意：返回的实体在对应批次 clear 后可能已经脱离持久化上下文。
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
     * 使用默认批次大小
     */
    public static <T> List<T> mergeAll(
            EntityManager entityManager,
            Collection<T> entities
    ) {
        return mergeAll(
                entityManager,
                entities,
                DEFAULT_BATCH_SIZE
        );
    }

    /**
     * 批量持久化新实体
     *
     * 只适用于尚未持久化的实体。
     */
    public static <T> void persistAll(
            EntityManager entityManager,
            Collection<T> entities,
            int batchSize
    ) {
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
}
