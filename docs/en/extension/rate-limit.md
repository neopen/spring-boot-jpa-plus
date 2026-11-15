# API rate limiting

`@RateLimit` performs rate limiting in local memory based on Guava `RateLimiter`, suitable for protecting APIs in single-instance services.

## Prerequisites

```yaml
neopen:
  extension:
    rate-limit:
      enabled: true
```

The aspect is registered by auto-configuration.

## Annotation attributes

| Attribute | Type | Default | Description |
| --- | --- | --- | --- |
| `key` | `String` | `""` | Rate-limiting dimension expression, supports SpEL; when empty, rate limiting is per method |
| `permits` | `double` | `5` | Number of permits allowed per time window; must be greater than 0 |
| `interval` | `long` | `1` | Length of the time window; must be greater than 0 |
| `unit` | `TimeUnit` | `SECONDS` | Unit of the time window |
| `message` | `String` | `操作过于频繁，请稍后重试` | Message returned when rate limiting is triggered |

## Usage

Rate limiting per method:

```java
@RateLimit(permits = 10, interval = 1, unit = TimeUnit.SECONDS)
public void submit() {
}
```

Rate limiting per user:

```java
@RateLimit(key = "#userId", permits = 1, interval = 1)
public void send(String userId) {
}
```

## Behavior

- Rate limiters are cached by the resolved `key`; one is created on first call and the same instance is reused afterwards.
- When `key` is empty or the expression result is blank, `ClassName#methodName` is used as the default dimension.
- Failure to acquire a permit throws `RateLimitException`, with the message taken from `message`.
- Validation completes before method execution; rejected requests do not execute business logic.

## Applicable boundaries

- It is based on local memory; in multi-instance deployments each node counts independently, so the overall quota is the configured value × number of instances.
- For cluster-level rate limiting, replace it with a solution based on shared storage such as Redis.
- Changing annotation parameters does not affect already-cached rate limiters; adjusting the quota requires a restart or changing the `key`.
- It only takes effect for calls through a Spring proxy; internal calls within the same class are not intercepted.
