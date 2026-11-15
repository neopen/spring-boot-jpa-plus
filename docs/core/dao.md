# DAO 与 CRUD

`core` 提供通用数据访问抽象，用于替代在多个 Service 中重复出现的 `EntityManager` 样板代码。它不替代 `JpaRepository`，两者可以并存。

## JpaPlusDao

`io.github.neopen.core.persistence.dao.JpaPlusDao<T, ID>` 定义通用数据访问契约：

| 方法 | 说明 |
| --- | --- |
| `Optional<T> findById(ID id)` | 按主键查询 |
| `List<T> findAll()` | 查询全部，不会返回 `null` |
| `T save(T entity)` | 保存实体，返回受管实体 |
| `List<T> saveAll(Collection<T> entities)` | 批量保存 |
| `void deleteById(ID id)` | 按主键删除，`id` 为 `null` 时不执行 |
| `void delete(T entity)` | 删除实体，`entity` 为 `null` 时不执行 |
| `void deleteAll(Collection<T> entities)` | 批量删除，空集合不执行 |
| `boolean existsById(ID id)` | 判断是否存在 |
| `long count()` | 统计总数 |
| `Page<T> findPage(Pageable pageable)` | 无附加条件分页 |
| `List<T> findList(QueryWrapper<T> wrapper)` | 按动态条件查询列表，`wrapper` 为 `null` 时查询全部 |
| `Page<T> findPage(QueryWrapper<T> wrapper, Pageable pageable)` | 按动态条件分页 |

## AbstractJpaPlusDao

`AbstractJpaPlusDao` 是基于 `EntityManager` 的抽象实现：

- 类上声明 `@Transactional(readOnly = true)`，写操作方法重新声明事务语义，因此需要由 Spring 容器管理 Bean。
- 构造器校验 `EntityManager`、实体类型与批次大小，非法参数直接抛 `IllegalArgumentException`。
- 两个构造器：`(EntityManager, Class<T>)` 使用默认批次大小 500；`(EntityManager, Class<T>, int batchSize)` 可指定。

```java
@Repository
public class OrderDao extends AbstractJpaPlusDao<Order, Long> {

    public OrderDao(EntityManager entityManager) {
        super(entityManager, Order.class);
    }
}
```

按动态条件分页：

```java
QueryWrapper<Order> wrapper = new QueryWrapper<Order>()
        .like("orderNo", "DS")
        .eq("status", "PAID")
        .between("totalAmount", start, end);

Page<Order> page = orderDao.findPage(wrapper, PageRequest.of(0, 10));
```

## 与 Repository 的分工

| 场景 | 建议 |
| --- | --- |
| 标准 CRUD、派生查询 | 继续使用 `JpaRepository` |
| 需要按业务条件动态拼接 | 使用 `JpaPlusDao` + `QueryWrapper`，或 `SpecWrapper` |
| 需要 SQL / HQL 手写语句 | 使用 `JpaPlusHqlExecutor` / `JpaPlusSqlExecutor` |
| 需要审计、逻辑删除 | 见 [审计字段](/core/auditing) 与 [逻辑删除](/core/logic-delete) |

注意：`AbstractJpaPlusDao` 的删除方法执行的是物理删除，不会自动转换为逻辑删除；逻辑删除请使用 `LogicDeletes` 或 `JpaPlusSpecExecutor`。
