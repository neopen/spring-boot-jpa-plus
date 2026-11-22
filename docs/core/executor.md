# Specification 执行器

`JpaPlusSpecExecutor` 集中处理 `Specification` 查询的分页、切片、计数与 fetch join 分页，避免每个 Repository 重复实现计数逻辑。

## 创建

```java
JpaPlusSpecExecutor<Order> executor =
        JpaPlusSpecExecutor.of(entityManager, Order.class);

// 主键属性不是 id 时显式指定
JpaPlusSpecExecutor<Order> custom =
        JpaPlusSpecExecutor.of(entityManager, Order.class, "orderId");
```

| 方法 | 说明 |
| --- | --- |
| `static <T> JpaPlusSpecExecutor<T> of(EntityManager, Class<T>)` | 默认主键属性为 `id` |
| `static <T> JpaPlusSpecExecutor<T> of(EntityManager, Class<T>, String idAttribute)` | 指定主键属性 |
| `logicDelete(boolean enabled)` | 控制是否自动追加逻辑删除过滤，默认开启 |

## 查询方法

| 方法 | 说明 |
| --- | --- |
| `List<T> findAll(Specification<T> spec)` | 查询列表 |
| `Page<T> findPage(Specification<T> spec, Pageable pageable)` | 分页查询 |
| `Page<T> findPage(Specification<T> spec, Pageable pageable, List<String> fetchAttributes)` | fetch join 分页 |
| `Slice<T> findSlice(Specification<T> spec, Pageable pageable)` | 切片查询 |
| `Stream<T> stream(Specification<T> spec)` | 流式查询，按主键升序 |
| `void forEachBatch(Specification<T> spec, int batchSize, Consumer<List<T>> consumer)` | 按批流式消费，每批后执行 `flush` / `clear` |
| `long count(Specification<T> spec)` | 计数 |
| `boolean exists(Specification<T> spec)` | 判断是否存在，基于 count |

## 流式查询

`stream` 不会一次性把结果读入内存，适合大数据量导出与批处理：

```java
try (Stream<Order> stream = executor.stream(spec)) {
    stream.map(Order::getOrderNo).forEach(this::export);
}
```

- 结果按主键升序排列，顺序稳定。
- 流绑定当前事务，必须在事务内消费完毕，并优先使用 try-with-resources 关闭。
- 逻辑删除、租户与数据权限条件仍会自动追加。
- HQL 与原生 SQL 执行器同样提供 `stream(...)` 重载。

## 分批消费

`forEachBatch` 在流式查询基础上按批聚合结果，每批回调返回后自动执行 `flush` 与 `clear`，因此一级缓存不会随导出总量增长：

```java
executor.forEachBatch(spec, 500, batch -> {
    batch.forEach(this::export);
});
```

- 每批实体在回调返回后被清出持久化上下文，回调外不要继续持有引用。
- 执行器不提交事务，需要在事务范围内调用，由调用方决定提交或回滚。
- 回调抛出异常时执行器会关闭流并原样抛出；需要提前结束遍历时，同样在回调内抛出异常。
- `batchSize` 必须大于 0，回调不能为 `null`。
- HQL 与原生 SQL 执行器同样提供 `forEachBatch(...)` 重载，参数顺序与对应 `stream(...)` 一致，并在末尾追加 `batchSize` 与 `Consumer`。

## 分页与计数

```java
Page<Order> page = executor.findPage(
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        PageRequest.of(0, 10, Sort.by("id").descending())
);
```

计数使用配置的主键属性执行 `countDistinct`，因此连表产生的一对多重复行不会放大总数。

`findSlice` 不执行 count，而是多取一条记录判断是否存在下一页：

```java
Slice<Order> slice = executor.findSlice(spec, PageRequest.of(0, 10));
boolean hasNext = slice.hasNext();
```

## fetch join 分页

直接在分页查询上使用 fetch join，JPA 会把分页放到内存里执行。执行器在传入 `fetchAttributes` 时改走三段式查询：

1. 用 `countDistinct` 统计总数
2. 只查询当前页的主键
3. 按主键 + fetch join 加载实体

```java
Page<Order> page = executor.findPage(
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        PageRequest.of(0, 10, Sort.by("id")),
        List.of("orderProducts")
);
```

三段式逻辑也以静态方法暴露，便于在自定义查询中复用：

```java
Page<Order> page = FetchJoinPaging.findPage(
        entityManager,
        Order.class,
        "id",
        SpecWrapper.create(Order.class).like("orderNo", "DS").toSpecification(),
        List.of("orderProducts"),
        PageRequest.of(0, 10, Sort.by("id"))
);
```

## 单条与删除

| 方法 | 说明 |
| --- | --- |
| `T findById(Object id)` | 按主键查询，同样应用逻辑删除过滤，未找到返回 `null` |
| `int deleteById(Object id)` | 逻辑删除优先，实体未标注 `@LogicDelete` 时物理删除 |
| `int deleteAllById(Collection<?> ids)` | 按主键批量删除 |
| `int deleteAll(Collection<T> entities)` | 按实体批量删除 |

```java
Order order = executor.findById(1L);
int affected = executor.deleteById(1L);
```

删除方法的行为取决于实体是否标注 `@LogicDelete`；具体差异见 [逻辑删除](/core/logic-delete)。

## 与 Repository 的关系

执行器直接使用 `EntityManager`，不要求 `Repository` 继承 `JpaSpecificationExecutor`。两种方式可以并存：

- Repository 已有 `JpaSpecificationExecutor` 且不需要 fetch join 分页：直接用 `findAll(spec, pageable)`。
- 需要 `Slice`、`countDistinct`、fetch join 分页：使用执行器。
