package io.github.neopen.jpa.bean.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 16:47
 */
@Data
public class OrderVo extends BaseVo {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单总价
     */
    private String totalAmount;

    /**
     * 订单状态
     * : 待付款
     * : 已付款
     * : 已发货
     * DONE: 已完成
     * CANCEL: 已取消
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

}
