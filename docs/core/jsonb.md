# PostgreSQL JSONB

PostgreSQL 的 `jsonb` 适合存放结构不固定的扩展字段。`spring-boot-jpa-plus` 不需要额外封装，直接使用 Hibernate 6 的 JSON 类型映射即可；本页给出推荐写法与常见查询方式。

## 实体映射

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

也可以映射为自定义类型：

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "attributes", columnDefinition = "jsonb")
private DocumentAttributes attributes;
```

| 约束 | 说明 |
| --- | --- |
| 数据库 | 仅 PostgreSQL 支持 `jsonb`，H2、MySQL 需要替换为各自的 JSON 类型 |
| 序列化 | 类路径存在 Jackson 时，Hibernate 自动使用 Jackson 处理 JSON |
| 可变对象 | 更新 Map 内部元素不会触发脏检查，整体替换字段或调用 `save` 更可靠 |
| 索引 | 需要按 JSON 字段过滤时，创建 GIN 索引 |

## 表结构

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

## 查询

### 原生 SQL

`->>` 取出文本值，适合等值与模糊匹配：

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

### 写入

```sql
update document_record
set attributes = cast(:payload as jsonb)
where id = :id
```

### Criteria

通过 `function` 调用 PostgreSQL 函数，可以继续复用本项目的过滤链：

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

多租户、数据权限与逻辑删除仍会按原有顺序追加条件。

## 与加密、脱敏的关系

- `@EncryptedField` 作用于字符串字段，不建议直接标注在 `jsonb` 字段上；需要加密时先序列化为字符串再加密。
- 脱敏在序列化阶段处理，对 `jsonb` 内的嵌套字段不生效，需要在 DTO 层转换。
- 变更历史记录的是字段级前后值，`jsonb` 字段会作为整体比较，Map 顺序变化也会被视为变更。

## 适用边界

- `jsonb` 过滤依赖数据库函数，无法被 Hibernate 的 SQL 生成器优化，复杂查询建议直接写原生 SQL。
- 高频过滤的字段建议建表达式索引，例如 `((attributes ->> 'status'))`。
- 单元测试通常使用 H2，遇到 `jsonb` 语法错误时请改用 PostgreSQL 容器或关闭相关测试。
