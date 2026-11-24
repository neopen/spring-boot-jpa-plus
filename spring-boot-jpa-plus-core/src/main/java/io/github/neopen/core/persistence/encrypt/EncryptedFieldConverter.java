/*
 * Copyright 2026 neopen
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

import jakarta.persistence.AttributeConverter;

/**
 * 加密字段的 JPA 转换器。
 *
 * <p>写入数据库前调用 {@link EncryptedFields#encrypt(String)}，
 * 读取时调用 {@link EncryptedFields#decrypt(String)}。实体字段需要同时
 * 标注 {@link EncryptedField} 与
 * {@code @Convert(converter = EncryptedFieldConverter.class)}：</p>
 *
 * <pre>{@code
 * @EncryptedField
 * @Convert(converter = EncryptedFieldConverter.class)
 * private String idCard;
 * }</pre>
 *
 * <p>只会加密字符串字段，数值、日期等类型请先转换为字符串再存储。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class EncryptedFieldConverter
        implements AttributeConverter<String, String> {

    /**
     * 创建默认实例。
     */
    public EncryptedFieldConverter() {
    }

    /**
     * 加密字段值。
     *
     * @param attribute 明文字段值，允许为 {@code null}
     * @return 密文
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptedFields.encrypt(attribute);
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
