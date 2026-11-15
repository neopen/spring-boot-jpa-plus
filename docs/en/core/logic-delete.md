# Logical Deletion

Logical deletion is implemented through an entity field annotation: the annotated field being `false` means not deleted, the query side appends the filter uniformly, and the write side provides marking methods.

## Declaration

```java
@Entity
public class Order {

    @LogicDelete
    private Boolean deleted = Boolean.FALSE;
}
```

| Constraint | Description |
| --- | --- |
| Field type | `Boolean` or `boolean` |
| Not deleted value | `false` |
| Deleted value | `true` |
| Inheritance | The annotation can be inherited from a parent class; metadata is cached by entity type |

## Query Filtering

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .filterDeleted()
        .like("orderNo", "DS")
        .toSpecification();

// 或与已有 Specification 组合
Specification<Order> filtered = LogicDeletes.filter(Order.class, spec);
```

`JpaPlusSpecExecutor` appends the logical deletion filter automatically by default, which can be turned off with `logicDelete(false)`.

## Write-Side Deletion

```java
int affected = LogicDeletes.deleteById(entityManager, Order.class, 1L);
int batch = LogicDeletes.deleteAllById(entityManager, Order.class, ids);
int marked = LogicDeletes.deleteAll(entityManager, Order.class, orders);
```

| Method | Behavior |
| --- | --- |
| `deleteById(EntityManager, Class<T>, Object id)` | Executes a JPQL update setting the logical deletion field to `true` |
| `deleteAllById(EntityManager, Class<T>, Collection<?> ids)` | Same as above, with batch primary keys |
| `deleteAll(EntityManager, Class<T>, Collection<T> entities)` | Modifies the managed entity fields directly; the persistence context synchronizes them on commit |

Differences:

- `deleteById` / `deleteAllById` use bulk update statements, **do not trigger entity lifecycle callbacks**, and do not refresh the state of already loaded entities.
- `deleteAll` requires the entities to be in a managed state and changes the field values in memory.
- When the entity is not annotated with `@LogicDelete`, all three methods throw `IllegalArgumentException`.

## Global Switch

```java
LogicDeletes.setEnabled(false);   // 维护任务需要读取已删除数据时
```

```yaml
neopen:
  jpa:
    logic-delete:
      enabled: false
```

- The switch only affects **query filtering**; write-side methods are unaffected.
- The current state can also be read with `LogicDeletes.isEnabled()`.
- `LogicDeletes.isLogicDeleted(Order.class)` can be used to check whether an entity declares a logical deletion field.

## Applicable Boundaries

- Filtering only takes effect for JPA Criteria queries, including `SpecWrapper` and `JpaPlusSpecExecutor`.
- Native SQL does not append conditions automatically; handle it in the statements yourself.
- The HQL builder likewise does not append deletion conditions automatically; write `eq("deleted", false)` explicitly.
- The delete methods of `AbstractJpaPlusDao` perform physical deletion and are unrelated to logical deletion.
