# 逻辑删除

逻辑删除通过实体字段注解实现：标注字段为 `false` 表示未删除，查询侧统一追加过滤，写侧提供标记方法。

## 声明

```java
@Entity
public class Order {

    @LogicDelete
    private Boolean deleted = Boolean.FALSE;
}
```

| 约束 | 说明 |
| --- | --- |
| 字段类型 | `Boolean` 或 `boolean` |
| 未删除值 | `false` |
| 已删除值 | `true` |
| 继承 | 注解可从父类继承，元数据按实体类型缓存 |

## 查询过滤

```java
Specification<Order> spec = SpecWrapper.create(Order.class)
        .filterDeleted()
        .like("orderNo", "DS")
        .toSpecification();

// 或与已有 Specification 组合
Specification<Order> filtered = LogicDeletes.filter(Order.class, spec);
```

`JpaPlusSpecExecutor` 默认自动追加逻辑删除过滤，可通过 `logicDelete(false)` 关闭。

## 写侧删除

```java
int affected = LogicDeletes.deleteById(entityManager, Order.class, 1L);
int batch = LogicDeletes.deleteAllById(entityManager, Order.class, ids);
int marked = LogicDeletes.deleteAll(entityManager, Order.class, orders);
```

| 方法 | 行为 |
| --- | --- |
| `deleteById(EntityManager, Class<T>, Object id)` | 执行 JPQL 更新，把逻辑删除字段置为 `true` |
| `deleteAllById(EntityManager, Class<T>, Collection<?> ids)` | 同上，批量主键 |
| `deleteAll(EntityManager, Class<T>, Collection<T> entities)` | 直接修改受管实体字段，由持久化上下文在提交时同步 |

差异：

- `deleteById` / `deleteAllById` 走批量更新语句，**不会触发实体生命周期回调**，也不刷新已加载的实体状态。
- `deleteAll` 要求实体处于托管状态，改的是内存中的字段值。
- 实体未标注 `@LogicDelete` 时，三个方法都抛 `IllegalArgumentException`。

## 全局开关

```java
LogicDeletes.setEnabled(false);   // 维护任务需要读取已删除数据时
```

```yaml
neopen:
  jpa:
    logic-delete:
      enabled: false
```

- 开关只影响**查询过滤**，写侧方法不受影响。
- 也可以通过 `LogicDeletes.isEnabled()` 读取当前状态。
- 可用 `LogicDeletes.isLogicDeleted(Order.class)` 判断实体是否声明了逻辑删除字段。

多数据源场景可以按作用域单独关闭过滤，不影响主库：

```java
LogicDeletes.setEnabled("secondary", false);

JpaPlusScope.run("secondary", () -> {
    // 这里查询不过滤已删除数据
    secondaryRepository.findAll(spec);
});
```

对应的配置项为 `neopen.jpa.scopes.<name>.logic-delete-enabled`，详见[配置项](/reference/configuration)。

## 适用边界

- 过滤只对 JPA Criteria 查询生效，包括 `SpecWrapper`、`JpaPlusSpecExecutor`。
- 原生 SQL 不会自动追加条件，需要在语句中自行处理。
- HQL 构建器同样不会自动拼接删除条件，需要显式写 `eq("deleted", false)`。
- `AbstractJpaPlusDao` 的删除方法执行物理删除，与逻辑删除无关。
