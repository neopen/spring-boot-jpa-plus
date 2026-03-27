package com.hengline.jpa.bean.vo;

import com.hengline.jpa.bean.entity.Product;
import jakarta.persistence.Column;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项实体类
 */
@Data
public class OrderProductVo {

    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 关联的商品库存信息
     */
    private Product product;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品单价
     */
    private BigDecimal unitPrice;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 商品图片URL
     */
    private String productImage;

    /**
     * 产品编号
     */
    private String productCode;

    /**
     * 产品类型
     */
    private String productType;

/////////////////////////////////////////////////////

    /**
     * SKU编码
     */
    private String skuCode;


    /**
     * 产品规格
     */
    private String productSpec;

    /**
     * 产品品牌
     */
    private String productBrand;

    /**
     * 生产厂家
     */
    private String manufacturer;


    /**
     * 商品描述
     */
    private String productDescription;

    /**
     * 商品重量
     */
    private BigDecimal weight;

    /**
     * 商品体积
     */
    private BigDecimal volume;


}

