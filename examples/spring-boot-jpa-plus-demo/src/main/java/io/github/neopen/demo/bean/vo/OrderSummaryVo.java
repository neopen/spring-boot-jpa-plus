package io.github.neopen.demo.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单摘要视图对象。
 *
 * <p>用于 union 查询与分页示例，只携带列表展示需要的字段。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryVo implements Serializable {

    /** 订单主键。 */
    private Long id;

    /** 订单编号。 */
    private String orderNo;

    /** 订单状态。 */
    private String status;

    /** 订单总价。 */
    private BigDecimal totalAmount;
}
