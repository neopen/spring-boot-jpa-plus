# 慢方法日志

`@SlowLog` 在方法耗时达到阈值时输出 WARN 日志，用于快速定位接口或批处理的性能问题。

## 用法

```java
@SlowLog(threshold = 200)
public List<Order> loadOrders(Long userId) {
    // ...
}
```

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `threshold` | 500 | 触发日志的耗时阈值，单位毫秒 |

## 日志格式

```text
Slow method OrderService.loadOrders(..) took 812 ms (threshold 200 ms)
```

使用 `SlowLogAspect` 作为日志名，方便单独配置级别或接入告警。

## 行为说明

- 方法正常返回和抛出异常都会记录耗时，异常仍按原样向外传播。
- 耗时按毫秒向下取整，达到阈值即记录。
- 不修改返回值，也不改变异常类型。

## 配置开关

```yaml
neopen:
  extension:
    slow-log:
      enabled: true
```

默认启用。关闭后切面不再注册。

## 注意事项

- 阈值按方法单独配置，建议区分查询、写入与批处理设置不同阈值。
- 日志只反映单次调用耗时，定位性能问题时应结合调用链与数据库慢查询日志。
