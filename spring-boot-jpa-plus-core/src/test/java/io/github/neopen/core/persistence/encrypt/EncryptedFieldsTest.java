package io.github.neopen.core.persistence.encrypt;

import io.github.neopen.core.exception.JpaPlusException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EncryptedFields} 与 {@link EncryptedFieldConverter} 单元测试。
 */
class EncryptedFieldsTest {

    /**
     * 每个用例前重置密钥与开关。
     */
    @BeforeEach
    void setUp() {
        EncryptedFields.clearKey();
        EncryptedFields.setEnabled(true);
        EncryptedFields.setKey("unit-test-secret");
    }

    /**
     * 每个用例后清理全局状态。
     */
    @AfterEach
    void tearDown() {
        EncryptedFields.clearKey();
        EncryptedFields.setEnabled(true);
    }

    /**
     * 验证加解密往返以及随机 IV。
     */
    @Test
    void shouldEncryptAndDecrypt() {
        String plain = "110101199001011234";

        String first = EncryptedFields.encrypt(plain);
        String second = EncryptedFields.encrypt(plain);

        assertThat(first).isNotEqualTo(plain);
        assertThat(first).isNotEqualTo(second);
        assertThat(EncryptedFields.decrypt(first)).isEqualTo(plain);
        assertThat(EncryptedFields.decrypt(second)).isEqualTo(plain);
    }

    /**
     * 验证空值原样返回。
     */
    @Test
    void shouldKeepNullAndEmptyValues() {
        assertThat(EncryptedFields.encrypt(null)).isNull();
        assertThat(EncryptedFields.encrypt("")).isEmpty();
        assertThat(EncryptedFields.decrypt(null)).isNull();
        assertThat(EncryptedFields.decrypt("")).isEmpty();
    }

    /**
     * 验证密文被篡改时解密失败。
     */
    @Test
    void shouldRejectTamperedCipherText() {
        byte[] raw = Base64.getDecoder().decode(
                EncryptedFields.encrypt("secret-value")
        );
        raw[raw.length - 1] ^= 1;

        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> EncryptedFields.decrypt(tampered))
                .isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证未配置密钥时拒绝加解密。
     */
    @Test
    void shouldRejectMissingKey() {
        EncryptedFields.clearKey();

        assertThatThrownBy(() -> EncryptedFields.encrypt("value"))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("key");

        assertThatThrownBy(() -> EncryptedFields.decrypt("dmFsdWU="))
                .isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证密钥长度校验。
     */
    @Test
    void shouldValidateKeyLength() {
        assertThatThrownBy(() -> EncryptedFields.setKey(new byte[15]))
                .isInstanceOf(IllegalArgumentException.class);

        EncryptedFields.setKey(new byte[16]);
        assertThat(EncryptedFields.hasKey()).isTrue();

        EncryptedFields.setKey(new byte[24]);
        EncryptedFields.setKey(new byte[32]);

        assertThatThrownBy(() -> EncryptedFields.setKey(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证开关关闭后原样返回。
     */
    @Test
    void shouldPassThroughWhenDisabled() {
        EncryptedFields.setEnabled(false);

        assertThat(EncryptedFields.encrypt("plain")).isEqualTo("plain");
        assertThat(EncryptedFields.decrypt("cipher")).isEqualTo("cipher");
    }

    /**
     * 验证转换器双向转换。
     */
    @Test
    void shouldConvertWithAttributeConverter() {
        EncryptedFieldConverter converter = new EncryptedFieldConverter();

        String encrypted = converter.convertToDatabaseColumn("13800000000");

        assertThat(encrypted).isNotEqualTo("13800000000");
        assertThat(converter.convertToEntityAttribute(encrypted))
                .isEqualTo("13800000000");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    /**
     * 验证非法 Base64 密文给出明确异常。
     */
    @Test
    void shouldRejectInvalidBase64() {
        assertThatThrownBy(() ->
                EncryptedFields.decrypt("not-base64-%%%"))
                .isInstanceOf(JpaPlusException.class);
    }

    /**
     * 验证派生密钥的稳定性，相同字符串密钥可解密彼此的密文。
     */
    @Test
    void shouldDeriveStableKeyFromString() {
        String encrypted = EncryptedFields.encrypt("payload");

        EncryptedFields.setKey("unit-test-secret");

        assertThat(EncryptedFields.decrypt(encrypted)).isEqualTo("payload");
        assertThat(EncryptedFields.encrypt("payload")
                .getBytes(StandardCharsets.UTF_8)).isNotEmpty();
    }
}
