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
| `static <T> UpsertResult upsertAll(EntityManager em, Class<T> type, Collection<T> entities, Function<T,Object> keyExtractor, String keyProperty)` | Inserts or updates by business key in batches |
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

## Batch Upsert

Insert or update by business key without supplying primary keys:

```java
BatchOperation.UpsertResult result = BatchOperation.upsertAll(
        entityManager,
        DocumentRecord.class,
        records,
        DocumentRecord::getDocNo,   // business key extractor
        "docNo",                    // business key property
        500
);
// result.inserted() / result.updated() / result.total()
```

Execution:

1. Split into chunks of `batchSize` and query existing managed entities by business key.
2. `persist` entities that do not exist.
3. Copy persistable fields onto the managed entities for existing ones; dirty checking updates them on `flush`.
4. `flush` and `clear` after each chunk.

Rules:

- The business key must be unique; duplicates in one chunk match the same row.
- Copying skips `@Id`, `@Version`, `@Transient` and the key property, and only handles simple field types.
- All input entities are detached after the call; do not rely on their lazy loading afterwards.
- A blank `keyProperty`, a non-positive batch size, or a null extractor throws `IllegalArgumentException`.

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
