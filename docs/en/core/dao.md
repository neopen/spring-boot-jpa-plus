# DAO and CRUD

`core` provides a generic data access abstraction that replaces the `EntityManager` boilerplate repeated across multiple services. It does not replace `JpaRepository`; the two can coexist.

## JpaPlusDao

`io.github.neopen.core.persistence.dao.JpaPlusDao<T, ID>` defines the generic data access contract:

| Method | Description |
| --- | --- |
| `Optional<T> findById(ID id)` | Find by primary key |
| `List<T> findAll()` | Find all, never returns `null` |
| `T save(T entity)` | Save an entity, returns the managed entity |
| `List<T> saveAll(Collection<T> entities)` | Save in batch |
| `void deleteById(ID id)` | Delete by primary key, skipped when `id` is `null` |
| `void delete(T entity)` | Delete an entity, skipped when `entity` is `null` |
| `void deleteAll(Collection<T> entities)` | Delete in batch, skipped for an empty collection |
| `boolean existsById(ID id)` | Check whether the entity exists |
| `long count()` | Count all records |
| `Page<T> findPage(Pageable pageable)` | Paginate without additional conditions |
| `List<T> findList(QueryWrapper<T> wrapper)` | Query a list by dynamic conditions, all records when `wrapper` is `null` |
| `Page<T> findPage(QueryWrapper<T> wrapper, Pageable pageable)` | Paginate by dynamic conditions |

## AbstractJpaPlusDao

`AbstractJpaPlusDao` is an abstract implementation based on `EntityManager`:

- The class declares `@Transactional(readOnly = true)`, and write methods re-declare their transaction semantics, so the bean must be managed by the Spring container.
- The constructor validates `EntityManager`, the entity type and the batch size, and throws `IllegalArgumentException` for illegal arguments.
- Two constructors: `(EntityManager, Class<T>)` uses the default batch size of 500; `(EntityManager, Class<T>, int batchSize)` lets you specify it.

```java
@Repository
public class OrderDao extends AbstractJpaPlusDao<Order, Long> {

    public OrderDao(EntityManager entityManager) {
        super(entityManager, Order.class);
    }
}
```

Paginate by dynamic conditions:

```java
QueryWrapper<Order> wrapper = new QueryWrapper<Order>()
        .like("orderNo", "DS")
        .eq("status", "PAID")
        .between("totalAmount", start, end);

Page<Order> page = orderDao.findPage(wrapper, PageRequest.of(0, 10));
```

## Division of Labor with Repository

| Scenario | Recommendation |
| --- | --- |
| Standard CRUD, derived queries | Keep using `JpaRepository` |
| Dynamic composition by business conditions | Use `JpaPlusDao` + `QueryWrapper`, or `SpecWrapper` |
| Hand-written SQL / HQL statements | Use `JpaPlusHqlExecutor` / `JpaPlusSqlExecutor` |
| Auditing, logical deletion | See [Auditing Fields](/en/core/auditing) and [Logical Deletion](/en/core/logic-delete) |

Note: the delete methods of `AbstractJpaPlusDao` perform physical deletion and are not automatically converted to logical deletion; for logical deletion use `LogicDeletes` or `JpaPlusSpecExecutor`.
