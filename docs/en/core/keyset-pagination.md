# Keyset Pagination

`KeysetPageParams`, `KeysetCursor`, and `KeysetPage` provide keyset pagination. Instead of `OFFSET`, the query starts from the sort values and primary key of the last row on the previous page, generating a `(sort_key, id) > (?, ?)` condition. It fits deep pagination, infinite scrolling, and large exports.

```java
KeysetPageParams params = KeysetPageParams.builder()
        .size(20)
        .sort(Sort.by("createTime").descending())
        .build();

KeysetPage<Order> page = executor.keyset(spec, params);

if (page.hasNext()) {
    String cursor = page.getNextCursor().encode();
}
```

## Paging Flow

The server issues the cursor and the client sends it back unchanged. The first page has no cursor:

```java
KeysetPageParams first = KeysetPageParams.builder()
        .size(20)
        .sort(Sort.by("createTime").descending())
        .build();

KeysetPage<Order> page = executor.keyset(spec, first);

KeysetPageParams next = KeysetPageParams.builder()
        .size(20)
        .sort(Sort.by("createTime").descending())
        .cursor(KeysetCursor.decode(page.getNextCursor().encode()))
        .build();

KeysetPage<Order> second = executor.keyset(spec, next);
```

When `KeysetPage#hasNext()` is `false`, `getNextCursor()` returns `null`; no page number or total count is needed.

## Types and Methods

`KeysetPageParams`

| Method | Description |
| --- | --- |
| `builder()` | Full configuration |
| `static KeysetPageParams of(int size, Sort sort)` | First-page parameters |
| `getSize()` | Page size, default 10, max 500 |
| `getSort()` | Sort rule, must not be empty |
| `getCursor()` | Previous-page cursor, `null` on the first page |
| `isFirst()` | Whether this is the first page |

`KeysetCursor`

| Method | Description |
| --- | --- |
| `static KeysetCursor of(Object... values)` | Create from sort values |
| `encode()` | Produce a URL-safe string for query parameters |
| `static KeysetCursor decode(String encoded)` | Restore a cursor; values are strings |
| `getValues()` | Immutable list of values |

`KeysetPage`

| Method | Description |
| --- | --- |
| `getContent()` | Current page data |
| `getNextCursor()` | Cursor for the next page, `null` if none |
| `hasNext()` | Whether another page exists |

## Sort Rules

- The sort must not be empty. The executor appends the primary key as a tiebreaker; it is not appended twice when already present.
- `ignoreCase` and `NULLS_FIRST` / `NULLS_LAST` are rejected with `IllegalArgumentException`: such ordering cannot be expressed with a simple comparison.
- The number of cursor values must match the number of sort items (including the appended primary key), otherwise `IllegalArgumentException` is thrown.

## Supported Value Types

A decoded cursor contains strings; the executor converts them according to the sort property type:

`String`, `Integer` / `Long` / `Short` / `Byte`, `Double` / `Float`, `BigDecimal` / `BigInteger`, `Boolean`, `UUID`, `LocalDate`, `LocalDateTime`, `LocalTime`, `Instant`, and enums.

Other types (for example `java.util.Date`) raise `IllegalArgumentException`. Use one of the types above for sort keys.

## Limitations

- Only single-column primary keys are supported; entities with `@IdClass` or `@EmbeddedId` composite keys are rejected.
- Sort values in a cursor must not be `null`; avoid nullable sort keys.
- The filter chain matches offset pagination: logical deletion, tenant, then data permission filters.
- A cursor carries no query conditions; do not reuse an old cursor after changing sort or filters.

## Compared with Offset Pagination

| Aspect | Offset (`PageParams`) | Keyset (`KeysetPageParams`) |
| --- | --- | --- |
| Navigation | Page number + size | Previous-page cursor |
| Total count | Requires `count` | No `count` |
| Deep pagination | Degrades with offset | Stable, index-dependent |
| Concurrent inserts/deletes | May duplicate or skip rows | Stable relative to cursor |
| Random page access | Supported | Not supported |

Use `PageParams` for admin lists that need random page access, and keyset pagination for infinite scrolling, exports, and deep pagination.
