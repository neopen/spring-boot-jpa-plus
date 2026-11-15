package io.github.neopen.core.persistence.encrypt;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要加密存储的字符串字段。
 *
 * <p>注解本身不参与持久化，必须与 {@link EncryptedFieldConverter}
 * 配合使用：</p>
 *
 * <pre>{@code
 * @EncryptedField
 * @Convert(converter = EncryptedFieldConverter.class)
 * private String idCard;
 * }</pre>
 *
 * <p>密钥通过 {@link EncryptedFields#setKey(String)} 或
 * {@link EncryptedFields#setKey(byte[])} 配置，
 * 也可以在 Spring Boot 中通过 {@code neopen.jpa.encryption.key} 配置。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptedField {
}
