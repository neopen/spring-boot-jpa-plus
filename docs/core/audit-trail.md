# 变更历史

变更历史在实体新增、修改、删除时生成字段级的前后对比记录，通过 `AuditLogPublisher` 交给业务落库、写消息或推送到审计系统。

## 声明实体

```java
@Entity
@Audited
@EntityListeners(AuditTrailListener.class)
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;
}
```

| 元素 | 作用 |
| --- | --- |
| `@Audited` | 标记需要记录变更历史的实体 |
| `@EntityListeners(AuditTrailListener.class)` | 注册生命周期回调，负责采集快照与发布记录 |

两个注解缺一不可：只加监听器不会记录，只加 `@Audited` 不会触发回调。

## 实现发布者

```java
@Bean
public AuditLogPublisher auditLogPublisher(AuditLogRepository repository) {
    return entry -> repository.save(AuditLogEntity.from(entry));
}
```

写入数据库可能较慢，可以使用内置的异步发布者，避免阻塞业务事务：

```java
@Bean(destroyMethod = "close")
public AuditLogPublisher auditLogPublisher(AuditLogRepository repository) {
    return new AsyncAuditLogPublisher(
            entry -> repository.save(AuditLogEntity.from(entry)),
            1024
    );
}
```

- 队列有界，满时丢弃新记录并输出 WARNING 日志，不阻塞业务线程。
- 默认单个后台线程，保证记录顺序与产生顺序一致；需要吞吐量时可以在构造器中增加线程数，但顺序不再有保证。
- 容器关闭时调用 `close()` 等待队列处理完毕，超时后中断后台线程。
- `discardedCount()` 查看丢弃数量，`queuedCount()` 查看积压情况。

`AuditEntry` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `entityName` | `String` | 实体简单类名 |
| `entityType` | `Class<?>` | 实体类型 |
| `entityId` | `Object` | 主键值，`@PostPersist` 之后可读取 |
| `action` | `AuditAction` | `CREATE`、`UPDATE`、`DELETE` |
| `changes` | `List<AuditChange>` | 字段变更明细 |
| `actor` | `String` | 操作人，来自 `Audits` 当前操作人 |
| `occurredAt` | `Instant` | 记录生成时间 |

`AuditChange` 为 `record(field, before, after)`：新增只有 `after`，删除只有 `before`，修改两者都有。

## 变更明细

| 动作 | 记录内容 |
| --- | --- |
| `CREATE` | 所有可记录字段，`before` 为 `null` |
| `UPDATE` | 仅记录前后值不同的字段 |
| `DELETE` | 删除前的字段快照，`after` 为 `null` |

可记录字段包括基本类型、枚举、`String`、`Boolean`、`Character`、`UUID`、`Number`、`Date` 与 `java.time` 类型；`static`、`transient`、`@Transient` 字段会被跳过。

操作人取自 `Audits.currentAuditor()`，与审计字段共用 `AuditorProvider`，也可以配置 `neopen.jpa.audit.auditor`。

## 快照与更新

- 实体加载时（`@PostLoad`）保存字段快照，修改提交前（`@PreUpdate`）与快照对比生成 UPDATE 记录。
- 快照按**对象身份**存放在弱引用表中，随实体回收；字段变化不会影响快照查找。
- 通过 `merge` 合并的游离实体没有快照，`@PreUpdate` 只会记下当前值对应的改动；需要精确前后对比时，请先查询实体再修改。
- 批量 JPQL 更新与删除不触发实体回调，不会产生记录。

## 开关与作用域

```yaml
neopen:
  jpa:
    audit:
      enabled: true          # 审计字段填充
      trail-enabled: true    # 变更历史
    scopes:
      import-job:
        trail-enabled: false
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `neopen.jpa.audit.trail-enabled` | `true` | 是否发布变更历史 |
| `neopen.jpa.scopes.<name>.trail-enabled` | 空 | 该作用域是否发布变更历史 |

代码方式：

```java
AuditTrails.setEnabled(false);
AuditTrails.setEnabled("import-job", false);
AuditTrails.setPublisher(entry -> auditStore.add(entry));        // 全局
AuditTrails.setPublisher("archive", entry -> archiveStore.add(entry)); // 作用域
AuditTrails.clearScope("import-job");
```

`trail-enabled` 与 `audit.enabled` 相互独立：前者控制变更历史，后者控制创建/更新人与时间字段。

## 注意事项

- 发布异常只会记录 WARNING 日志，不会回滚业务事务；需要强一致时请在发布者内部抛出并自行处理事务边界。
- 发布发生在持久化回调内，落库操作会与业务共用同一事务；建议发布者只做轻量入队或异步落盘，避免拖慢主流程。
- 快照保存在内存中，未加载就删除的实体无法生成 DELETE 记录；批量删除请使用托管实体或自行审计。
