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
- 多密钥模式下格式为 `密钥编号:Base64(IV + 密文 + 认证标签)`；只有一把密钥且编号为 `default` 时保持无前缀格式

相同明文每次加密结果不同，密文被篡改时解密会因认证失败抛出 `JpaPlusException`。

## 密钥轮换

多把密钥通过 `keys` 配置，`active-key-id` 指定新数据使用的密钥：

```yaml
neopen:
  jpa:
    encryption:
      keys:
        v1: old-secret
        v2: new-secret
      active-key-id: v2
```

- 新写入的数据使用 `v2` 加密，并在密文前加 `v2:` 前缀。
- 读取 `v1:` 前缀的旧数据时自动使用 `v1` 解密，无需迁移即可继续读写。
- 历史无前缀密文会依次尝试当前密钥和其他密钥，兼容 0.5.0 之前的单密钥数据。

代码中也可以直接操作：

```java
EncryptedFields.setKeys(Map.of("v1", "old-secret", "v2", "new-secret"), "v2");

// 灰度切换：历史数据仍可用旧密钥解密
EncryptedFields.setActiveKey("v1");
```

配置多把密钥时必须指定 `active-key-id`，否则启动失败。确认旧数据完成重写后，可以移除旧密钥编号。

## 确定性加密

默认随机 IV 每次加密结果不同，无法按密文等值查询。需要检索的字段（如手机号）可以改用确定性加密：

```java
@Entity
public class Customer {

    @EncryptedField(mode = EncryptedField.Mode.DETERMINISTIC)
    @Convert(converter = DeterministicEncryptedFieldConverter.class)
    private String phone;
}
```

IV 由 `HMAC-SHA256(密钥, 明文)` 前 12 字节派生，因此相同明文得到相同密文，可以直接等值查询：

```java
wrapper.eq("phone", EncryptedFields.encryptDeterministic(phone));
```

::: warning 安全权衡
确定性加密会泄露明文是否相同以及出现频率。只对确实需要等值检索的字段使用，其余字段保持默认的随机 IV 模式。
:::

## 多数据源密钥

不同数据源可以使用不同密钥，通过作用域隔离：

```java
EncryptedFields.setKey("secondary", "secondary-secret");

String cipher = JpaPlusScope.call(
        "secondary",
        () -> EncryptedFields.encrypt("110101199001011234")
);
```

对应的配置项为 `neopen.jpa.scopes.<name>.encryption-key` 或 `encryption-keys`，详见[配置项](/reference/configuration)。未绑定作用域时使用全局密钥。

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
- 随机 IV 模式下的密文不可用于等值或模糊查询；需要等值检索时改用确定性加密。
- 移除密钥编号前，必须确认该编号下的历史数据已全部重写，否则读取会抛出未知密钥编号异常。
- 关闭加密开关后读取已有密文会得到密文本身，切换前需确认数据状态。
