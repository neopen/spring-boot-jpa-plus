package com.hengline.jpa.bean.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品库存实体类
 * 用于维护各种商品信息，购买时从中扣减库存
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
     * 库存数量
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    /**
     * 安全库存阈值
     */
    @Column(name = "safety_stock")
    private Integer safetyStock;

    /**
     * 商品编号
     */
    @Column(name = "product_code", length = 100)
    private String productCode;

    /**
     * SKU编码
     */
    @Column(name = "sku_code", length = 50)
    private String skuCode;

    /**
     * 产品类型
     */
    @Column(name = "product_type", length = 100)
    private String productType;

    /**
     * 产品规格
     */
    @Column(name = "product_spec", length = 200)
    private String productSpec;

    /**
     * 产品品牌
     */
    @Column(name = "product_brand", length = 100)
    private String productBrand;

    /**
     * 生产厂家
     */
    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    /**
     * 商品图片URL
     */
    @Column(name = "product_image", length = 500)
    private String productImage;

    /**
     * 商品描述
     */
    @Column(name = "product_description", length = 1000)
    private String productDescription;

    /**
     * 商品重量
     */
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    /**
     * 商品体积
     */
    @Column(name = "volume", precision = 10, scale = 2)
    private BigDecimal volume;

    /**
     * 库存状态
     * INSTOCK: 有库存
     * LOWSTOCK: 库存不足
     * OUTOFSTOCK: 缺货
     */
    @Column(name = "stock_status", length = 20)
    private String stockStatus;

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
     * 是否上架
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 最后入库时间
     */
    @Column(name = "last_stock_in_time")
    private LocalDateTime lastStockInTime;

    /**
     * 最后出库时间
     */
    @Column(name = "last_stock_out_time")
    private LocalDateTime lastStockOutTime;
}