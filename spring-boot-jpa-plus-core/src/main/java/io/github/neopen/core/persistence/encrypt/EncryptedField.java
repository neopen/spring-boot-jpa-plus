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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要加密存储的字符串字段。
 *
 * <p>注解本身不参与持久化，必须与加密转换器配合使用：</p>
 *
 * <pre>{@code
 * @EncryptedField
 * @Convert(converter = EncryptedFieldConverter.class)
 * private String idCard;
 *
 * @EncryptedField(mode = EncryptedField.Mode.DETERMINISTIC)
 * @Convert(converter = DeterministicEncryptedFieldConverter.class)
 * private String phone;
 * }</pre>
 *
 * <p>{@link Mode#RANDOM} 每次加密使用随机 IV，安全性更高；
 * {@link Mode#DETERMINISTIC} 使用确定性 IV，相同明文得到相同密文，
 * 可以直接按密文做等值查询，但会泄露明文相等关系与出现频率。</p>
 *
 * <p>密钥通过 {@link EncryptedFields#setKey(String)}、
 * {@link EncryptedFields#setKeys(java.util.Map, String)} 配置，
 * 也可以在 Spring Boot 中通过 {@code neopen.jpa.encryption.key} 或
 * {@code neopen.jpa.encryption.keys} 配置。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptedField {

    /**
     * 加密模式。
     *
     * @return 加密模式，默认随机 IV
     */
    Mode mode() default Mode.RANDOM;

    /**
     * 加密模式。
     */
    enum Mode {

        /**
         * 随机 IV，相同明文每次加密结果不同。
         */
        RANDOM,

        /**
         * 确定性 IV，相同明文得到相同密文，支持等值查询。
         */
        DETERMINISTIC
    }
}
