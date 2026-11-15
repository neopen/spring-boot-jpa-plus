# 数据脱敏

脱敏能力位于 `extension` 模块，提供两种使用方式：就地处理对象字段，或交给 fastjson2 在序列化 JSON 时自动脱敏。

## 标注字段

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

## 内置类型

| 类型 | 示例输入 | 输出 |
| --- | --- | --- |
| `NAME` | 张三丰 | 张\*\* |
| `PHONE` | 13800001234 | 138\*\*\*\*1234 |
| `ID_CARD` | 110101199001011234 | 110101\*\*\*\*\*\*\*\*1234 |
| `BANK_CARD` | 6222021234567890 | 6222\*\*\*\*7890 |
| `EMAIL` | alice@example.com | a\*\*\*@example.com |
| `ADDRESS` | 北京市朝阳区建国路88号 | 北京市朝阳区\*\*\*\* |
| `PASSWORD` | 任意值 | \*\*\*\*\*\* |

长度不足或格式不符合预期时返回固定掩码，避免泄露原文。

## 就地脱敏

```java
UserView masked = Desensitizes.apply(userView);
```

- 只处理 `String` 字段；
- 会扫描父类字段；
- 空值与原值为空的字段保持不变；
- 字段元数据按类型缓存。

也可以直接脱敏单个字符串：

```java
String value = Desensitizes.mask("13800001234", DesensitizeType.PHONE);
```

## JSON 序列化脱敏

```java
String json = JSON.toJSONString(userView, new DesensitizeValueFilter());
```

过滤器只处理标注 `@Desensitize` 的字段，未标注字段原样输出。嵌入到返回对象、列表或嵌套结构中的同类对象同样会生效。

## 注意事项

- JSON 场景按属性名匹配字段名，字段与 getter 命名不一致时请保持属性名一致。
- 脱敏不可逆，需要还原原文时请在脱敏前保存映射或直接查询原字段。
- 脱敏不改变数据库内容，只影响输出。
