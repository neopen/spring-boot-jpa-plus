# 日期字符串校验

`@DateStrValid` 与 `@DateStrFormat` 用于在进入业务方法前校验请求参数中的日期字符串，避免每个接口重复写解析与格式判断。

## 注解

| 注解 | 作用目标 | 说明 |
| --- | --- | --- |
| `@DateStrValid` | 方法、类、参数 | 触发日期校验，方法、类或参数任意一处标注即可 |
| `@DateStrFormat` | 字段 | 指定日期格式，支持 `required` 与 `ignore` |

`@DateStrFormat` 的属性：

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `value` | 必填 | 日期格式，例如 `yyyy-MM-dd` |
| `required` | `false` | 为 `true` 时字段不能为空 |
| `ignore` | `false` | 为 `true` 时跳过格式校验，但 `required` 仍然生效 |

## 示例

```java
public class OrderParam {

    @DateStrFormat(value = "yyyy-MM-dd", required = true)
    private String orderDate;
}

@RestController
public class OrderController {

    @PostMapping("/order")
    @DateStrValid
    public Json<Void> create(@RequestBody OrderParam param) {
        return Json.getSucc(null);
    }
}
```

## 校验规则

- 递归扫描参数对象，支持父类字段、嵌套对象、集合、Map 与数组。
- 只校验 `CharSequence` 类型的值；如果字段已经是 `LocalDate`、`LocalDateTime` 等类型，不再做字符串格式校验。
- `required = true` 且值为空时校验失败。
- `ignore = true` 时跳过格式校验，但 `required` 仍然生效。
- 格式非法时抛出 `JpaPlusException`，由业务统一异常处理转换为错误响应。
- 使用线程安全的 `DateTimeFormatter` 并缓存格式，不再对参数做 JSON 序列化。

## 自动配置

引入 Starter 后会自动注册校验切面，无需扫描 `io.github.neopen.convert` 包。需要关闭时配置：

```yaml
neopen:
  jpa:
    convert:
      date-validation:
        enabled: false
```

如果项目没有引入 convert 模块，自动配置会自动跳过，不影响其他功能。
