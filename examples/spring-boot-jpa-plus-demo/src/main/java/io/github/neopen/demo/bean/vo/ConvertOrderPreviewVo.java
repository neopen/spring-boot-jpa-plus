package io.github.neopen.demo.bean.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 参数映射预览结果。
 *
 * <p>返回映射后的字段值，以及未匹配字段和发生过转换的字段，
 * 便于直接观察 {@code ParamMapper} 的实际行为。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
public class ConvertOrderPreviewVo {

    /** 主键。开启 ignoreJpaManaged 后应保持服务端预设值。 */
    private Long id;

    /** 版本号。开启 ignoreJpaManaged 后应保持服务端预设值。 */
    private Long version;

    /** 订单编号。 */
    private String orderNo;

    /** 订单总价。 */
    private BigDecimal totalAmount;

    /** 订单日期。 */
    private LocalDate orderDate;

    /** 城市。 */
    private String city;

    /** 详细地址。 */
    private String detail;

    /** 商品引用是否已解析。 */
    private boolean productReferenceResolved;

    /** 备注。 */
    private String remark;

    /** 创建时间。开启 ignoreJpaManaged 后应保持服务端预设值。 */
    private LocalDateTime createTime;

    /** 参数中未匹配到实体字段的名称。 */
    private List<String> unknownFields;

    /** 发生过类型转换或关联解析的参数字段名。 */
    private List<String> convertedFields;
}
