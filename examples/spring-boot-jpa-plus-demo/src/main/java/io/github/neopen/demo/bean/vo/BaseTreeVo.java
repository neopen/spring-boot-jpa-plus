package io.github.neopen.demo.bean.vo;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * 基础树节点视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel
@NoArgsConstructor
public class BaseTreeVo implements Comparable<BaseTreeVo>, Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 2151235468126855310L;

    /** 父节点标识。 */
    private Object pId;

    /** 节点名称。 */
    private String name = "--";

    /** 节点唯一编号。 */
    @ApiModelProperty("唯一编号")
    private Object code;

    /** 节点显示值。 */
    private String value = "--";

    /** 节点数量。 */
    @ApiModelProperty("节点数量")
    private long count;

    /** 是否为父节点。 */
    @ApiModelProperty("是否节点")
    private boolean node;

    /** 是否在线。 */
    @ApiModelProperty("是否在线")
    private boolean online;

    /** 经度。 */
    private Double longitude;

    /** 纬度。 */
    private Double latitude;

    /** 节点状态标识。 */
    @ApiModelProperty("状态表示")
    private int state;

    /** 子节点列表。 */
    List<BaseTreeVo> children;

    /**
     * 创建顶层节点。
     *
     * @param id   节点标识
     * @param name 节点名称；空白时使用默认名称
     */
    public BaseTreeVo(Object id, String name) {
        this.name = StrUtil.isAllBlank(name) ? "--" : name;
        this.node = true;
        this.code = UUID.randomUUID();
    }

    /**
     * 创建子节点。
     *
     * @param pId  父节点标识
     * @param id   节点标识
     * @param name 节点名称；空白时使用默认名称
     */
    public BaseTreeVo(Object pId, Object id, String name) {
        this.pId = pId;
        this.name = StrUtil.isAllBlank(name) ? "--" : name;
        this.code = UUID.randomUUID();
    }

    /**
     * 创建包含坐标的子节点。
     *
     * @param pId       父节点标识
     * @param id        节点标识
     * @param name      节点名称；空白时使用默认名称
     * @param longitude 经度
     * @param latitude  纬度
     */
    public BaseTreeVo(Object pId, Object id, String name, Double longitude, Double latitude) {
        this.pId = pId;
        this.name = StrUtil.isAllBlank(name) ? "--" : name;
        this.count = 1L;
        this.code = UUID.randomUUID();
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * 按节点名称降序比较。
     *
     * @param o 待比较节点
     * @return 比较结果
     */
    @Override
    public int compareTo(BaseTreeVo o) {
        return (getName().compareTo(o.getName())) > 0 ? -1 : 1;
    }

}
