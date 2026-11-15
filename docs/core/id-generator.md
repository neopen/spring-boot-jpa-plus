# 雪花 ID

`SnowflakeIdGenerator` 生成递增、全局唯一的 64 位 ID，适合作为数据库主键。

## 结构

| 部分 | 位数 | 说明 |
| --- | --- | --- |
| 时间戳 | 41 | 相对纪元毫秒数 |
| 数据中心 ID | 5 | 0 到 31 |
| 机器 ID | 5 | 0 到 31 |
| 序列号 | 12 | 同一毫秒内自增，0 到 4095 |

默认纪元为 `2020-01-01T00:00:00Z`。

## 用法

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

## 行为约定

- `nextId()` 为同步方法，可在多线程环境共享同一实例。
- 同一毫秒内序列号用尽时自旋等待下一毫秒。
- 检测到系统时钟回拨时抛出 `IllegalStateException`，不会生成可能重复的 ID。
- 机器 ID、数据中心 ID 越界，或纪元为负数时，构造阶段抛出 `IllegalArgumentException`。

## 注意事项

- 同一 JVM 内应复用单例，避免同一毫秒序列冲突。
- 多实例部署必须保证 `(dataCenterId, workerId)` 组合唯一。
- 时间戳回拨通常来自 NTP 校时，建议在应用层结合告警或重试策略处理。
