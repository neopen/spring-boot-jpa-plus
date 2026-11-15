# 字段加密

`EncryptedFields` 与 `EncryptedFieldConverter` 对字符串字段做透明加解密，数据库只保存密文。

## 使用方式

实体字段同时标注 `@EncryptedField` 与 `@Convert`：

```java
@Entity
public class Customer {

    @EncryptedField
    @Convert(converter = EncryptedFieldConverter.class)
    private String idCard;

    @EncryptedField
    @Convert(converter = EncryptedFieldConverter.class)
    private String phone;
}
```

`@EncryptedField` 只起标记作用，真正生效的是 `EncryptedFieldConverter`。

## 配置密钥

方式一：通过 Spring Boot 配置。

```yaml
neopen:
  jpa:
    encryption:
      enabled: true
      key: change-me-in-production
```

方式二：代码中设置。

```java
EncryptedFields.setKey("change-me-in-production");

// 或直接使用 16 / 24 / 32 字节密钥
EncryptedFields.setKey(keyBytes);
```

字符串密钥会经 SHA-256 派生为 256 位密钥；字节密钥长度必须为 16、24 或 32。

## 算法与格式

- 算法：`AES/GCM/NoPadding`
- 每次加密生成 12 字节随机 IV
- 存储格式：`Base64(IV + 密文 + 认证标签)`

相同明文每次加密结果不同，密文被篡改时解密会因认证失败抛出 `JpaPlusException`。

## 行为约定

| 场景 | 行为 |
| --- | --- |
| 明文为 `null` 或空串 | 原样返回 |
| 未配置密钥 | 读写时抛出 `JpaPlusException` |
| `encryption.enabled=false` | 加解密均原样返回，字段按明文读写 |
| 密文损坏或密钥不匹配 | 抛出 `JpaPlusException` |

## 直接使用

```java
String stored = EncryptedFields.encrypt("110101199001011234");
String plain = EncryptedFields.decrypt(stored);
```

## 注意事项

- 只适用于字符串字段，数值、日期等请先转成字符串存储。
- 密文不可用于等值或模糊查询；需要检索时另外建立不可逆摘要列。
- 密钥轮换需要自行迁移历史数据，转换器不会尝试用多个密钥解密。
- 关闭加密开关后读取已有密文会得到密文本身，切换前需确认数据状态。
