# 审计字段

审计字段由实体监听器在持久化时自动填充，不需要在每个 Service 里手动写 `setCreateTime`。

## 声明

```java
@Entity
@EntityListeners(AuditListener.class)
public class Order {

    @CreatedAt
    private Instant createdAt;

    @UpdatedAt
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @UpdatedBy
    private String updatedBy;
}
```

| 注解 | 写入时机 |
| --- | --- |
| `@CreatedAt` | 仅插入 |
| `@CreatedBy` | 仅插入 |
| `@UpdatedAt` | 插入与更新 |
| `@UpdatedBy` | 插入与更新 |

## 支持的时间类型

`@CreatedAt` / `@UpdatedAt` 支持：

- `Instant`
- `LocalDateTime`
- `LocalDate`
- `OffsetDateTime`
- `ZonedDateTime`
- `java.util.Date`
- `long`（epoch 毫秒）

不支持的类型会在首次扫描实体时直接失败，不会静默跳过。

## 运行时配置

```java
Audits.setClock(Clock.systemDefaultZone());
Audits.setAuditorSupplier(() -> currentUserId());
```

| 方法 | 说明 |
| --- | --- |
| `Audits.setEnabled(boolean)` / `isEnabled()` | 启用或停用审计填充 |
| `Audits.setClock(Clock)` | 设置时钟，影响时间字段取值 |
| `Audits.setAuditorSupplier(Supplier<String>)` | 设置操作人来源 |
| `Audits.isAudited(Class<?>)` | 判断实体是否包含审计字段 |

约定：

- `auditorSupplier` 返回 `null` 时不覆盖字段原值，便于在无登录态的场景保留手工赋值。
- 时钟与操作人只需在启动阶段配置一次。

## 配置项

```yaml
neopen:
  jpa:
    audit:
      enabled: true
      zone: Asia/Shanghai
      auditor: system
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.audit.enabled` | `true` | 是否启用审计填充 |
| `neopen.jpa.audit.zone` | `UTC` | 写入 `LocalDateTime`、`ZonedDateTime` 等字段时使用的时区 |
| `neopen.jpa.audit.auditor` | 空 | 固定的操作人名称，为空时不写入 |

未配置的属性不会覆盖代码中通过 `Audits` 设置的值。

## 适用边界

- 依赖 JPA 实体生命周期回调，批量更新语句（如 `LogicDeletes.deleteAllById`）不会触发。
- 只处理注解声明的字段，不会自动识别 `BaseEntity` 中的同名字段，除非字段上确实有注解。
- 审计只负责写入，不提供历史记录表；需要变更留痕时应结合业务日志。
