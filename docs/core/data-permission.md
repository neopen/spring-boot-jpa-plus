# 行级数据权限

行级数据权限把「当前用户能看到哪些行」抽象为字段集合规则，由 `DataScopes` 统一转换为查询条件并与其他条件按 `AND` 合并。

## 定义提供者

```java
@Bean
public DataPermissionProvider dataPermissionProvider() {
    return entityType -> {
        if (!DocumentRecord.class.equals(entityType)) {
            return List.of();
        }

        Set<Long> deptIds = SecurityContext.visibleDeptIds();

        return deptIds.isEmpty()
                ? List.of()
                : List.of(DataRule.of("deptId", deptIds.toArray()));
    };
}
```

`DataRule` 表示「字段值必须落在给定集合内」：

```java
DataRule rule = new DataRule("deptId", List.of(1L, 2L));
DataRule same = DataRule.of("deptId", 1L, 2L);

// 关联路径：按部门的上级部门过滤
DataRule nested = DataRule.of("dept.parentId", 1L);

// OR 组合：本部门或本人负责
List<DataRule> rules = List.of(
        DataRule.of("deptId", 1L, 2L),
        DataRule.or("ownerId", 9L)
);
// deptId in (1,2) or ownerId in (9)
```

| 约束 | 说明 |
| --- | --- |
| `field` | 实体字段名，支持 `dept.parentId` 形式的关联路径，不能为空白 |
| `values` | 允许值集合，不能为空 |
| `logic` | 与上一条规则的组合方式，默认 `AND`，可用 `DataRule.or(...)` 改为 `OR` |
| 空规则 | 返回 `null` 或空列表表示该实体不做限制 |

多条规则按声明顺序从左到右组合，`a OR b AND c` 等价于 `(a OR b) AND c`。需要更复杂的优先级时，请在提供者中把条件换算为字段值集合。

提供者抛出异常会中断查询，请在提供者内部处理降级逻辑。返回空集合表示**不做限制**，与「无权限」不同；需要拒绝全部数据时请返回一个不可能匹配的规则，或在业务层直接抛错。

## 查询过滤

`JpaPlusSpecExecutor` 会自动追加权限条件，顺序为逻辑删除 → 租户 → 数据权限：

```java
Specification<DocumentRecord> spec =
        (root, query, cb) -> cb.equal(root.get("docNo"), "DOC-001");

Page<DocumentRecord> page = JpaPlusSpecExecutor
        .of(entityManager, DocumentRecord.class)
        .findPage(spec, pageable);
// where deleted = false and tenant_id = ? and dept_id in (?, ?)
```

手动组合：

```java
Specification<DocumentRecord> scoped =
        DataScopes.filter(DocumentRecord.class, spec);
```

| 方法 | 说明 |
| --- | --- |
| `DataScopes.filter(Class<T>)` | 仅返回权限条件 |
| `DataScopes.filter(Class<T>, Specification<T>)` | 与原条件按 `AND` 合并 |
| `DataScopes.isActive()` | 当前是否有生效的提供者 |
| `DataScopes.setProvider(...)` | 代码方式设置提供者 |

## 作用域覆盖

```java
DataScopes.setProvider("batch-job", entityType -> List.of());
DataScopes.setEnabled("batch-job", false);

JpaPlusScope.run("batch-job", () -> documentRepository.findAll(spec));
```

- 作用域提供者优先于全局提供者。
- 关闭开关后即使提供者存在也不会追加条件。

全局开关：

```yaml
neopen:
  jpa:
    data-scope:
      enabled: true
```

`neopen.jpa.scopes` 当前没有数据权限相关字段，作用域级开关与提供者需要通过 `DataScopes.setEnabled(scope, false)`、`DataScopes.setProvider(scope, provider)` 在代码中注册。

## 组合顺序

| 顺序 | 过滤 | 开关 |
| --- | --- | --- |
| 1 | 逻辑删除 | `@LogicDelete`、`neopen.jpa.logic-delete.enabled` |
| 2 | 多租户 | `@TenantId`、`neopen.jpa.tenant.enabled` |
| 3 | 行级数据权限 | `DataPermissionProvider`、`neopen.jpa.data-scope.enabled` |

三层条件都通过 `Specification.and(...)` 合并，任意一层未启用或没有数据时不影响其他层。

## 临时跳过

后台统计、数据修复等场景可以按方法跳过权限条件：

```java
@IgnoreDataScope
public long countAllOrders() {
    return orderRepository.count();
}
```

嵌套调用按层恢复，也可以使用 `DataScopes.runIgnored(...)` / `callIgnored(...)` 在代码中临时跳过。详见 [过滤跳过](/core/filter-ignore)。

## 适用边界

- 只覆盖 JPA Criteria 查询；`HqlWrapper`、`SqlWrapper` 与原生 SQL 需要自行追加条件。
- 规则支持 `字段 IN 集合`、关联路径与 `OR` 组合，但不支持子查询；复杂权限建议在提供者中换算为字段值集合。
- 批量 JPQL 更新/删除不经过执行器，`LogicDeletes`、`BatchOperation` 等方法不会自动附加权限条件，调用前需要自行校验数据范围。
