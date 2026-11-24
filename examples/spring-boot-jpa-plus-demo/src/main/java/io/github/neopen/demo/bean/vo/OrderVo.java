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
package io.github.neopen.demo.bean.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图对象。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
public class OrderVo implements Serializable {

    /** 订单标识。 */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单总价
     */
    private String totalAmount;

    /**
     * 订单状态。
     *
     * <p>常用状态包括待付款、已付款、已发货、已完成和已取消。</p>
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 发货地址
     */
    private String shippingAddress;

    /**
     * 发货人
     */
    private String senderName;

    /**
     * 发货人手机
     */
    private String senderPhone;

    /**
     * 收货人
     */
    private String receiverName;

    /**
     * 收货地址
     */
    private String receiverAddress;

    /**
     * 收货手机
     */
    private String receiverPhone;


    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 运费
     */
    private BigDecimal shippingFee;

    /**
     * 物流单号
     */
    private String trackingNumber;
    /**
     * 支付方式
     */
    private String paymentMethod;

    /** 订单包含的订单项列表。 */
    private List<OrderProductVo> orderProducts;
}
