package io.github.neopen.demo.bean.param;

import io.github.neopen.demo.bean.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @email helpenx@gmail.com
 * @date 2026/11/4 19:20
 */
@Data
@AllArgsConstructor
public class OrderProductParam {

    /**
     * 商品ID
     */
    private Long productId;

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
     * 产品编号
     */
    private String productCode;


}
