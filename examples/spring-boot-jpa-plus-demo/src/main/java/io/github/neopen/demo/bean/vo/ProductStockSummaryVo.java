package io.github.neopen.demo.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品库存状态统计视图对象。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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
