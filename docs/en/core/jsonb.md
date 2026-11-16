# PostgreSQL JSONB

PostgreSQL `jsonb` is a good fit for dynamic extension fields. `spring-boot-jpa-plus` needs no extra wrapper: use Hibernate 6 JSON mapping directly. This page shows the recommended mapping and query patterns.

## Entity Mapping

```java
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_record")
public class DocumentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_no", nullable = false, unique = true)
    private String docNo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes;
}
```

A custom type works as well:

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "attributes", columnDefinition = "jsonb")
private DocumentAttributes attributes;
```

| Constraint | Description |
| --- | --- |
| Database | `jsonb` is PostgreSQL-only; use the native JSON type on H2 or MySQL |
| Serialization | Hibernate uses Jackson automatically when it is on the classpath |
| Mutability | In-place map changes may not trigger dirty checking; replace the value or call `save` |
| Indexing | Add a GIN index when filtering by JSON fields |

## Schema

```sql
create table document_record (
    id          bigserial primary key,
    doc_no      varchar(64) not null unique,
    attributes  jsonb
);

create index idx_document_attributes
    on document_record
    using gin (attributes jsonb_path_ops);
```

## Queries

### Native SQL

`->>` extracts text values and suits equality or prefix matching:

```java
public interface DocumentRecordRepository
        extends JpaRepository<DocumentRecord, Long> {

    @Query(value = """
            select * from document_record
            where attributes ->> 'status' = :status
            """, nativeQuery = true)
    List<DocumentRecord> findByStatus(@Param("status") String status);
}
```

### Writing

```sql
update document_record
set attributes = cast(:payload as jsonb)
where id = :id
```

### Criteria

Call PostgreSQL functions through `function` and keep using the library filter chain:

```java
Specification<DocumentRecord> spec = (root, query, cb) -> cb.equal(
        cb.function(
                "jsonb_extract_path_text",
                String.class,
                root.get("attributes"),
                cb.literal("status")
        ),
        "ACTIVE"
);

Page<DocumentRecord> page = JpaPlusSpecExecutor
        .of(entityManager, DocumentRecord.class)
        .findPage(spec, Pageable.ofSize(20));
```

Tenant, data permission and logical deletion conditions are still appended in the usual order.

## Interaction With Encryption and Desensitization

- `@EncryptedField` targets string fields; do not annotate a `jsonb` field directly. Serialize first, then encrypt.
- Desensitization runs during serialization and does not reach nested JSON fields; convert through a DTO instead.
- Change history compares fields as a whole, so a `jsonb` map reorder is treated as a change.

## Boundaries

- JSON filters rely on database functions and cannot be optimized by Hibernate's SQL generator; use native SQL for complex queries.
- Add expression indexes for frequently filtered keys, for example `((attributes ->> 'status'))`.
- Unit tests usually run on H2; switch to a PostgreSQL container or disable those tests when `jsonb` syntax fails.
