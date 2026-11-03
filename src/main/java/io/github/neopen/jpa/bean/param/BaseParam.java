package io.github.neopen.jpa.bean.param;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * <b> 基础查询参数对象
 * <p>
 *
 * @author neopen
 * @version 1.0
 * @date 2020年5月18日 下午2:19:47
 */
@Data
public class BaseParam implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 691794652215049532L;
    @Size(max = 12, message = "时间长度最大{max}")
    private String startDate; // 开始计算的日期, 如：2018-04-12
    @Size(max = 12, message = "时间长度最大{max}")
    private String endDate; // 结束计算的日期
    @Size(max = 20, message = "时间长度最大{max}")
    private String startDateTime; // 开始计算的日期, 如：2018-04-12
    @Size(max = 20, message = "时间长度最大{max}")
    private String endDateTime; // 结束计算的日期
    @Size(max = 8, message = "时间长度最大{max}")
    private String startTime; // 开始计算的日期, 如：2018-04-12
    @Size(max = 8, message = "时间长度最大{max}")
    private String endTime; // 结束计算的日期
}
