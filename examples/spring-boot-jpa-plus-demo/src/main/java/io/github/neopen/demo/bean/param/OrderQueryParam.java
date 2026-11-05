package io.github.neopen.demo.bean.param;

import lombok.Data;

/**
 * 订单查询参数。
 */
@Data
public class OrderQueryParam extends PageParam {

    /** 订单编号，支持模糊查询。 */
    private String orderNo;

    /** 订单状态，支持精确查询。 */
    private String status;

    /** 发货人手机号。 */
    private String senderPhone;
}
