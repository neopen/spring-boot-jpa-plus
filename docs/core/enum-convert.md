# 枚举值转换

`EnumValues` 与 `EnumValueConverter` 把枚举常量映射为数据库中真正想要的值，避免 `@Enumerated(EnumType.STRING)` 直接写入常量名。

## 声明持久化值

在枚举常量上标注 `@EnumValue`：

```java
public enum OrderStatus {

    @EnumValue("PAID")
    PAID,

    @EnumValue("CANCELLED")
    CANCELLED,

    // 未标注时使用 name()
    NEW;
}
```

也可以在枚举的无参方法上标注，方法返回值即持久化值，适合由已有字段派生的场景：

```java
public enum Channel {

    ALIPAY(1), WECHAT(2);

    private final int code;

    Channel(int code) {
        this.code = code;
    }

    @EnumValue
    public String persistedValue() {
        return String.valueOf(code);
    }
}
```

常量注解与方法注解二选一；同时存在会在首次解析时抛出 `JpaPlusException`。

## 配置实体转换器

`EnumValueConverter` 是抽象基类，子类只需声明枚举泛型：

```java
public class OrderStatusConverter
        extends EnumValueConverter<OrderStatus> {
}
```

```java
@Convert(converter = OrderStatusConverter.class)
private OrderStatus status;
```

转换器在实例化时通过反射解析泛型；无法解析（例如直接使用原始类型）会立即抛出 `JpaPlusException`，而不是等到运行期才失败。

## 直接使用

```java
String value = EnumValues.toValue(OrderStatus.PAID);          // "PAID"
OrderStatus status = EnumValues.fromValue(OrderStatus.class, "PAID");
```

## 行为约定

| 场景 | 行为 |
| --- | --- |
| 入参 `null` | 返回 `null` |
| 持久化值为空串 | 返回 `null` |
| 未标注注解 | 使用 `name()` |
| 按值未命中 | 回退到忽略大小写的名称匹配 |
| 值重复 / 注解冲突 | 解析时抛出 `JpaPlusException` |
| 值无法匹配 | `EnumValues.fromValue` 抛出 `JpaPlusException` |

解析结果按枚举类型缓存，重复转换不会重复反射。
