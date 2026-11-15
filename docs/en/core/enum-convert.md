# Enum Value Conversion

`EnumValues` and `EnumValueConverter` map enum constants to the values you actually want in the database, avoiding `@Enumerated(EnumType.STRING)` writing the constant name directly.

## Declaring Persisted Values

Annotate enum constants with `@EnumValue`:

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

You can also annotate a no-arg method of the enum, where the return value is the persisted value; this suits scenarios derived from existing fields:

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

Use either the constant annotation or the method annotation; if both exist, `JpaPlusException` is thrown during the first resolution.

## Configuring the Entity Converter

`EnumValueConverter` is an abstract base class, and subclasses only need to declare the enum generic:

```java
public class OrderStatusConverter
        extends EnumValueConverter<OrderStatus> {
}
```

```java
@Convert(converter = OrderStatusConverter.class)
private OrderStatus status;
```

The converter resolves the generic through reflection when it is instantiated; if resolution fails (for example, using the raw type directly), `JpaPlusException` is thrown immediately rather than failing at runtime.

## Direct Usage

```java
String value = EnumValues.toValue(OrderStatus.PAID);          // "PAID"
OrderStatus status = EnumValues.fromValue(OrderStatus.class, "PAID");
```

## Behavior

| Scenario | Behavior |
| --- | --- |
| Input is `null` | Returns `null` |
| Persisted value is an empty string | Returns `null` |
| No annotation present | Uses `name()` |
| No match by value | Falls back to case-insensitive name matching |
| Duplicate values / conflicting annotations | `JpaPlusException` is thrown during resolution |
| Value cannot be matched | `EnumValues.fromValue` throws `JpaPlusException` |

Resolution results are cached per enum type, so repeated conversions do not repeat reflection.
