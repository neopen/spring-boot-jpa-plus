# 多租户

多租户通过实体字段注解 + 租户提供者实现：写入时自动填充租户字段，查询时自动追加租户条件，业务代码无需显式传租户。

## 声明

```java
@Entity
@EntityListeners(TenantListener.class)
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "doc_no", nullable = false, unique = true)
    private String docNo;
}
```

| 约束 | 说明 |
| --- | --- |
| 字段类型 | `String`、`Long`、`long`、`Integer`、`int`、`UUID` |
| 状态 | 字段不需要单独初始化，监听器只在字段为空时写入 |
| 监听器 | 必须声明 `@EntityListeners(TenantListener.class)`，用于写入自动填充 |

字段类型不合法时会在首次解析元数据时抛出 `IllegalArgumentException`。

## 提供当前租户

注册 `TenantProvider` Bean 即可，自动配置会写入 `Tenants`：

```java
@Bean
public TenantProvider tenantProvider() {
    return () -> TenantHolder.current();
}
```

也可以直接设置：

```java
Tenants.setProvider(() -> TenantHolder.current());
```

`TenantProvider#currentTenant` 返回 `null` 或空白字符串时视为没有租户上下文，此时**不做任何过滤**，也不会阻塞写入。需要强制隔离时，请在业务入口保证租户一定存在。

## 查询过滤

`JpaPlusSpecExecutor` 会在逻辑删除过滤之后自动追加租户条件，只对声明了 `@TenantId` 的实体生效：

```java
// 自动追加：where deleted = false and tenant_id = ?
Page<DocumentRecord> page = JpaPlusSpecExecutor
        .of(entityManager, DocumentRecord.class)
        .findPage(spec, pageable);
```

需要手动组合时：

```java
Specification<DocumentRecord> spec = Tenants.filter(
        DocumentRecord.class,
        (root, query, cb) -> cb.equal(root.get("docNo"), "DOC-001")
);
```

| 方法 | 说明 |
| --- | --- |
| `Tenants.filter(Class<T>)` | 仅返回租户条件 |
| `Tenants.filter(Class<T>, Specification<T>)` | 与原条件按 `AND` 合并 |
| `Tenants.isTenantEntity(Class<?>)` | 判断实体是否声明了租户字段 |
| `Tenants.currentTenant()` | 读取当前生效租户，未启用或无上下文返回 `null` |

## 作用域固定租户

归档、运维等场景需要跨租户访问单个租户的数据，可以按作用域固定租户：

```yaml
neopen:
  jpa:
    scopes:
      archive-view:
        tenant: demo-tenant
```

```java
JpaPlusScope.run("archive-view", () -> {
    // 这里固定使用 demo-tenant，忽略全局租户提供者
    documentRepository.findAll(spec);
});
```

也可以调用 `Tenants.setTenant("archive-view", "demo-tenant")`，传 `null` 清除。

解析优先级：作用域固定租户 > 作用域提供者 > 全局提供者。

## 配置与开关

```yaml
neopen:
  jpa:
    tenant:
      enabled: true
    scopes:
      report:
        tenant-enabled: false
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.tenant.enabled` | `true` | 是否启用租户过滤 |
| `neopen.jpa.scopes.<name>.tenant-enabled` | 空 | 该作用域是否启用租户过滤 |
| `neopen.jpa.scopes.<name>.tenant` | 空 | 该作用域固定租户 |

关闭过滤只影响查询，`TenantListener` 仍会在有租户上下文时填充字段。

## 适用边界

- 自动过滤只覆盖 JPA Criteria 查询，包括 `SpecWrapper`、`JpaPlusSpecExecutor`。
- `HqlWrapper`、`SqlWrapper` 与原生 SQL 不会自动追加租户条件，需要显式书写。
- 租户字段只保证查询隔离；跨租户唯一约束需要在数据库层面把租户字段纳入唯一索引。
- 批量 JPQL 更新/删除不会触发监听器，写入租户请使用托管实体。
