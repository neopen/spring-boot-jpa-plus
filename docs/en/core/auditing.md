# Auditing Fields

Auditing fields are automatically populated by entity listeners during persistence, so you do not need to write `setCreateTime` manually in every Service.

## Declaration

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

| Annotation | Write timing |
| --- | --- |
| `@CreatedAt` | Insert only |
| `@CreatedBy` | Insert only |
| `@UpdatedAt` | Insert and update |
| `@UpdatedBy` | Insert and update |

## Supported Time Types

`@CreatedAt` / `@UpdatedAt` support:

- `Instant`
- `LocalDateTime`
- `LocalDate`
- `OffsetDateTime`
- `ZonedDateTime`
- `java.util.Date`
- `long` (epoch milliseconds)

Unsupported types fail immediately during the first entity scan instead of being skipped silently.

## Runtime Configuration

```java
Audits.setClock(Clock.systemDefaultZone());
Audits.setAuditorSupplier(() -> currentUserId());
```

| Method | Description |
| --- | --- |
| `Audits.setEnabled(boolean)` / `isEnabled()` | Enable or disable auditing population |
| `Audits.setClock(Clock)` | Set the clock, which affects the values of time fields |
| `Audits.setAuditorSupplier(Supplier<String>)` | Set the source of the auditor |
| `Audits.isAudited(Class<?>)` | Determine whether an entity contains auditing fields |

Conventions:

- When `auditorSupplier` returns `null`, the original field value is not overwritten, which preserves manual assignments in scenarios without a login context.
- The clock and auditor only need to be configured once at startup.

## Configuration Properties

```yaml
neopen:
  jpa:
    audit:
      enabled: true
      zone: Asia/Shanghai
      auditor: system
```

| Property | Default value | Description |
| --- | --- | --- |
| `neopen.jpa.audit.enabled` | `true` | Whether auditing population is enabled |
| `neopen.jpa.audit.zone` | `UTC` | Time zone used when writing fields such as `LocalDateTime` and `ZonedDateTime` |
| `neopen.jpa.audit.auditor` | Empty | Fixed auditor name; when empty, nothing is written |

Properties that are not configured do not override values set through `Audits` in code.

## Applicable Boundaries

- It relies on JPA entity lifecycle callbacks, so batch update statements (such as `LogicDeletes.deleteAllById`) do not trigger it.
- It only handles fields declared with annotations and does not automatically recognize fields with the same name in `BaseEntity` unless the field actually carries the annotation.
- Auditing is write-only and provides no history table; for change tracking, combine it with business logs.
