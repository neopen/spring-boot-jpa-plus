/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.persistence.encrypt;

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.core.scope.JpaPlusScope;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>需要轮换密钥时使用 {@link #setKeys(Map, String)} 配置多把密钥，
 * 新数据使用当前密钥加密并在密文前加上密钥编号（{@code 密钥编号:密文}），
 * 旧数据仍可用原密钥解密。历史数据没有密钥编号前缀，解密时会依次尝试
 * 当前密钥和其他已配置密钥。</p>
 *
 * <p>需要按密文做等值查询时使用 {@link #encryptDeterministic(String)}：
 * IV 由 HMAC-SHA256(密钥, 明文) 派生，相同明文得到相同密文，因此可以
 * 直接比较密文。代价是密文泄露了明文是否相同以及出现频率，只应在确实
 * 需要检索的字段上使用。</p>
 *
 * <p>未配置密钥时加解密会抛出 {@link JpaPlusException}，避免数据被明文
 * 写入数据库而不易察觉。关闭加密开关后加解密方法均原样返回入参。</p>
 *
 * <p>多数据源应用中，可以通过 {@link JpaPlusScope} 绑定数据源作用域，
 * 并用 {@link #setKey(String, String)} 等方法为每个作用域配置独立密钥；
 * 未绑定作用域时使用全局默认密钥。</p>
 *
 * <pre>{@code
 * EncryptedFields.setKey("my-secret");
 *
 * String stored = EncryptedFields.encrypt("110101199001011234");
 * String plain = EncryptedFields.decrypt(stored);
 * }</pre>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public final class EncryptedFields {

    /** 加密算法。 */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** 密钥算法。 */
    private static final String KEY_ALGORITHM = "AES";

    /** 密钥派生算法。 */
    private static final String DIGEST_ALGORITHM = "SHA-256";

    /** 确定性 IV 派生算法。 */
    private static final String MAC_ALGORITHM = "HmacSHA256";

    /** GCM 随机初始化向量长度，单位字节。 */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签长度，单位位。 */
    private static final int TAG_LENGTH_BITS = 128;

    /** 默认密钥编号。 */
    public static final String DEFAULT_KEY_ID = "default";

    /** 密钥编号与密文的分隔符。 */
    private static final char KEY_SEPARATOR = ':';

    /** 随机数生成器。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** 是否启用加密，默认启用。 */
    private static volatile boolean enabled = true;

    /** 全局默认密钥仓库。 */
    private static volatile KeyRing defaultRing = KeyRing.EMPTY;

    /** 按数据源作用域配置的加密开关。 */
    private static final Map<String, Boolean> scopeEnabled =
            new ConcurrentHashMap<>();

    /** 按数据源作用域配置的密钥仓库。 */
    private static final Map<String, KeyRing> scopeRings =
            new ConcurrentHashMap<>();

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
     * 为指定数据源作用域设置加密开关。
     *
     * @param scope   作用域名称，等于默认作用域时等同于全局设置
     * @param enabled 是否启用
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void setEnabled(String scope, boolean enabled) {
        if (JpaPlusScope.isDefault(scope)) {
            setEnabled(enabled);
            return;
        }

        scopeEnabled.put(JpaPlusScope.requireName(scope), enabled);
    }

    /**
     * 判断全局字段加密是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 判断指定作用域的字段加密是否启用。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    public static boolean isEnabled(String scope) {
        return activeEnabled(scope);
    }

    /**
     * 使用任意字符串配置密钥。
     *
     * <p>字符串按 UTF-8 编码后经 SHA-256 派生为 256 位密钥，
     * 密钥编号固定为 {@value #DEFAULT_KEY_ID}，密文不携带密钥编号。</p>
     *
     * @param secret 密钥字符串，不能为空白
     * @throws IllegalArgumentException 密钥为空时抛出
     */
    public static void setKey(String secret) {
        setKeys(
                Map.of(DEFAULT_KEY_ID, requireSecret(secret)),
                DEFAULT_KEY_ID
        );
    }

    /**
     * 为指定数据源作用域配置字符串密钥。
     *
     * @param scope  作用域名称，等于默认作用域时等同于全局设置
     * @param secret 密钥字符串，不能为空白
     * @throws IllegalArgumentException 作用域或密钥为空时抛出
     */
    public static void setKey(String scope, String secret) {
        if (JpaPlusScope.isDefault(scope)) {
            setKey(secret);
            return;
        }

        setKeys(
                scope,
                Map.of(DEFAULT_KEY_ID, requireSecret(secret)),
                DEFAULT_KEY_ID
        );
    }

    /**
     * 使用原始字节配置密钥。
     *
     * @param secret 密钥字节，长度必须为 16、24 或 32
     * @throws IllegalArgumentException 密钥为空或长度非法时抛出
     */
    public static void setKey(byte[] secret) {
        validateKeyBytes(secret);

        defaultRing = KeyRing.of(
                Map.of(
                        DEFAULT_KEY_ID,
                        new SecretKeySpec(
                                secret.clone(),
                                KEY_ALGORITHM
                        )
                ),
                DEFAULT_KEY_ID
        );
    }

    /**
     * 为指定数据源作用域配置原始字节密钥。
     *
     * @param scope  作用域名称，等于默认作用域时等同于全局设置
     * @param secret 密钥字节，长度必须为 16、24 或 32
     * @throws IllegalArgumentException 作用域名为空、密钥为空或长度非法
     */
    public static void setKey(String scope, byte[] secret) {
        if (JpaPlusScope.isDefault(scope)) {
            setKey(secret);
            return;
        }

        validateKeyBytes(secret);

        scopeRings.put(
                JpaPlusScope.requireName(scope),
                KeyRing.of(
                        Map.of(
                                DEFAULT_KEY_ID,
                                new SecretKeySpec(
                                        secret.clone(),
                                        KEY_ALGORITHM
                                )
                        ),
                        DEFAULT_KEY_ID
                )
        );
    }

    /**
     * 配置多把密钥以支持轮换。
     *
     * <p>密钥字符串按 SHA-256 派生，密钥编号不能为空且不能包含
     * {@code :}。未指定当前密钥且只配置一把密钥时自动使用该密钥；
     * 配置多把密钥时必须显式指定当前密钥。密文格式为
     * {@code 密钥编号:Base64(IV + 密文 + 认证标签)}，单密钥且编号为
     * {@value #DEFAULT_KEY_ID} 时保持无前缀的历史格式。</p>
     *
     * @param secrets      密钥编号到密钥字符串的映射，不能为空
     * @param activeKeyId  当前加密使用的密钥编号，允许为 {@code null}
     * @throws IllegalArgumentException 密钥为空、编号非法或当前密钥不存在时抛出
     */
    public static void setKeys(
            Map<String, String> secrets,
            String activeKeyId
    ) {
        Map<String, SecretKeySpec> parsed = parseKeys(secrets);

        defaultRing = KeyRing.of(
                parsed,
                resolveActiveKeyId(parsed.keySet(), activeKeyId)
        );
    }

    /**
     * 为指定数据源作用域配置多把密钥。
     *
     * @param scope       作用域名称，等于默认作用域时等同于全局设置
     * @param secrets     密钥编号到密钥字符串的映射，不能为空
     * @param activeKeyId 当前加密使用的密钥编号，允许为 {@code null}
     * @throws IllegalArgumentException 作用域非法或密钥配置非法时抛出
     */
    public static void setKeys(
            String scope,
            Map<String, String> secrets,
            String activeKeyId
    ) {
        if (JpaPlusScope.isDefault(scope)) {
            setKeys(secrets, activeKeyId);
            return;
        }

        Map<String, SecretKeySpec> parsed = parseKeys(secrets);

        scopeRings.put(
                JpaPlusScope.requireName(scope),
                KeyRing.of(
                        parsed,
                        resolveActiveKeyId(parsed.keySet(), activeKeyId)
                )
        );
    }

    /**
     * 切换当前加密使用的密钥。
     *
     * <p>用于灰度轮换：先配置新旧两把密钥，再把当前密钥切换到新密钥，
     * 历史数据仍可使用旧密钥解密。</p>
     *
     * @param keyId 已配置的密钥编号
     * @throws JpaPlusException 密钥编号未配置时抛出
     */
    public static void setActiveKey(String keyId) {
        defaultRing = switchActiveKey(defaultRing, keyId);
    }

    /**
     * 切换指定作用域加密使用的密钥。
     *
     * @param scope 作用域名称，等于默认作用域时等同于全局设置
     * @param keyId 已配置的密钥编号
     * @throws JpaPlusException 密钥编号未配置时抛出
     */
    public static void setActiveKey(String scope, String keyId) {
        if (JpaPlusScope.isDefault(scope)) {
            setActiveKey(keyId);
            return;
        }

        scopeRings.computeIfPresent(
                JpaPlusScope.requireName(scope),
                (name, ring) -> switchActiveKey(ring, keyId)
        );
    }

    /**
     * 清除全局密钥。
     *
     * <p>用于测试隔离或密钥重置。清除后加解密会抛出异常。</p>
     */
    public static void clearKey() {
        defaultRing = KeyRing.EMPTY;
    }

    /**
     * 清除指定作用域的密钥。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearKey(String scope) {
        scopeRings.remove(JpaPlusScope.requireName(scope));
    }

    /**
     * 清除指定作用域的加密开关与密钥配置。
     *
     * @param scope 作用域名称
     * @throws IllegalArgumentException 作用域名称为空时抛出
     */
    public static void clearScope(String scope) {
        String name = JpaPlusScope.requireName(scope);

        scopeEnabled.remove(name);
        scopeRings.remove(name);
    }

    /**
     * 判断当前作用域是否已配置密钥。
     *
     * @return 已配置时返回 {@code true}
     */
    public static boolean hasKey() {
        return !ring().keys().isEmpty();
    }

    /**
     * 获取当前作用域加密使用的密钥编号。
     *
     * @return 密钥编号；未配置密钥时返回 {@code null}
     */
    public static String getActiveKeyId() {
        return ring().activeKeyId();
    }

    /**
     * 使用随机 IV 加密字符串。
     *
     * @param plainText 明文，允许为 {@code null}
     * @return 密文；入参为 {@code null} 或空串时原样返回
     * @throws JpaPlusException 未配置密钥或加密失败时抛出
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty() || !activeEnabled()) {
            return plainText;
        }

        KeyRing ring = ring();
        SecretKeySpec secret = requireActiveKey(ring);
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);

        return seal(plainText, ring, secret, iv);
    }

    /**
     * 使用确定性 IV 加密字符串，支持按密文等值查询。
     *
     * <p>IV 取 HMAC-SHA256(密钥, 明文) 的前 12 字节，相同明文得到
     * 相同密文。确定性加密泄露明文相等关系与频率信息，只应在需要
     * 检索的字段上使用。</p>
     *
     * @param plainText 明文，允许为 {@code null}
     * @return 密文；入参为 {@code null} 或空串时原样返回
     * @throws JpaPlusException 未配置密钥或加密失败时抛出
     */
    public static String encryptDeterministic(String plainText) {
        if (plainText == null || plainText.isEmpty() || !activeEnabled()) {
            return plainText;
        }

        KeyRing ring = ring();
        SecretKeySpec secret = requireActiveKey(ring);
        byte[] iv = deriveIv(secret, plainText);

        return seal(plainText, ring, secret, iv);
    }

    /**
     * 解密字符串。
     *
     * <p>密文带密钥编号前缀时使用对应密钥解密；历史无前缀密文会
     * 依次尝试当前密钥和其余已配置密钥。</p>
     *
     * @param cipherText 密文，允许为 {@code null}
     * @return 明文；入参为 {@code null} 或空串时原样返回
     * @throws JpaPlusException 未配置密钥、密文损坏、密钥编号未知或
     *                          校验失败时抛出
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty() || !activeEnabled()) {
            return cipherText;
        }

        KeyRing ring = ring();
        Map<String, SecretKeySpec> current = ring.keys();

        if (current.isEmpty()) {
            throw new JpaPlusException(
                    "Encryption key is not configured, call "
                            + "EncryptedFields.setKey or set "
                            + "neopen.jpa.encryption.key"
            );
        }

        int separator = cipherText.indexOf(KEY_SEPARATOR);

        if (separator > 0) {
            String keyId = cipherText.substring(0, separator);
            SecretKeySpec secret = current.get(keyId);

            if (secret == null) {
                throw new JpaPlusException(
                        "Unknown encryption key id: " + keyId
                );
            }

            return open(
                    cipherText.substring(separator + 1),
                    secret
            );
        }

        return openWithAnyKey(cipherText, ring);
    }

    /**
     * 使用指定密钥加密并编码。
     *
     * @param plainText 明文
     * @param ring      密钥仓库
     * @param secret    密钥
     * @param iv        初始化向量
     * @return 密文
     */
    private static String seal(
            String plainText,
            KeyRing ring,
            SecretKeySpec secret,
            byte[] iv
    ) {
        try {
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

            String encoded = Base64.getEncoder()
                    .encodeToString(output);

            return withPrefix(ring, encoded);
        } catch (GeneralSecurityException e) {
            throw new JpaPlusException("Cannot encrypt value", e);
        }
    }

    /**
     * 为密文添加密钥编号前缀。
     *
     * <p>单密钥且编号为默认编号时保持历史无前缀格式，避免升级后
     * 所有密文格式变化。</p>
     *
     * @param ring    密钥仓库
     * @param encoded Base64 密文
     * @return 带前缀的密文
     */
    private static String withPrefix(KeyRing ring, String encoded) {
        String keyId = ring.activeKeyId();

        if (ring.keys().size() == 1 && DEFAULT_KEY_ID.equals(keyId)) {
            return encoded;
        }

        return keyId + KEY_SEPARATOR + encoded;
    }

    /**
     * 使用指定密钥解密。
     *
     * @param cipherText Base64 密文
     * @param secret     密钥
     * @return 明文
     */
    private static String open(
            String cipherText,
            SecretKeySpec secret
    ) {
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
     * 依次尝试所有密钥解密历史无前缀密文。
     *
     * @param cipherText 密文
     * @param ring       密钥仓库
     * @return 明文
     * @throws JpaPlusException 所有密钥都无法解密时抛出
     */
    private static String openWithAnyKey(
            String cipherText,
            KeyRing ring
    ) {
        Map<String, SecretKeySpec> current = ring.keys();
        List<String> keyIds = new ArrayList<>(current.size());

        if (ring.activeKeyId() != null
                && current.containsKey(ring.activeKeyId())) {
            keyIds.add(ring.activeKeyId());
        }

        for (String keyId : current.keySet()) {
            if (!keyIds.contains(keyId)) {
                keyIds.add(keyId);
            }
        }

        JpaPlusException failure = null;

        for (String keyId : keyIds) {
            try {
                return open(cipherText, current.get(keyId));
            } catch (JpaPlusException e) {
                failure = e;
            }
        }

        throw failure == null
                ? new JpaPlusException("Cannot decrypt value")
                : failure;
    }

    /**
     * 派生确定性初始化向量。
     *
     * @param secret    密钥
     * @param plainText 明文
     * @return 12 字节初始化向量
     */
    private static byte[] deriveIv(
            SecretKeySpec secret,
            String plainText
    ) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(secret);

            byte[] digest = mac.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(digest, 0, iv, 0, IV_LENGTH);

            return iv;
        } catch (GeneralSecurityException e) {
            throw new JpaPlusException(
                    "Cannot derive deterministic IV",
                    e
            );
        }
    }

    /**
     * 获取当前加密密钥。
     *
     * @param ring 密钥仓库
     * @return 密钥
     * @throws JpaPlusException 未配置密钥时抛出
     */
    private static SecretKeySpec requireActiveKey(KeyRing ring) {
        SecretKeySpec current = ring.activeKey();

        if (current == null) {
            throw new JpaPlusException(
                    "Encryption key is not configured, call "
                            + "EncryptedFields.setKey or set "
                            + "neopen.jpa.encryption.key"
            );
        }

        return current;
    }

    /**
     * 解析密钥配置。
     *
     * @param secrets 密钥编号到密钥字符串的映射
     * @return 密钥编号到密钥的映射
     * @throws IllegalArgumentException 密钥为空或编号非法时抛出
     */
    private static Map<String, SecretKeySpec> parseKeys(
            Map<String, String> secrets
    ) {
        if (secrets == null || secrets.isEmpty()) {
            throw new IllegalArgumentException(
                    "Encryption keys must not be empty"
            );
        }

        Map<String, SecretKeySpec> parsed = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : secrets.entrySet()) {
            parsed.put(
                    normalizeKeyId(entry.getKey()),
                    new SecretKeySpec(
                            derive(requireSecret(entry.getValue())),
                            KEY_ALGORITHM
                    )
            );
        }

        return parsed;
    }

    /**
     * 解析当前加密使用的密钥编号。
     *
     * @param keyIds      已配置的密钥编号
     * @param activeKeyId 指定的密钥编号，允许为 {@code null}
     * @return 当前密钥编号
     * @throws IllegalArgumentException 未指定且存在多把密钥，或编号未配置
     */
    private static String resolveActiveKeyId(
            Set<String> keyIds,
            String activeKeyId
    ) {
        if (activeKeyId == null || activeKeyId.isBlank()) {
            if (keyIds.size() > 1) {
                throw new IllegalArgumentException(
                        "Active encryption key id must be configured"
                );
            }

            return keyIds.iterator().next();
        }

        String active = normalizeKeyId(activeKeyId);

        if (!keyIds.contains(active)) {
            throw new IllegalArgumentException(
                    "Active encryption key id is not configured: " + active
            );
        }

        return active;
    }

    /**
     * 切换密钥仓库的当前密钥。
     *
     * @param ring  密钥仓库
     * @param keyId 目标密钥编号
     * @return 切换后的密钥仓库
     * @throws JpaPlusException 密钥编号未配置时抛出
     */
    private static KeyRing switchActiveKey(KeyRing ring, String keyId) {
        String normalized = normalizeKeyId(keyId);

        if (!ring.keys().containsKey(normalized)) {
            throw new JpaPlusException(
                    "Encryption key id is not configured: " + normalized
            );
        }

        return KeyRing.of(ring.keys(), normalized);
    }

    /**
     * 获取当前作用域生效的密钥仓库。
     *
     * @return 密钥仓库
     */
    private static KeyRing ring() {
        String scope = JpaPlusScope.currentScopeName();

        if (scope == null) {
            return defaultRing;
        }

        KeyRing scoped = scopeRings.get(scope);

        return scoped == null ? defaultRing : scoped;
    }

    /**
     * 判断当前作用域的加密是否启用。
     *
     * @return 启用时返回 {@code true}
     */
    private static boolean activeEnabled() {
        return activeEnabled(JpaPlusScope.currentScopeName());
    }

    /**
     * 解析指定作用域生效的加密开关。
     *
     * @param scope 作用域名称，允许为 {@code null}
     * @return 启用时返回 {@code true}
     */
    private static boolean activeEnabled(String scope) {
        if (scope == null) {
            return enabled;
        }

        Boolean scoped = scopeEnabled.get(scope);

        return scoped == null ? enabled : scoped;
    }

    /**
     * 把密钥字符串派生为 256 位密钥字节。
     *
     * @param secret 密钥字符串
     * @return 密钥字节
     */
    private static byte[] derive(String secret) {
        try {
            return MessageDigest
                    .getInstance(DIGEST_ALGORITHM)
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new JpaPlusException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }

    /**
     * 校验并规范化密钥编号。
     *
     * @param keyId 密钥编号
     * @return 去除首尾空白后的编号
     * @throws IllegalArgumentException 编号为空或包含分隔符时抛出
     */
    private static String normalizeKeyId(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Encryption key id must not be blank"
            );
        }

        String normalized = keyId.trim();

        if (normalized.indexOf(KEY_SEPARATOR) >= 0) {
            throw new IllegalArgumentException(
                    "Encryption key id must not contain ':'"
            );
        }

        return normalized;
    }

    /**
     * 校验密钥字符串。
     *
     * @param secret 密钥字符串
     * @return 原始密钥字符串
     * @throws IllegalArgumentException 密钥为空时抛出
     */
    private static String requireSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "Encryption key must not be blank"
            );
        }

        return secret;
    }

    /**
     * 校验原始密钥字节。
     *
     * @param secret 密钥字节
     * @throws IllegalArgumentException 密钥为空或长度非法时抛出
     */
    private static void validateKeyBytes(byte[] secret) {
        if (secret == null
                || (secret.length != 16
                && secret.length != 24
                && secret.length != 32)) {
            throw new IllegalArgumentException(
                    "Encryption key must be 16, 24 or 32 bytes"
            );
        }
    }

    /**
     * 密钥仓库。
     *
     * @param keys        密钥编号到密钥的映射
     * @param activeKeyId 当前加密使用的密钥编号
     */
    private record KeyRing(
            Map<String, SecretKeySpec> keys,
            String activeKeyId
    ) {

        /** 空密钥仓库。 */
        private static final KeyRing EMPTY =
                new KeyRing(Map.of(), null);

        /**
         * 创建密钥仓库。
         *
         * @param keys        密钥编号到密钥的映射
         * @param activeKeyId 当前加密使用的密钥编号
         * @return 密钥仓库
         */
        private static KeyRing of(
                Map<String, SecretKeySpec> keys,
                String activeKeyId
        ) {
            return new KeyRing(Map.copyOf(keys), activeKeyId);
        }

        /**
         * 获取当前加密密钥。
         *
         * @return 密钥；未配置或编号不存在时返回 {@code null}
         */
        private SecretKeySpec activeKey() {
            return activeKeyId == null
                    ? null
                    : keys.get(activeKeyId);
        }
    }
}
