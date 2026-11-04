package io.github.neopen.demo.bean.param;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @email helpenx@gmail.com
 * @date 2026/11/4 19:14
 */
@Data
public class OrderParam {
    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单总价
     */
    private BigDecimal totalAmount;

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
     * 用户ID
     */
    private Long userId;

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
     * 发货网点ID
     */
    private Long shippingBranchId;

    /**
     * 收货网点ID
     */
    private Long receivingBranchId;

    /**
     * 物流单号
     */
    private String trackingNumber;

    /**
     * 支付方式
     */
    private String paymentMethod;

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
     * 关联的订单项列表
     * 一对多关系，cascade表示级联操作，orphanRemoval表示删除孤立实体
     */
    private List<OrderProductParam> orderProducts;


}
