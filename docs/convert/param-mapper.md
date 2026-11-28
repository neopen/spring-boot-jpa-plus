# 参数 → 实体映射

`ParamMapper` 把请求参数对象映射到实体，用于创建与部分更新。它与 Entity → VO 的转换是两套独立 API，方向与用途都不同。

## 方法

| 方法 | 说明 |
| --- | --- |
| `static <P, E> E toEntity(P param, Class<E> entityType)` | 创建实体并填充 |
| `static <P, E> E toEntity(P param, Class<E> entityType, EntityManager em)` | 创建实体并填充，支持 `@ParamReference` |
| `static <P, E> E toEntity(P param, Supplier<E> supplier)` | 由 `supplier` 提供实体实例 |
| `static <P, E> E toEntity(P param, Supplier<E> supplier, EntityManager em)` | 由 `supplier` 提供实例，支持关联引用 |
| `static <P, E> void apply(P param, E entity)` | 把参数写入已有实体，适合更新 |
| `static <P, E> void apply(P param, E entity, EntityManager em)` | 更新实体，支持关联引用 |
| `static <P, E> List<E> toEntities(Collection<P> params, Class<E> entityType)` | 批量创建 |
| `static <P, E> List<E> toEntities(Collection<P> params, Supplier<E> supplier)` | 批量创建，实例由 `supplier` 提供 |
| `static <P, E> ParamMapping<P, E> mapper(Class<P>, Class<E>)` | 创建可配置映射 |

## 映射规则

- 字段名匹配忽略大小写与下划线：`orderNo`、`order_no`、`ORDER_NO` 都能匹配。
- 同时匹配 JPA 列名别名：实体字段 `number` 标注 `@Column(name = "order_no")` 后，参数里的 `orderNo` 也能命中。
- 默认只覆盖**非空**的参数字段，参数中为 `null` 的字段保留实体原值，因此可以直接用于部分更新。
- 参数字段在实体中不存在时默认忽略；开启严格模式后会报错。
- 支持常见类型自动转换：字符串、数值、布尔、字符、枚举、UUID、日期时间、集合/数组元素，以及 JPA `@Convert` 声明的 `AttributeConverter`。
- 时间字段可使用 Spring 的 `@DateTimeFormat(pattern = "...")` 指定格式；未指定时按 ISO 格式解析，`LocalDateTime` 也兼容 `yyyy-MM-dd HH:mm:ss`。

## 注解

| 注解 | 说明 |
| --- | --- |
| `@ParamField("orderNo")` | 参数字段与实体字段名不一致时指定目标字段，支持 `address.city` 这样的点号路径 |
| `@ParamIgnore` | 忽略该参数字段，不参与映射 |
| `@ParamReference("customer")` | 把外键 ID 转为关联实体引用，需要传入 `EntityManager` |

```java
public class OrderParam {

    @ParamField("orderNo")
    private String number;

    private BigDecimal totalAmount;

    @ParamField("address.city")
    private String city;

    @ParamReference("customer")
    private Long customerId;

    @ParamIgnore
    private String clientVersion;
}
```

## 创建实体

```java
Order order = ParamMapper.toEntity(param, Order.class);
orderRepository.save(order);
```

需要预设默认值或使用带参构造器时，用 `Supplier`：

```java
Order order = ParamMapper.toEntity(param, () -> {
    Order created = new Order();
    created.setStatus("CREATED");
    return created;
});
```

需要映射关联对象时，传入 `EntityManager`：

```java
Order order = ParamMapper.toEntity(param, Order.class, entityManager);
```

`@ParamReference` 内部调用 `EntityManager#getReference`，只创建代理，不会为了设置外键而查询整行数据。

## 更新实体

```java
Order existing = orderRepository.findById(id).orElseThrow();
ParamMapper.apply(param, existing);
orderRepository.save(existing);
```

`apply` 只写入参数中非空的字段，参数未传的字段保持原值。

## 可配置映射

需要更精细的控制时，使用 `ParamMapping`：

```java
MappingResult<Order> result = ParamMapper
        .mapper(OrderParam.class, Order.class)
        .ignore("token")
        .rename("note", "remark")
        .convert("status", (value, context) -> Status.valueOf(value.toString()))
        .nullPolicy(NullPolicy.SET_NULL)
        .ignoreJpaManaged(true)
        .strict(true)
        .map(param, existingOrder);
```

| 方法 | 说明 |
| --- | --- |
| `ignore(String...)` | 忽略指定参数字段 |
| `rename(String, String)` | 参数字段映射到另一个实体字段，支持点号路径 |
| `convert(String, PropertyConverter)` | 为单个字段指定转换器 |
| `nullPolicy(NullPolicy)` | `IGNORE` 保留原值（默认），`SET_NULL` 清空原值 |
| `ignoreJpaManaged(boolean)` | 跳过 `@Id`、`@Version`、自动生成值与审计字段，避免客户端伪造 |
| `strict(boolean)` | 参数存在未匹配字段时直接报错 |
| `map(P, E)` / `apply(P, E)` | 执行映射并返回 `MappingResult` |

`MappingResult` 包含：

| 属性 | 说明 |
| --- | --- |
| `entity` | 映射后的实体 |
| `unknownFields` | 参数中未匹配到实体字段的名称 |
| `convertedFields` | 发生过类型转换或关联解析的参数字段名 |

`NullPolicy.SET_NULL` 不会向基本类型字段写入 `null`，基本类型字段会保持原值。

## 自定义转换器

内置转换不满足需求时，可以注册全局转换器，对所有 `ParamMapper` 与 `ResultMapper` 生效：

```java
Converters.register(new PropertyConverter() {

    @Override
    public boolean supports(ConversionContext context, Object value) {
        return context.targetType() == Money.class;
    }

    @Override
    public Object convert(Object value, ConversionContext context) {
        return Money.of(value.toString());
    }
});
```

只需要影响某个字段时，用构建器：

```java
ParamMapper.mapper(OrderParam.class, Order.class)
        .convert("amount", (value, context) -> new BigDecimal(value.toString()))
        .apply(param, order);
```

后注册的全局转换器优先于内置转换器；目标字段上的 `@Convert` 又优先于普通枚举转换。转换器拿到的 `ConversionContext` 包含目标字段，可以读取字段上的 `@DateTimeFormat` 等注解。

## 与 Entity → VO 的区别

| 项 | Entity → VO | Param → Entity |
| --- | --- | --- |
| 方向 | 实体输出为视图 | 请求参数写入实体 |
| 实现方式 | 显式转换函数（`baseVo`） | 字段名/列名匹配 + 注解 |
| 空值处理 | 由转换函数决定 | 默认非空覆盖，可配置 `NullPolicy` |
| 典型用途 | 查询结果返回 | 新增、部分更新 |
