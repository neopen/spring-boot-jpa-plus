package io.github.neopen.demo.bean.param;

import io.github.neopen.convert.annotation.DateStrFormat;
import io.github.neopen.convert.annotation.DateStrValid;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 基础查询参数。
 *
 * <p>提供日期、日期时间和时间范围条件，并启用日期格式校验。</p>
 */
@Data
@DateStrValid
public class BaseParam implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 691794652215049532L;

    /** 开始日期，格式为 {@code yyyy-MM-dd}。 */
    @ApiModelProperty("最小日期字符串（2018-04-12）")
    @Size(max = 12, message = "时间长度最大{max}")
    @DateStrFormat("yyyy-MM-dd")
    private String startDate;

    /** 结束日期，格式为 {@code yyyy-MM-dd}。 */
    @ApiModelProperty("最大日期字符串（2018-04-12）  ")
    @Size(max = 12, message = "时间长度最大{max}")
    @DateStrFormat("yyyy-MM-dd")
    private String endDate;

    /** 开始日期时间，格式为 {@code yyyy-MM-dd HH:mm:ss}。 */
    @ApiModelProperty("最小日期时间字符串（2018-04-12 00:00:00）")
    @Size(max = 20, message = "时间长度最大{max}")
    @DateStrFormat("yyyy-MM-dd HH:mm:ss")
    private String startDateTime;

    /** 结束日期时间，格式为 {@code yyyy-MM-dd HH:mm:ss}。 */
    @ApiModelProperty("最大日期时间字符串（2018-04-12 00:00:00）  ")
    @DateStrFormat("yyyy-MM-dd HH:mm:ss")
    @Size(max = 20, message = "时间长度最大{max}")
    private String endDateTime;

    /** 开始时间，格式为 {@code HH:mm:ss}。 */
    @ApiModelProperty("最小时间字符串（ 00:00:00）")
    @Size(max = 8, message = "时间长度最大{max}")
    @DateStrFormat("HH:mm:ss")
    private String startTime;

    /** 结束时间，格式为 {@code HH:mm:ss}。 */
    @ApiModelProperty("最大时间字符串（ 00:00:00）  ")
    @DateStrFormat("HH:mm:ss")
    @Size(max = 8, message = "时间长度最大{max}")
    private String endTime;
}
