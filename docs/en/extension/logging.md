# Operation logging annotations

`@SysControllerLog` and `@SysServiceLog` add log descriptions to controller methods and service methods, and are output uniformly by `SysLogAspect`.

## Annotation attributes

Both annotations have only a single `value` attribute, defaulting to an empty string, with `@Target` set to method or parameter:

| Annotation | Purpose |
| --- | --- |
| `@SysControllerLog("创建订单")` | Records the operation description, request information, and access IP before the controller method executes |
| `@SysServiceLog("创建订单失败")` | Records exception information after the service method throws an exception |

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

## Registration

`SysLogAspect` is annotated with `@Component`, but it resides in the library's own package, so it is **not** discovered by the application's component scanning by default, nor is it registered by `ExtensionAutoConfiguration`. It must be enabled explicitly:

```java
@Configuration
@ComponentScan("io.github.neopen.extension.aspect")
public class LogAspectConfig {
}
```

Or declare the bean directly:

```java
@Bean
public SysLogAspect sysLogAspect() {
    return new SysLogAspect();
}
```

## Dependencies and limitations

- A Servlet environment is required (`HttpServletRequest`, `HttpSession`); calling it in non-Web applications or on async threads will fail.
- The aspect reads the session and the client IP; when the description text is empty, only method information is logged.
- The aspect also has a `limiter.enable` switch (default `false`); when enabled, it consults the rules in `RateLimiterUtil` to rate limit before the controller method executes. This is an earlier implementation independent of `@RateLimit`; new projects should use [rate limiting](/en/extension/rate-limit) instead.
- Logs are only written to the application log; they are not persisted to a database and no audit table is provided.
