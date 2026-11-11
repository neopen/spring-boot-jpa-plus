package io.github.neopen.demo.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品库存状态统计视图对象。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockSummaryVo implements Serializable {

    /** 库存状态编码。 */
    private String stockStatus;

    /** 该状态下的商品数量。 */
    private Long total;
}
