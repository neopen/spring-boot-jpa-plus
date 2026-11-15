package io.github.neopen.core.persistence.encrypt;

import io.github.neopen.core.exception.JpaPlusException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 字段加密工具。
 *
 * <p>使用 AES/GCM/NoPadding 对字符串加密。每次加密生成 12 字节随机 IV，
 * 输出为 {@code Base64(IV + 密文 + 认证标签)}，因此相同明文每次加密结果
 * 都不同。</p>
 *
 * <p>密钥支持两种配置方式：</p>
 *
 * <ul>
 *     <li>{@link #setKey(String)}：任意字符串，经 SHA-256 派生为 256 位密钥；</li>
 *     <li>{@link #setKey(byte[])}：直接指定 16、24 或 32 字节密钥。</li>
 * </ul>
 *
 * <p>未配置密钥时加解密会抛出 {@link JpaPlusException}，避免数据被明文
 * 写入数据库而不易察觉。关闭加密开关后 {@link #encrypt(String)} 与
 * {@link #decrypt(String)} 均原样返回入参。</p>
 *
 * <pre>{@code
 * EncryptedFields.setKey("my-secret");
 *
 * String stored = EncryptedFields.encrypt("110101199001011234");
 * String plain = EncryptedFields.decrypt(stored);
 * }</pre>
 */
public final class EncryptedFields {

    /** 加密算法。 */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** 密钥算法。 */
    private static final String KEY_ALGORITHM = "AES";

    /** 密钥派生算法。 */
    private static final String DIGEST_ALGORITHM = "SHA-256";

    /** GCM 随机初始化向量长度，单位字节。 */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签长度，单位位。 */
    private static final int TAG_LENGTH_BITS = 128;

    /** 随机数生成器。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 是否启用加密，默认启用。 */
    private static volatile boolean enabled = true;

    /** 当前密钥。 */
    private static volatile SecretKeySpec key;

    /** 工具类不允许实例化。 */
    private EncryptedFields() {
    }

    /**
     * 全局启用或关闭字段加密。
     *
     * @param enabled 是否启用
     */
    public static void setEnabled(boolean enabled) {
        EncryptedFields.enabled = enabled;
    }

    /**
     * 判断字段加密是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 使用任意字符串配置密钥。
     *
     * <p>字符串按 UTF-8 编码后经 SHA-256 派生为 256 位密钥。</p>
     *
     * @param secret 密钥字符串，不能为空白
     * @throws IllegalArgumentException 密钥为空时抛出
     */
    public static void setKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "Encryption key must not be blank"
            );
        }

        try {
            byte[] digest = MessageDigest
                    .getInstance(DIGEST_ALGORITHM)
                    .digest(secret.getBytes(StandardCharsets.UTF_8));

            setKey(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new JpaPlusException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }

    /**
     * 使用原始字节配置密钥。
     *
     * @param secret 密钥字节，长度必须为 16、24 或 32
     * @throws IllegalArgumentException 密钥为空或长度非法时抛出
     */
    public static void setKey(byte[] secret) {
        if (secret == null
                || (secret.length != 16
                && secret.length != 24
                && secret.length != 32)) {
            throw new IllegalArgumentException(
                    "Encryption key must be 16, 24 or 32 bytes"
            );
        }

        key = new SecretKeySpec(secret.clone(), KEY_ALGORITHM);
    }

    /**
     * 清除当前密钥。
     *
     * <p>用于测试隔离或密钥轮换前的重置。清除后加解密会抛出异常。</p>
     */
    public static void clearKey() {
        key = null;
    }

    /**
     * 判断是否已配置密钥。
     *
     * @return 已配置时返回 {@code true}
     */
    public static boolean hasKey() {
        return key != null;
    }

    /**
     * 加密字符串。
     *
     * @param plainText 明文，允许为 {@code null}
     * @return Base64 编码的密文；入参为 {@code null} 或空串时原样返回
     * @throws JpaPlusException 未配置密钥或加密失败时抛出
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty() || !enabled) {
            return plainText;
        }

        SecretKeySpec secret = requireKey();

        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secret,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            byte[] output = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(
                    encrypted,
                    0,
                    output,
                    iv.length,
                    encrypted.length
            );

            return Base64.getEncoder().encodeToString(output);
        } catch (GeneralSecurityException e) {
            throw new JpaPlusException("Cannot encrypt value", e);
        }
    }

    /**
     * 解密字符串。
     *
     * @param cipherText Base64 编码的密文，允许为 {@code null}
     * @return 明文；入参为 {@code null} 或空串时原样返回
     * @throws JpaPlusException 未配置密钥、密文损坏或校验失败时抛出
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty() || !enabled) {
            return cipherText;
        }

        SecretKeySpec secret = requireKey();

        try {
            byte[] raw = Base64.getDecoder().decode(cipherText);

            if (raw.length <= IV_LENGTH + TAG_LENGTH_BITS / 8) {
                throw new JpaPlusException(
                        "Invalid encrypted value"
                );
            }

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(raw, 0, iv, 0, IV_LENGTH);

            byte[] encrypted = new byte[raw.length - IV_LENGTH];
            System.arraycopy(
                    raw,
                    IV_LENGTH,
                    encrypted,
                    0,
                    encrypted.length
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secret,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException e) {
            throw new JpaPlusException(
                    "Encrypted value is not valid Base64",
                    e
            );
        } catch (GeneralSecurityException e) {
            throw new JpaPlusException(
                    "Cannot decrypt value, the key may be wrong "
                            + "or the data has been tampered with",
                    e
            );
        }
    }

    /**
     * 获取当前密钥。
     *
     * @return 密钥
     * @throws JpaPlusException 未配置密钥时抛出
     */
    private static SecretKeySpec requireKey() {
        SecretKeySpec current = key;

        if (current == null) {
            throw new JpaPlusException(
                    "Encryption key is not configured, call "
                            + "EncryptedFields.setKey or set "
                            + "neopen.jpa.encryption.key"
            );
        }

        return current;
    }
}
