# 安全排序

`Sorts` 把外部传入的排序表达式解析为 Spring Data 的 `Sort`，并校验字段与方向，避免前端传入任意属性名。

## 方法

| 方法 | 说明 |
| --- | --- |
| `static Sort parse(String expression, Class<?> entityType)` | 按实体字段校验 |
| `static Sort parse(String expression, Class<?> entityType, Map<String, String> aliases)` | 支持外部字段名到实体属性的别名映射 |
| `static Sort parse(String expression, Set<String> allowedFields)` | 按显式白名单校验 |

## 表达式格式

```java
Sort sort = Sorts.parse("createTime,desc;id,asc", Order.class);
Sort allowed = Sorts.parse("id,desc", Set.of("id", "totalAmount"));
```

- 多个排序字段用 `;` 分隔
- 同一字段的字段名与方向用 `,` 分隔
- 方向只接受 `asc` / `desc`
- 空表达式返回 `Sort.unsorted()`

## 校验规则

- 字段不存在、方向非法：抛 `IllegalArgumentException`
- 嵌套路径按段校验，最多三层：

```java
Sort sort = Sorts.parse("customer.name,asc", Order.class);
```

- 别名映射的目标字段同样会被校验，别名指向不存在的属性时同样拒绝：

```java
Sort sort = Sorts.parse(
        "name,desc",
        Order.class,
        Map.of("name", "customer.name")
);
```

## 使用位置

```java
Page<Order> page = orderRepository.findAll(
        spec,
        PageRequest.of(0, 10, Sorts.parse(param.getSort(), Order.class))
);
```

HQL / SQL 构建器也支持直接传入 `Sort`：

```java
HqlWrapper.create(Order.class)
        .orderBy(Sorts.parse("createTime,desc", Order.class));

SqlWrapper.create("t_order", "o")
        .orderBy(Sorts.parse("create_time,desc", Set.of("create_time")));
```

不要直接把 `Sort.by(request.getParameter("sort"))` 交给查询：`Sort` 不做任何字段校验，无法限制外部输入。

## 请求分页参数

`PageParams` 在 `Sorts` 之上封装了页码、每页数量与排序校验，适合直接绑定前端请求参数：

```java
@GetMapping("/orders")
public Page<Order> page(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String sort
) {
    Pageable pageable = PageParams.builder()
            .page(page)
            .size(size)
            .sort(sort)
            .entityType(Order.class)
            .build()
            .toPageable();

    return orderRepository.findAll(spec, pageable);
}
```

| 方法 | 说明 |
| --- | --- |
| `static PageParams of(int page)` | 页码，每页默认 10 条 |
| `static PageParams of(int page, int size)` | 页码与每页数量 |
| `static PageParams of(int page, int size, String sort)` | 追加排序表达式 |
| `builder()` | 完整配置，支持上限、实体类型、别名与白名单 |
| `toPageable()` | 转换为 Spring Data `Pageable` |
| `getOffset()` | 计算偏移量 |

约束：

- 页码从 1 开始，转换为 `Pageable` 时自动减 1。
- 每页数量默认上限为 `500`，超过时抛 `IllegalArgumentException`，可通过 `maxSize` 调整。
- 排序只接受 `entityType` 或 `allowedFields` 能校验的字段；两者都未配置时忽略排序，避免未校验字段进入 SQL。

```java
PageParams.builder()
        .page(1)
        .size(20)
        .maxSize(100)
        .sort("name,desc")
        .entityType(Order.class)
        .aliases(Map.of("name", "customer.name"))
        .build();
```
