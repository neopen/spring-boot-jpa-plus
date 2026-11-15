# Batch Operations

`BatchOperation` performs `flush` and `clear` in batches to prevent the persistence context from growing continuously during large batch writes.

## Methods

| Method | Description |
| --- | --- |
| `static <T> List<T> mergeAll(EntityManager em, Collection<T> entities)` | Executes `merge` with the default batch size |
| `static <T> List<T> mergeAll(EntityManager em, Collection<T> entities, int batchSize)` | Specifies the batch size |
| `static <T> void persistAll(EntityManager em, Collection<T> entities, int batchSize)` | Batch `persist`, suitable for new entities |
| `static <T> void removeAll(EntityManager em, Collection<T> entities, int batchSize)` | Batch `remove`, suitable for managed entities |
| `static <T> void forEachBatch(Collection<T> values, int batchSize, Consumer<List<T>> consumer)` | Consumes a collection in batches, suitable for calling repository batch methods |
| `static void setDefaultBatchSize(int batchSize)` / `static int getDefaultBatchSize()` | Reads or modifies the default batch size |

## Usage

```java
BatchOperation.persistAll(entityManager, orders, 500);
BatchOperation.removeAll(entityManager, orders, 500);

BatchOperation.forEachBatch(ids, 500, batch ->
        orderRepository.deleteAllByIdInBatch(batch));
```

## Behavior

- `flush` and `clear` are executed after each batch is processed; an empty collection returns immediately without any database operation.
- `mergeAll` returns the list of merged entities.
- A null `EntityManager`, a non-positive batch size, or a null callback throws `IllegalArgumentException` before any database operation starts.
- `persistAll` targets entities that are not yet persisted, while `removeAll` targets managed entities; confirm the entity state before use, otherwise JPA will throw an error.

## Default Batch Size

```java
BatchOperation.setDefaultBatchSize(200);
```

```yaml
neopen:
  jpa:
    batch-size: 200
```

`neopen.jpa.batch-size` is written into `BatchOperation` during autoconfiguration startup, and overloads that do not explicitly pass a batch size use that value; the default is 500.

Note: this value is the **number of rows flushed / cleared per batch**, not Hibernate's `hibernate.jdbc.batch_size`; the two must be configured separately.
