# Date string validation

`@DateStrValid` and `@DateStrFormat` validate date strings in request parameters before a business method runs, so each endpoint does not need to repeat parsing and format checks.

## Annotations

| Annotation | Target | Description |
| --- | --- | --- |
| `@DateStrValid` | Method, type, parameter | Enables date validation; annotating any one of them is enough |
| `@DateStrFormat` | Field | Declares the date pattern, plus `required` and `ignore` |

`@DateStrFormat` attributes:

| Attribute | Default | Description |
| --- | --- | --- |
| `value` | Required | Date pattern, for example `yyyy-MM-dd` |
| `required` | `false` | When `true`, the field must not be empty |
| `ignore` | `false` | When `true`, skips the format check but `required` still applies |

## Example

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

## Rules

- The parameter object is scanned recursively, including superclass fields, nested objects, collections, maps and arrays.
- Only `CharSequence` values are checked; fields that are already `LocalDate` or `LocalDateTime` are not format-checked.
- Validation fails when `required = true` and the value is empty.
- `ignore = true` skips the format check, but `required` still applies.
- An invalid value throws `JpaPlusException`, which a global exception handler can convert into an error response.
- Formats use a thread-safe `DateTimeFormatter` cache; parameters are no longer serialized to JSON.

## Auto-configuration

The starter registers the aspect automatically, so scanning the `io.github.neopen.convert` package is unnecessary. Disable it with:

```yaml
neopen:
  jpa:
    convert:
      date-validation:
        enabled: false
```

If the convert module is not on the classpath, the auto-configuration is skipped and other features are unaffected.
