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
package io.github.neopen.demo.persistence.repo;

import io.github.neopen.demo.bean.entity.CustomerSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 客户敏感信息数据仓库。
 *
 * <p>{@code phone} 列保存的是确定性密文，按手机号查询时需要传入
 * {@code EncryptedFields.encryptDeterministic(phone)}。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Repository
public interface CustomerSecretRepo
        extends JpaRepository<CustomerSecret, Long> {

    /**
     * 按确定性密文查询客户。
     *
     * <p>使用原生 SQL 直接比较密文列，避免 JPA 转换器再次加密参数。
     * 查询结果仍会由转换器解密为明文。</p>
     *
     * @param phone 手机号密文
     * @return 匹配的客户列表
     */
    @Query(
            value = "select * from t_customer_secret where phone = :phone",
            nativeQuery = true
    )
    List<CustomerSecret> findAllByPhone(@Param("phone") String phone);

    /**
     * 按客户编号查询全部客户。
     *
     * @param customerNo 客户编号
     * @return 客户列表
     */
    List<CustomerSecret> findByCustomerNo(String customerNo);
}
