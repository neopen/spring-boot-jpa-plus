package io.github.neopen.demo.bean.param;

import lombok.Data;

import java.util.List;

/**
 * 商品批量补货参数。
 */
@Data
public class ProductRestockParam {

    /** 需要补货的商品主键列表。 */
    private List<Long> ids;

    /** 每个商品增加的库存数量。 */
    private Integer quantity;
}
