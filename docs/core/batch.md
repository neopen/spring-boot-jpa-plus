# 批量操作

`BatchOperation` 按批次执行 `flush` 与 `clear`，避免大批量写入时持久化上下文持续膨胀。

## 方法

| 方法 | 说明 |
| --- | --- |
| `static <T> List<T> mergeAll(EntityManager em, Collection<T> entities)` | 使用默认批次大小执行 `merge` |
| `static <T> List<T> mergeAll(EntityManager em, Collection<T> entities, int batchSize)` | 指定批次大小 |
| `static <T> void persistAll(EntityManager em, Collection<T> entities, int batchSize)` | 批量 `persist`，适用于新实体 |
| `static <T> void removeAll(EntityManager em, Collection<T> entities, int batchSize)` | 批量 `remove`，适用于托管实体 |
| `static <T> void forEachBatch(Collection<T> values, int batchSize, Consumer<List<T>> consumer)` | 按批次消费集合，适合调用仓储批量方法 |
| `static <T> UpsertResult upsertAll(EntityManager em, Class<T> type, Collection<T> entities, Function<T,Object> keyExtractor, String keyProperty)` | 按业务唯一键批量新增或更新 |
| `static void setDefaultBatchSize(int batchSize)` / `static int getDefaultBatchSize()` | 读取或修改默认批次大小 |

## 用法

```java
BatchOperation.persistAll(entityManager, orders, 500);
BatchOperation.removeAll(entityManager, orders, 500);

BatchOperation.forEachBatch(ids, 500, batch ->
        orderRepository.deleteAllByIdInBatch(batch));
```

## 行为约定

- 每处理完一批执行 `flush` 与 `clear`；空集合直接返回，不做任何数据库操作。
- `mergeAll` 返回合并后的实体列表。
- `EntityManager` 为空、批次大小不为正数、回调为空，都会在开始数据库操作前抛出 `IllegalArgumentException`。
- `persistAll` 面向尚未持久化的实体，`removeAll` 面向托管实体；使用前需确认实体状态，否则会由 JPA 抛错。

## 批量 upsert

按业务唯一键新增或更新，调用方不需要提供主键：

```java
BatchOperation.UpsertResult result = BatchOperation.upsertAll(
        entityManager,
        DocumentRecord.class,
        records,
        DocumentRecord::getDocNo,   // 业务键提取
        "docNo",                    // 业务键字段名
        500
);
// result.inserted() / result.updated() / result.total()
```

执行过程：

1. 按 `batchSize` 切分，每批用业务键查询已存在的托管实体。
2. 不存在的实体执行 `persist`。
3. 已存在的实体把可持久化字段复制到托管实体，由脏检查在 `flush` 时更新。
4. 每批结束后执行 `flush` 与 `clear`。

约定：

- 业务键必须唯一，否则一批中会出现重复匹配。
- 复制时跳过 `@Id`、`@Version`、`@Transient` 与业务键字段，且只复制简单类型字段。
- 返回后传入的实体均处于游离状态，不能继续依赖其延迟加载能力。
- `keyProperty` 为空、批次大小不为正数、业务键提取函数为空都会抛 `IllegalArgumentException`。

## 默认批次大小

```java
BatchOperation.setDefaultBatchSize(200);
```

```yaml
neopen:
  jpa:
    batch-size: 200
```

`neopen.jpa.batch-size` 在自动配置启动时写入 `BatchOperation`，未显式传批次大小的重载会使用该值，默认 500。

注意：该值是**每批 flush / clear 的条数**，不是 Hibernate 的 `hibernate.jdbc.batch_size`，两者需要分别配置。
