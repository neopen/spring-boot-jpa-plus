package io.github.neopen.demo.bean.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单实体类
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 订单编号
     */
    @Column(name = "order_no", unique = true, nullable = false, length = 32)
    private String orderNo;

    /**
     * 订单总价
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 订单状态
     * : 待付款
     * : 已付款
     * : 已发货
     * DONE: 已完成
     * CANCEL: 已取消
     */
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 发货地址
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /**
     * 发货人
     */
    @Column(name = "sender_name", length = 50)
    private String senderName;

    /**
     * 发货人手机
     */
    @Column(name = "sender_phone", length = 20)
    private String senderPhone;

    /**
     * 收货人
     */
    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    /**
     * 收货地址
     */
    @Column(name = "receiver_address", nullable = false, length = 500)
    private String receiverAddress;

    /**
     * 收货手机
     */
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    /**
     * 发货网点ID
     */
    @Column(name = "shipping_branch_id")
    private Long shippingBranchId;

    /**
     * 收货网点ID
     */
    @Column(name = "receiving_branch_id")
    private Long receivingBranchId;

    /**
     * 物流单号
     */
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    /**
     * 支付方式
     */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /**
     * 支付时间
     */
    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    /**
     * 订单备注
     */
    @Column(name = "remark", length = 1000)
    private String remark;

    /**
     * 优惠金额
     */
    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    /**
     * 运费
     */
    @Column(name = "shipping_fee", precision = 10, scale = 2)
    private BigDecimal shippingFee;

    /**
     * 关联的订单项列表
     * 一对多关系，cascade表示级联操作，orphanRemoval表示删除孤立实体
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderProduct> orderProducts;

}

