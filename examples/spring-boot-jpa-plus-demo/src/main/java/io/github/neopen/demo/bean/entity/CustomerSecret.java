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
package io.github.neopen.demo.bean.entity;

import io.github.neopen.core.persistence.encrypt.DeterministicEncryptedFieldConverter;
import io.github.neopen.core.persistence.encrypt.EncryptedField;
import io.github.neopen.core.persistence.encrypt.EncryptedFieldConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户敏感信息实体。
 *
 * <p>演示两种加密模式：身份证号使用随机 IV，相同明文每次密文不同；
 * 手机号使用确定性加密，相同明文密文一致，可以直接按密文等值查询。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_customer_secret")
public class CustomerSecret {

    /** 客户主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 客户编号，明文存储用于关联。 */
    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    /**
     * 身份证号。
     *
     * <p>随机 IV 加密，相同明文每次写入的密文都不同，安全性更高。</p>
     */
    @EncryptedField
    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "id_card", nullable = false, length = 256)
    private String idCard;

    /**
     * 手机号。
     *
     * <p>确定性加密，相同明文密文一致，可以按密文做等值查询。</p>
     */
    @EncryptedField(mode = EncryptedField.Mode.DETERMINISTIC)
    @Convert(converter = DeterministicEncryptedFieldConverter.class)
    @Column(name = "phone", nullable = false, length = 256)
    private String phone;
}
