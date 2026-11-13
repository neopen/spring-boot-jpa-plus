package io.github.neopen.demo.bean.param;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单查询参数。
 */
@Data
public class OrderQueryParam extends PageParam {

    /** 订单编号，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String orderNo;

    /** 订单状态，支持精确查询。 */
    @QueryCondition
    private String status;

    /** 发货人手机号，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String senderPhone;

    /**
     * 订单项商品名称，支持模糊查询。
     *
     * <p>JPA 实现通过连表查询过滤订单项，不参与注解自动映射。</p>
     */
    private String productName;

    /** 订单项最小商品数量。 */
    private Integer minQuantity;

    /** 最小订单金额。 */
    @QueryCondition(
            value = "totalAmount",
            operator = ConditionOperator.BETWEEN,
            endField = "maxTotalAmount"
    )
    private BigDecimal minTotalAmount;

    /** 最大订单金额。 */
    private BigDecimal maxTotalAmount;
}
