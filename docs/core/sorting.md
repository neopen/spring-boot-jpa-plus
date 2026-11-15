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
