# 游标分页

`KeysetPageParams`、`KeysetCursor` 与 `KeysetPage` 提供游标（keyset）分页。它不使用 `OFFSET`，而是以「上一页最后一行的排序键 + 主键」为起点，生成 `(排序键, 主键) > (?, ?)` 条件，适合深翻页、滚动加载与大数据量导出。

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

## 翻页流程

游标由服务端生成、客户端原样带回。第一页不传游标：

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

`KeysetPage#hasNext()` 为 `false` 时 `getNextCursor()` 返回 `null`，不需要再记录页码或总记录数。

## 类型与方法

`KeysetPageParams`

| 方法 | 说明 |
| --- | --- |
| `builder()` | 完整配置 |
| `static KeysetPageParams of(int size, Sort sort)` | 第一页参数 |
| `getSize()` | 每页数量，默认 10，上限 500 |
| `getSort()` | 排序规则，不能为空 |
| `getCursor()` | 上一页游标，第一页为 `null` |
| `isFirst()` | 是否第一页 |

`KeysetCursor`

| 方法 | 说明 |
| --- | --- |
| `static KeysetCursor of(Object... values)` | 用排序键取值直接创建 |
| `encode()` | 输出 URL 安全的字符串，适合放入查询参数 |
| `static KeysetCursor decode(String encoded)` | 还原游标，取值为字符串 |
| `getValues()` | 不可变的取值列表 |

`KeysetPage`

| 方法 | 说明 |
| --- | --- |
| `getContent()` | 当前页数据 |
| `getNextCursor()` | 下一页游标，没有下一页时为 `null` |
| `hasNext()` | 是否存在下一页 |

## 排序规则

- 排序不能为空，执行器会在末尾自动追加主键作为 tiebreaker；已包含主键时不重复追加。
- 禁止 `ignoreCase` 与 `NULLS_FIRST` / `NULLS_LAST`：这两类排序无法用简单的比较条件稳定表达，构建参数时直接抛 `IllegalArgumentException`。
- 游标值个数必须与排序项（含自动追加的主键）一致，不一致时抛出 `IllegalArgumentException`。

## 支持的值类型

游标解码后是字符串，执行器会按排序属性类型转换：

`String`、`Integer` / `Long` / `Short` / `Byte`、`Double` / `Float`、`BigDecimal` / `BigInteger`、`Boolean`、`UUID`、`LocalDate`、`LocalDateTime`、`LocalTime`、`Instant` 与枚举。

其他类型（例如 `java.util.Date`）会抛出 `IllegalArgumentException`，请在实体中使用上表中的类型作为排序键。

## 限制

- 仅支持单一主键；`@IdClass` 或 `@EmbeddedId` 的复合主键实体会被拒绝。
- 游标中的排序键取值不能为 `null`，请避免用可空字段作为排序键。
- 过滤链与 offset 分页一致，依次追加逻辑删除、租户与数据权限条件。
- 游标不携带查询条件，排序或过滤条件变化后旧游标不应继续使用。

## 与 offset 分页对比

| 维度 | offset 分页（`PageParams`） | 游标分页（`KeysetPageParams`） |
| --- | --- | --- |
| 翻页方式 | 页码 + 每页数量 | 上一页游标 |
| 总数统计 | 需要 `count` | 不执行 `count` |
| 深翻页性能 | 随偏移量下降 | 稳定，取决于索引 |
| 数据插入/删除 | 可能重复或漏行 | 以游标为基准，结果稳定 |
| 随机跳页 | 支持 | 不支持 |

对需要随机跳页的后台列表使用 `PageParams`，对滚动加载、导出与深翻页使用游标分页。
