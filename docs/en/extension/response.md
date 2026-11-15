# Unified paged response

The extension module provides `Json`, `JsonPageConverter`, and `JsonPageResponseFactory` to wrap paged results into a unified response structure.

## Json structure

Fields of `Json<V>`:

| Field | Type | Description |
| --- | --- | --- |
| `success` | `boolean` | Whether the operation succeeded |
| `code` | `Integer` | Business code |
| `message` | `String` | Message |
| `data` | `V` | Data |
| `count` | `Long` | Total count, used in paging scenarios |

Common factory methods:

| Method | Description |
| --- | --- |
| `Json.getSucc()` | Empty success response |
| `Json.getSucc(T data)` | Success with data |
| `Json.getSucc(T data, String msg)` | Success with data and message |
| `Json.getSucc(T data, Long count)` | Success with data and total count |
| `Json.getSucc(String msg, Integer code, T data, Long count)` | Full parameters |
| `Json.getError()` / `getError(String msg)` / `getError(String msg, Integer code)` | Failure response |
| `Json.getJson(boolean result)` | Builds a response from a boolean result |

## Page converter

`JsonPageConverter` is registered by auto-configuration (it is not overridden when the business defines its own `JsonPageConverter`):

| Method | Description |
| --- | --- |
| `Json<List<T>> convert(List<T> data, long total)` | List + total count |
| `Json<List<T>> convert(List<T> data)` | List, with the total count taken from the list size |
| `Json<List<T>> convert(Page<T> page)` | Spring Data `Page` |
| `Json<List<T>> empty()` | Empty paged response |

```java
@GetMapping("/orders")
public Json<List<OrderVo>> page(OrderQueryParam param, Pageable pageable) {
    Page<Order> page = orderService.page(param, pageable);
    return pageConverter.convert(page.map(orderService::parseVo));
}
```

## Custom response structure

The default implementation `DefaultJsonPageResponseFactory` returns `Json.getSucc(data, total)` and validates `total >= 0`. To use a different outer structure, implement `JsonPageResponseFactory`:

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

`JsonPageConverter` relies on this factory to perform the wrapping, so replacing the factory uniformly changes the response structure of all paged APIs.

## Applicable boundaries

- This component only handles wrapping, not exceptions; failure responses require the business to call `Json.getError(...)` in an exception handler.
- `Json` uses three fixed fields `success` / `code` / `message` and is tied to the project's existing API format, so it is not suitable as a public-facing open API protocol.
