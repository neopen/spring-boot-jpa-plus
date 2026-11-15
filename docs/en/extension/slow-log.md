# Slow method logging

`@SlowLog` outputs a WARN log when a method's elapsed time reaches the threshold, helping to quickly locate performance issues in APIs or batch processing.

## Usage

```java
@SlowLog(threshold = 200)
public List<Order> loadOrders(Long userId) {
    // ...
}
```

| Attribute | Default | Description |
| --- | --- | --- |
| `threshold` | 500 | Elapsed-time threshold that triggers the log, in milliseconds |

## Log format

```text
Slow method OrderService.loadOrders(..) took 812 ms (threshold 200 ms)
```

`SlowLogAspect` is used as the logger name, making it easy to configure its level separately or hook it into alerting.

## Behavior

- Elapsed time is recorded for both normal returns and thrown exceptions; exceptions are still propagated as-is.
- Elapsed time is rounded down to milliseconds, and the log is recorded as soon as the threshold is reached.
- It does not modify the return value or change the exception type.

## Configuration switch

```yaml
neopen:
  extension:
    slow-log:
      enabled: true
```

Enabled by default. When disabled, the aspect is no longer registered.

## Notes

- The threshold is configured per method; it is recommended to set different thresholds for queries, writes, and batch processing.
- The log only reflects the elapsed time of a single invocation; when locating performance issues, combine it with call chains and the database slow query log.
