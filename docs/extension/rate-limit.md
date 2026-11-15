# 接口限流

`@RateLimit` 基于 Guava `RateLimiter` 在本地内存中限流，适合单实例服务的接口保护。

## 前置条件

```yaml
neopen:
  extension:
    rate-limit:
      enabled: true
```

切面由自动配置注册。

## 注解属性

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `key` | `String` | `""` | 限流维度表达式，支持 SpEL；为空时按方法维度限流 |
| `permits` | `double` | `5` | 每个时间窗口允许的许可数量，必须大于 0 |
| `interval` | `long` | `1` | 时间窗口长度，必须大于 0 |
| `unit` | `TimeUnit` | `SECONDS` | 时间窗口单位 |
| `message` | `String` | `操作过于频繁，请稍后重试` | 触发限流时的提示信息 |

## 用法

按方法维度限流：

```java
@RateLimit(permits = 10, interval = 1, unit = TimeUnit.SECONDS)
public void submit() {
}
```

按用户维度限流：

```java
@RateLimit(key = "#userId", permits = 1, interval = 1)
public void send(String userId) {
}
```

## 行为

- 限流器按解析后的 `key` 缓存，首次调用时创建，后续复用同一实例。
- `key` 为空或表达式结果为空白时，使用 `类名#方法名` 作为默认维度。
- 获取许可失败抛 `RateLimitException`，异常信息取自 `message`。
- 校验在方法执行前完成，被拒绝的请求不会执行业务逻辑。

## 适用边界

- 基于本地内存，多实例部署时各节点独立计数，整体配额是配置值 × 实例数。
- 需要集群维度限流时，应替换为基于 Redis 等共享存储的方案。
- 修改注解参数不会影响已缓存的限流器，调整配额需要重启或更换 `key`。
- 只对 Spring 代理调用生效，同类内部调用不会被拦截。
