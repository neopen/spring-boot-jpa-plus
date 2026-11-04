package io.github.neopen.demo.bean.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体类
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_order_product")
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 商品ID
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /**
     * 关联的商品库存信息
     */
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
//    private Product product;

    /**
     * 关联的订单
     * 多对一关系，nullable=false表示订单项必须属于某个订单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * 商品名称
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * 商品单价
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 商品数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 商品图片URL
     */
    @Column(name = "product_image", length = 500)
    private String productImage;

    /**
     * 产品编号
     */
    @Column(name = "product_code", length = 100)
    private String productCode;

    /**
     * 产品类型
     */
    @Column(name = "product_type", length = 100)
    private String productType;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

}

