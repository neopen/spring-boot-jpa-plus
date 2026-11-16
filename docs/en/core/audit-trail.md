# Change History

Change history produces field-level before/after records when entities are created, updated, or deleted. Records are handed to an `AuditLogPublisher` for persistence, messaging, or an audit system.

## Declaring an Entity

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

| Element | Purpose |
| --- | --- |
| `@Audited` | Marks the entity as change-history enabled |
| `@EntityListeners(AuditTrailListener.class)` | Registers lifecycle callbacks that capture snapshots and publish records |

Both are required: the listener alone records nothing, and `@Audited` alone triggers no callbacks.

## Implementing a Publisher

```java
@Bean
public AuditLogPublisher auditLogPublisher(AuditLogRepository repository) {
    return entry -> repository.save(AuditLogEntity.from(entry));
}
```

Writing to the database can be slow; use the built-in asynchronous publisher so the business transaction is not blocked:

```java
@Bean(destroyMethod = "close")
public AuditLogPublisher auditLogPublisher(AuditLogRepository repository) {
    return new AsyncAuditLogPublisher(
            entry -> repository.save(AuditLogEntity.from(entry)),
            1024
    );
}
```

- The queue is bounded; when full, new records are discarded with a WARNING log and the business thread never blocks.
- A single background thread is used by default so records keep their production order; add threads in the constructor for throughput, at the cost of ordering.
- On shutdown `close()` waits for the queue to drain and interrupts the background thread after the timeout.
- `discardedCount()` reports discarded records and `queuedCount()` shows the backlog.

`AuditEntry` fields:

| Field | Type | Description |
| --- | --- | --- |
| `entityName` | `String` | Simple entity class name |
| `entityType` | `Class<?>` | Entity type |
| `entityId` | `Object` | Primary key, available after `@PostPersist` |
| `action` | `AuditAction` | `CREATE`, `UPDATE`, `DELETE` |
| `changes` | `List<AuditChange>` | Field-level changes |
| `actor` | `String` | Actor from the current `Audits` auditor |
| `occurredAt` | `Instant` | Record creation time |

`AuditChange` is `record(field, before, after)`: creates have only `after`, deletes have only `before`, updates have both.

## Change Details

| Action | Recorded content |
| --- | --- |
| `CREATE` | All recordable fields with `before` set to `null` |
| `UPDATE` | Only fields whose values differ |
| `DELETE` | The snapshot before deletion with `after` set to `null` |

Recordable fields include primitives, enums, `String`, `Boolean`, `Character`, `UUID`, `Number`, `Date` and `java.time` types; `static`, `transient` and `@Transient` fields are skipped.

The actor comes from `Audits.currentAuditor()`, sharing `AuditorProvider` with the audit fields; `neopen.jpa.audit.auditor` also applies.

## Snapshots and Updates

- A snapshot is captured on `@PostLoad`; `@PreUpdate` compares against it to build the UPDATE record.
- Snapshots are stored in a weak, **identity-based** map and are reclaimed with the entity, so field changes never break lookup.
- Entities merged from a detached state have no snapshot; `@PreUpdate` only records the current values. Query the entity before modifying it when an exact before/after diff is required.
- Bulk JPQL updates and deletes do not trigger callbacks and produce no records.

## Switches and Scopes

```yaml
neopen:
  jpa:
    audit:
      enabled: true          # audit field filling
      trail-enabled: true    # change history
    scopes:
      import-job:
        trail-enabled: false
```

| Property | Default | Description |
| --- | --- | --- |
| `neopen.jpa.audit.trail-enabled` | `true` | Whether change history is published |
| `neopen.jpa.scopes.<name>.trail-enabled` | empty | Whether the scope publishes change history |

Programmatically:

```java
AuditTrails.setEnabled(false);
AuditTrails.setEnabled("import-job", false);
AuditTrails.setPublisher(entry -> auditStore.add(entry));              // global
AuditTrails.setPublisher("archive", entry -> archiveStore.add(entry)); // scope
AuditTrails.clearScope("import-job");
```

`trail-enabled` and `audit.enabled` are independent: the former controls change history, the latter controls created/updated by and at fields.

## Notes

- Publishing failures are logged as WARNING and do not roll back the business transaction; throw inside the publisher and manage the transaction boundary yourself when strong consistency is required.
- Publishing happens inside persistence callbacks and shares the business transaction; keep publishers lightweight (enqueue or write asynchronously) to avoid slowing the main flow.
- Snapshots live in memory, so entities deleted without being loaded cannot produce DELETE records; use managed entities or audit those cases yourself.
