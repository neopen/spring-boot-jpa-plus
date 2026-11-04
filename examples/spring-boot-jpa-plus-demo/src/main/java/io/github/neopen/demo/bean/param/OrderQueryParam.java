package io.github.neopen.demo.bean.param;

import lombok.Data;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 16:47
 */
@Data
public class OrderQueryParam extends PageParam {
    private String orderNo;
    private String status;
    private String senderPhone;
}
