package io.github.neopen.demo.bean.vo;

import lombok.Data;

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
public class OrderDetailVo extends OrderVo {


    /**
     * 发货网点名称
     */
    private String shippingBranchName;

    /**
     * 发货网点地址
     */
    private String shippingBranchAddress;

    /**
     * 发货网点联系电话
     */
    private String shippingBranchPhone;

    /**
     * 收货网点ID
     */
    private Long receivingBranchId;

    /**
     * 收货网点名称
     */
    private String receivingBranchName;

    /**
     * 收货网点地址
     */
    private String receivingBranchAddress;

    /**
     * 收货网点联系电话
     */
    private String receivingBranchPhone;

    /**
     * 物流单号
     */
    private String trackingNumber;

    /**
     * 物流公司
     */
    private String logisticsCompany;

    /**
     * 支付方式
     */
    private String paymentMethod;


    /**
     * 预计送达时间
     */
    private LocalDateTime estimatedDeliveryTime;

    /**
     * 实际送达时间
     */
    private LocalDateTime actualDeliveryTime;


    /**
     * 关联的订单项列表
     * 一对多关系，cascade表示级联操作，orphanRemoval表示删除孤立实体
     */
    private List<OrderProductVo> orderProducts;
}
