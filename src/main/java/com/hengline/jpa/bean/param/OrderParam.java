package com.hengline.jpa.bean.param;

import lombok.Data;

/**
 * <b>
 * <p>
 *
 * @author HengLine
 * @email haeng2030@gmail.com
 * @date 2025/12/23 16:47
 */
@Data
public class OrderParam extends PageParam {
    private String orderNo;
    private String status;
    private String senderPhone;
}
