# Data desensitization

Data desensitization lives in the `extension` module and offers two ways to use it: mask object fields in place, or let fastjson2 desensitize automatically when serializing JSON.

## Annotating fields

```java
public class UserView {

    @Desensitize(type = DesensitizeType.NAME)
    private String name;

    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    @Desensitize(type = DesensitizeType.EMAIL)
    private String email;
}
```

## Built-in types

| Type | Example input | Output |
| --- | --- | --- |
| `NAME` | 张三丰 | 张\*\* |
| `PHONE` | 13800001234 | 138\*\*\*\*1234 |
| `ID_CARD` | 110101199001011234 | 110101\*\*\*\*\*\*\*\*1234 |
| `BANK_CARD` | 6222021234567890 | 6222\*\*\*\*7890 |
| `EMAIL` | alice@example.com | a\*\*\*@example.com |
| `ADDRESS` | 北京市朝阳区建国路88号 | 北京市朝阳区\*\*\*\* |
| `PASSWORD` | Any value | \*\*\*\*\*\* |

When the length is insufficient or the format is not as expected, a fixed mask is returned to avoid leaking the original text.

## In-place desensitization

```java
UserView masked = Desensitizes.apply(userView);
```

- Only `String` fields are processed;
- Superclass fields are scanned;
- Null values and fields whose original value is empty remain unchanged;
- Field metadata is cached by type.

A single string can also be desensitized directly:

```java
String value = Desensitizes.mask("13800001234", DesensitizeType.PHONE);
```

## JSON serialization desensitization

### fastjson2

```java
String json = JSON.toJSONString(userView, new DesensitizeValueFilter());
```

The filter only processes fields annotated with `@Desensitize`; unannotated fields are output as-is. Objects of the same kind embedded in response objects, lists, or nested structures are also handled.

### Jackson

When `jackson-databind` is present, auto-configuration registers `DesensitizeModule`, which applies to the Spring MVC `ObjectMapper` directly:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new DesensitizeModule());

String json = mapper.writeValueAsString(userView);
```

The output matches fastjson2 and only replaces annotated fields. Without Jackson the module is not loaded.

## Notes

- In the JSON scenario, fields are matched by property name, so keep property names consistent when field and getter names differ.
- Desensitization is irreversible; if the original text must be restored, save a mapping before desensitizing or query the original field directly.
- Desensitization does not change database contents; it only affects the output.
