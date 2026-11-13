package io.github.neopen.demo.bean.param;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import lombok.Data;

/**
 * 商品查询参数。
 */
@Data
public class ProductQueryParam extends PageParam {

    /** 商品名称，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String productName;

    /** 产品类型，支持精确查询。 */
    @QueryCondition
    private String productType;

    /** 产品品牌，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String productBrand;

    /** SKU 编码，支持精确查询。 */
    @QueryCondition
    private String skuCode;

    /** 上架状态，为 {@code null} 时不过滤。 */
    @QueryCondition("isActive")
    private Boolean active;
}
