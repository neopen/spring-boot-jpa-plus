package io.github.neopen.demo.bean.param;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增与编辑参数。
 */
@Data
public class ProductParam {

    /** 商品名称。 */
    private String productName;

    /** 商品单价。 */
    private BigDecimal unitPrice;

    /** 库存数量。 */
    private Integer stockQuantity;

    /** 安全库存阈值。 */
    private Integer safetyStock;

    /** 商品编号。 */
    private String productCode;

    /** SKU 编码。 */
    private String skuCode;

    /** 产品类型。 */
    private String productType;

    /** 产品规格。 */
    private String productSpec;

    /** 产品品牌。 */
    private String productBrand;

    /** 生产厂家。 */
    private String manufacturer;

    /** 商品图片地址。 */
    private String productImage;

    /** 商品描述。 */
    private String productDescription;

    /** 商品重量。 */
    private BigDecimal weight;

    /** 商品体积。 */
    private BigDecimal volume;

    /** 是否上架。 */
    private Boolean active;
}
