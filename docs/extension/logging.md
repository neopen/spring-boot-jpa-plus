# 操作日志注解

`@SysControllerLog` 与 `@SysServiceLog` 用于给控制器方法与服务方法补充日志描述，由 `SysLogAspect` 统一输出。

## 注解属性

两个注解都只有一个 `value` 属性，默认空字符串，`@Target` 为方法或参数：

| 注解 | 用途 |
| --- | --- |
| `@SysControllerLog("创建订单")` | 在控制器方法执行前记录操作描述、请求信息与访问 IP |
| `@SysServiceLog("创建订单失败")` | 在服务方法抛出异常后记录异常信息 |

```java
@PostMapping("/orders")
@SysControllerLog("创建订单")
public Json<OrderVo> create(@RequestBody OrderParam param) {
    return Json.getSucc(orderService.create(param));
}
```

```java
@Override
@SysServiceLog("创建订单失败")
public OrderVo create(OrderParam param) {
    // ...
}
```

## 注册方式

`SysLogAspect` 标注为 `@Component`，但位于库自身的包内，**不会**被应用的组件扫描默认发现，也不会被 `ExtensionAutoConfiguration` 注册。需要显式启用：

```java
@Configuration
@ComponentScan("io.github.neopen.extension.aspect")
public class LogAspectConfig {
}
```

或直接声明 Bean：

```java
@Bean
public SysLogAspect sysLogAspect() {
    return new SysLogAspect();
}
```

## 依赖与限制

- 需要 Servlet 环境（`HttpServletRequest`、`HttpSession`），非 Web 应用或异步线程中调用会失败。
- 切面会读取会话与客户端 IP；描述文本为空时只记录方法信息。
- 切面另有一个 `limiter.enable` 开关（默认 `false`），开启后在控制器方法执行前访问 `RateLimiterUtil` 中的规则进行限流。这是独立于 `@RateLimit` 的早期实现，新项目建议统一使用 [接口限流](/extension/rate-limit)。
- 日志只输出到应用日志，不落库、不提供审计表。
