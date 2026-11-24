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
package io.github.neopen.demo.bean.param;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单查询参数。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
public class OrderQueryParam extends PageParam {

    /** 订单编号，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String orderNo;

    /** 订单状态，支持精确查询。 */
    @QueryCondition
    private String status;

    /** 发货人手机号，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String senderPhone;

    /**
     * 订单项商品名称，支持模糊查询。
     *
     * <p>JPA 实现通过连表查询过滤订单项，不参与注解自动映射。</p>
     */
    private String productName;

    /** 订单项最小商品数量。 */
    private Integer minQuantity;

    /** 最小订单金额。 */
    @QueryCondition(
            value = "totalAmount",
            operator = ConditionOperator.BETWEEN,
            endField = "maxTotalAmount"
    )
    private BigDecimal minTotalAmount;

    /** 最大订单金额。 */
    private BigDecimal maxTotalAmount;
}
