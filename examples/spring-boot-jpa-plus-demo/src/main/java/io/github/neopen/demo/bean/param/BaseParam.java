package io.github.neopen.demo.bean.param;

import io.github.neopen.jpa.annotation.DateStrFormat;
import io.github.neopen.jpa.annotation.DateStrValid;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * <b> 基础查询参数对象
 * <p>
 *
 * @Author neopen
 * @version 1.0
 * @date 2020年5月18日 下午2:19:47
 */
@Data
@DateStrValid
public class BaseParam implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 691794652215049532L;

    @ApiModelProperty("最小日期字符串（2018-04-12）")
    @Size(max = 12, message = "时间长度最大{max}")
    @DateStrFormat("yyyy-MM-dd")
    private String startDate; // 开始计算的日期, 如：2018-04-12

    @ApiModelProperty("最大日期字符串（2018-04-12）  ")
    @Size(max = 12, message = "时间长度最大{max}")
    @DateStrFormat("yyyy-MM-dd")
    private String endDate; // 结束计算的日期

    @ApiModelProperty("最小日期时间字符串（2018-04-12 00:00:00）")
    @Size(max = 20, message = "时间长度最大{max}")
    @DateStrFormat("yyyy-MM-dd HH:mm:ss")
    private String startDateTime; // 开始计算的日期, 如：2018-04-12

    @ApiModelProperty("最大日期时间字符串（2018-04-12 00:00:00）  ")
    @DateStrFormat("yyyy-MM-dd HH:mm:ss")
    @Size(max = 20, message = "时间长度最大{max}")
    private String endDateTime; // 结束计算的日期

    @ApiModelProperty("最小时间字符串（ 00:00:00）")
    @Size(max = 8, message = "时间长度最大{max}")
    @DateStrFormat("HH:mm:ss")
    private String startTime; // 开始计算的日期, 如：2018-04-12

    @ApiModelProperty("最大时间字符串（ 00:00:00）  ")
    @DateStrFormat("HH:mm:ss")
    @Size(max = 8, message = "时间长度最大{max}")
    private String endTime; // 结束计算的日期
}
