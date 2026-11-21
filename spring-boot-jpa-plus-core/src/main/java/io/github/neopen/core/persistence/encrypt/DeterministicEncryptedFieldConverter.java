package io.github.neopen.core.persistence.encrypt;

import jakarta.persistence.AttributeConverter;

/**
 * 确定性加密字段的 JPA 转换器。
 *
 * <p>写入数据库前调用
 * {@link EncryptedFields#encryptDeterministic(String)}，读取时仍使用
 * {@link EncryptedFields#decrypt(String)}。实体字段建议同时标注
 * {@link EncryptedField}：</p>
 *
 * <pre>{@code
 * @EncryptedField(mode = EncryptedField.Mode.DETERMINISTIC)
 * @Convert(converter = DeterministicEncryptedFieldConverter.class)
 * private String phone;
 * }</pre>
 *
 * <p>相同明文得到相同密文，可以按密文做等值查询，例如
 * {@code wrapper.eq("phone", EncryptedFields.encryptDeterministic(phone))}。
 * 代价是密文泄露明文相等关系与出现频率，只应在确实需要检索的字段上
 * 使用。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class DeterministicEncryptedFieldConverter
        implements AttributeConverter<String, String> {

    /**
     * 创建默认实例。
     */
    public DeterministicEncryptedFieldConverter() {
    }

    /**
     * 确定性加密字段值。
     *
     * @param attribute 明文字段值，允许为 {@code null}
     * @return 密文
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptedFields.encryptDeterministic(attribute);
    }

    /**
     * 解密字段值。
     *
     * @param dbData 数据库中的密文，允许为 {@code null}
     * @return 明文
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptedFields.decrypt(dbData);
    }
}
