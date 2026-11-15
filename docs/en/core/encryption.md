# Field Encryption

`EncryptedFields` and `EncryptedFieldConverter` transparently encrypt and decrypt string fields, so the database stores only ciphertext.

## Usage

Annotate an entity field with both `@EncryptedField` and `@Convert`:

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

`@EncryptedField` only acts as a marker; what actually takes effect is `EncryptedFieldConverter`.

## Configuring the Key

Option one: configure through Spring Boot.

```yaml
neopen:
  jpa:
    encryption:
      enabled: true
      key: change-me-in-production
```

Option two: set it in code.

```java
EncryptedFields.setKey("change-me-in-production");

// 或直接使用 16 / 24 / 32 字节密钥
EncryptedFields.setKey(keyBytes);
```

A string key is derived into a 256-bit key through SHA-256; a byte key must be 16, 24, or 32 bytes long.

## Algorithm and Format

- Algorithm: `AES/GCM/NoPadding`
- A 12-byte random IV is generated for each encryption
- Storage format: `Base64(IV + ciphertext + authentication tag)`

The same plaintext produces a different result on each encryption; if the ciphertext is tampered with, decryption throws `JpaPlusException` because authentication fails.

## Behavior

| Scenario | Behavior |
| --- | --- |
| Plaintext is `null` or an empty string | Returned as-is |
| No key configured | `JpaPlusException` is thrown on read and write |
| `encryption.enabled=false` | Both encryption and decryption return values as-is, and fields are read and written as plaintext |
| Ciphertext is corrupted or the key does not match | `JpaPlusException` is thrown |

## Direct Usage

```java
String stored = EncryptedFields.encrypt("110101199001011234");
String plain = EncryptedFields.decrypt(stored);
```

## Notes

- It applies only to string fields; convert numbers, dates, etc. to strings before storing them.
- Ciphertext cannot be used for equality or fuzzy queries; when searching is required, create a separate irreversible digest column.
- Key rotation requires migrating historical data yourself; the converter does not attempt to decrypt with multiple keys.
- After disabling the encryption switch, reading existing ciphertext returns the ciphertext itself; confirm the data state before switching.
