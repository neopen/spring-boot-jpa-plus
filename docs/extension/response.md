# 统一分页响应

扩展模块提供 `Json`、`JsonPageConverter` 与 `JsonPageResponseFactory`，用于把分页结果包装成统一的响应结构。

## Json 结构

`Json<V>` 的字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `success` | `boolean` | 是否成功 |
| `code` | `Integer` | 业务码 |
| `message` | `String` | 提示信息 |
| `data` | `V` | 数据 |
| `count` | `Long` | 总数，分页场景使用 |

常用工厂方法：

| 方法 | 说明 |
| --- | --- |
| `Json.getSucc()` | 空成功响应 |
| `Json.getSucc(T data)` | 成功并携带数据 |
| `Json.getSucc(T data, String msg)` | 成功、携带数据与提示 |
| `Json.getSucc(T data, Long count)` | 成功、携带数据与总数 |
| `Json.getSucc(String msg, Integer code, T data, Long count)` | 完整参数 |
| `Json.getError()` / `getError(String msg)` / `getError(String msg, Integer code)` | 失败响应 |
| `Json.getJson(boolean result)` | 按布尔结果生成响应 |

## 分页转换器

`JsonPageConverter` 由自动配置注册（业务自定义 `JsonPageConverter` 时不覆盖）：

| 方法 | 说明 |
| --- | --- |
| `Json<List<T>> convert(List<T> data, long total)` | 列表 + 总数 |
| `Json<List<T>> convert(List<T> data)` | 列表，总数取列表大小 |
| `Json<List<T>> convert(Page<T> page)` | Spring Data `Page` |
| `Json<List<T>> empty()` | 空分页响应 |

```java
@GetMapping("/orders")
public Json<List<OrderVo>> page(OrderQueryParam param, Pageable pageable) {
    Page<Order> page = orderService.page(param, pageable);
    return pageConverter.convert(page.map(orderService::parseVo));
}
```

## 自定义响应结构

默认实现 `DefaultJsonPageResponseFactory` 返回 `Json.getSucc(data, total)`，并校验 `total >= 0`。需要不同的外层结构时实现 `JsonPageResponseFactory`：

```java
@Bean
public JsonPageResponseFactory jsonPageResponseFactory() {
    return (data, total) -> {
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        return MyResponse.of(data, total);
    };
}
```

`JsonPageConverter` 依赖该工厂完成包装，因此替换工厂即可统一改变所有分页接口的响应结构。

## 适用边界

- 该组件只负责包装，不处理异常；失败响应需要业务在异常处理器中调用 `Json.getError(...)`。
- `Json` 使用 `success` / `code` / `message` 三个固定字段，与项目既有接口格式绑定，不适合直接作为对外的开放 API 协议。
