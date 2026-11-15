# Idempotency control

`@Idempotent` performs duplicate-submission validation before method execution through an aspect, supporting both a one-time token mode and a business key mode.

## Prerequisites

The extension module must be included and kept enabled in configuration:

```xml
<dependency>
    <groupId>io.github.neopen</groupId>
    <artifactId>spring-boot-jpa-plus-extension</artifactId>
    <version>${version}</version>
</dependency>
```

```yaml
neopen:
  extension:
    idempotent:
      enabled: true
```

The aspect is registered by auto-configuration; business projects do not need to declare it as a component.

## Annotation attributes

| Attribute | Type | Default | Description |
| --- | --- | --- | --- |
| `required` | `boolean` | `false` | Whether validation must be performed; when `false`, the call is passed through |
| `key` | `String` | `""` | Business key expression, supports SpEL; when empty, the one-time token mode is used |
| `ttl` | `long` | `5` | Deduplication time window for the business key, in seconds; a value less than or equal to 0 means it remains valid until cache eviction |

## Business key mode

```java
@Idempotent(required = true, key = "#orderNo", ttl = 30)
public void submit(String orderNo) {
}
```

- `key` supports SpEL and can reference method parameters.
- The same business key may be executed successfully only once within `ttl` seconds; repeated requests are rejected.
- When the business key is empty, an `IOException` (`InvalidObjectException`) is thrown.

## One-time token mode

When `key` is not configured, the aspect looks for a `checkToken` field in the method parameters and compares it with the token cached in the current session:

```java
@Idempotent(required = true)
public void submit(OrderSubmitRequest request) {
    // request 中需包含 checkToken 字段
}
```

This mode depends on `HttpServletRequest` and the session, so it only works on Web request threads.

## Scope of effect

- It only applies to methods invoked through a Spring proxy; internal calls within the same class do not trigger it.
- Validation completes before method execution; if the method throws an exception, an already-occupied business key is not rolled back automatically; shorten `ttl` if retries are needed.
- Both business keys and tokens are stored in an in-process cache (the static LRU cache of `IdempotentUtil`); in multi-instance deployments each node makes independent decisions. For distributed idempotency, replace this with a shared storage solution.
