package io.github.neopen.demo.bean.vo;

import io.github.neopen.convert.convert.vo.BaseVo;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 16:47
 */
@Data
public class OrderVo extends BaseVo implements Serializable {

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

    /**
     * 物流单号
     */
    private String trackingNumber;
    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 关联的订单项列表
     * 一对多关系，cascade表示级联操作，orphanRemoval表示删除孤立实体
     */
    private List<OrderProductVo> orderProducts;
}
