package io.github.neopen.demo.bean.param;

import lombok.Data;

/**
 * 商品查询参数。
 */
@Data
public class ProductQueryParam extends PageParam {

    /** 商品名称，支持模糊查询。 */
    private String productName;

    /** 产品类型，支持精确查询。 */
    private String productType;

    /** 产品品牌，支持模糊查询。 */
    private String productBrand;

    /** SKU 编码，支持精确查询。 */
    private String skuCode;

    /** 上架状态，为 {@code null} 时不过滤。 */
    private Boolean active;
}
