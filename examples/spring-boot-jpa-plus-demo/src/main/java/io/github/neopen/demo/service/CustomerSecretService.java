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
package io.github.neopen.demo.service;

import io.github.neopen.core.persistence.encrypt.EncryptedFields;
import io.github.neopen.demo.bean.entity.CustomerSecret;
import io.github.neopen.demo.persistence.repo.CustomerSecretRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户敏感信息业务服务。
 *
 * <p>演示字段加密的读写：写入时由转换器自动加密，读取时自动解密；
 * 确定性加密字段可以通过密文等值查询。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Service
public class CustomerSecretService {

    /** 客户敏感信息数据仓库。 */
    private final CustomerSecretRepo customerSecretRepo;

    /**
     * 创建客户敏感信息服务。
     *
     * @param customerSecretRepo 客户敏感信息数据仓库
     */
    public CustomerSecretService(CustomerSecretRepo customerSecretRepo) {
        this.customerSecretRepo = customerSecretRepo;
    }

    /**
     * 保存客户敏感信息。
     *
     * @param customerNo 客户编号
     * @param idCard     身份证号明文
     * @param phone      手机号明文
     * @return 保存后的实体，字段已解密
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomerSecret save(
            String customerNo,
            String idCard,
            String phone
    ) {
        CustomerSecret secret = new CustomerSecret();
        secret.setCustomerNo(customerNo);
        secret.setIdCard(idCard);
        secret.setPhone(phone);

        return customerSecretRepo.saveAndFlush(secret);
    }

    /**
     * 按手机号查询客户。
     *
     * <p>手机号使用确定性加密，查询前把明文加密为密文即可等值匹配。</p>
     *
     * @param phone 手机号明文
     * @return 匹配的客户列表
     */
    @Transactional(readOnly = true)
    public List<CustomerSecret> findByPhone(String phone) {
        return customerSecretRepo.findAllByPhone(
                EncryptedFields.encryptDeterministic(phone)
        );
    }

    /**
     * 按客户编号查询客户列表。
     *
     * @param customerNo 客户编号
     * @return 客户列表
     */
    @Transactional(readOnly = true)
    public List<CustomerSecret> findByCustomerNo(String customerNo) {
        return customerSecretRepo.findByCustomerNo(customerNo);
    }
}
