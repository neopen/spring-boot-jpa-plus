# Snowflake ID

`SnowflakeIdGenerator` generates increasing, globally unique 64-bit IDs, suitable for use as database primary keys.

## Structure

| Part | Bits | Description |
| --- | --- | --- |
| Timestamp | 41 | Milliseconds since the epoch |
| Data center ID | 5 | 0 to 31 |
| Worker ID | 5 | 0 to 31 |
| Sequence | 12 | Increments within the same millisecond, 0 to 4095 |

The default epoch is `2020-01-01T00:00:00Z`.

## Usage

```java
SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);

long id = generator.nextId();

// 迁移历史数据时可指定纪元
SnowflakeIdGenerator custom = new SnowflakeIdGenerator(
        1,
        1,
        Instant.parse("2018-01-01T00:00:00Z").toEpochMilli()
);
```

## Behavior

- `nextId()` is a synchronized method, so the same instance can be shared in a multithreaded environment.
- When the sequence is exhausted within the same millisecond, it spins until the next millisecond.
- When a system clock rollback is detected, `IllegalStateException` is thrown instead of generating a potentially duplicate ID.
- When the worker ID or data center ID is out of range, or the epoch is negative, `IllegalArgumentException` is thrown during construction.

## Notes

- Reuse a singleton within the same JVM to avoid sequence conflicts in the same millisecond.
- Multi-instance deployments must ensure that the `(dataCenterId, workerId)` combination is unique.
- Timestamp rollback usually comes from NTP time synchronization; handle it at the application layer with alerting or a retry strategy.
