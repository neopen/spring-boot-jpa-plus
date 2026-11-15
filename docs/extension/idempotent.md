# 幂等控制

`@Idempotent` 通过切面在方法执行前完成重复提交校验，支持一次性令牌与业务键两种模式。

## 前置条件

需要引入扩展模块，并保持配置开启：

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

切面由自动配置注册，业务项目不需要再声明为组件。

## 注解属性

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `required` | `boolean` | `false` | 是否必须执行校验；为 `false` 时直接放行 |
| `key` | `String` | `""` | 业务键表达式，支持 SpEL；为空时使用一次性令牌模式 |
| `ttl` | `long` | `5` | 业务键去重时间窗口，单位秒；小于等于 0 表示长期有效直到缓存淘汰 |

## 业务键模式

```java
@Idempotent(required = true, key = "#orderNo", ttl = 30)
public void submit(String orderNo) {
}
```

- `key` 支持 SpEL，可引用方法参数。
- 同一业务键在 `ttl` 秒内只允许成功执行一次，重复请求会被拒绝。
- 业务键为空时抛 `IOException`（`InvalidObjectException`）。

## 一次性令牌模式

不配置 `key` 时，切面从方法参数中查找 `checkToken` 字段，并与当前会话中缓存的令牌比对：

```java
@Idempotent(required = true)
public void submit(OrderSubmitRequest request) {
    // request 中需包含 checkToken 字段
}
```

该模式依赖 `HttpServletRequest` 与会话，只适用于 Web 请求线程。

## 生效范围

- 只作用于 Spring 代理调用的方法，同类内部调用不触发。
- 校验在方法执行前完成，方法抛异常时不会自动回滚已占用的业务键；需要重试时请缩短 `ttl`。
- 业务键与令牌都保存在进程内缓存中（`IdempotentUtil` 的静态 LRU 缓存），多实例部署时各节点判断互相独立；需要分布式幂等时应替换为共享存储方案。
