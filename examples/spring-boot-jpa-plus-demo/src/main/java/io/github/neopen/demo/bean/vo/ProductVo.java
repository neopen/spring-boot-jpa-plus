package io.github.neopen.demo.bean.vo;

import io.github.neopen.convert.converter.vo.BaseVo;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品库存视图对象。
 */
@Data
public class ProductVo extends BaseVo {

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

    /** 库存状态。 */
    private String stockStatus;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 是否上架。 */
    private Boolean active;

    /** 最后入库时间。 */
    private LocalDateTime lastStockInTime;

    /** 最后出库时间。 */
    private LocalDateTime lastStockOutTime;
}
