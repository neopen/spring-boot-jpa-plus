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

## Auditor Source

In a Spring Boot application, register an `AuditorProvider` bean backed by the login context:

```java
@Configuration
public class AuditConfiguration {

    @Bean
    public AuditorProvider auditorProvider() {
        return () -> SecurityContextHolder.currentUsername();
    }
}
```

`AuditorProvider` takes precedence over `neopen.jpa.audit.auditor`: when the bean exists, it is called on every fill, which fits multi-user scenarios. Without the bean, the fixed configured value is used.

Returning `null` or a blank string leaves the auditor field untouched.

## Runtime Configuration

```java
Audits.setClock(Clock.systemDefaultZone());
Audits.setAuditorSupplier(() -> currentUserId());

// Or use the SPI directly
Audits.setAuditorProvider(() -> currentUserId());
```

| Method | Description |
| --- | --- |
| `Audits.setEnabled(boolean)` / `isEnabled()` | Enable or disable auditing population |
| `Audits.setClock(Clock)` | Set the clock, which affects the values of time fields |
| `Audits.setAuditorSupplier(Supplier<String>)` | Set the source of the auditor |
| `Audits.setAuditorProvider(AuditorProvider)` | Set the auditor source through the SPI |
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
| `neopen.jpa.audit.auditor` | Empty | Fixed auditor name; when empty, nothing is written. An `AuditorProvider` bean takes precedence |

Properties that are not configured do not override values set through `Audits` in code.

## Applicable Boundaries

- It relies on JPA entity lifecycle callbacks, so batch update statements (such as `LogicDeletes.deleteAllById`) do not trigger it.
- It only handles fields declared with annotations and does not automatically recognize fields with the same name in `BaseEntity` unless the field actually carries the annotation.
- Auditing is write-only and provides no history table; for change tracking, combine it with business logs.
