package io.github.neopen.jpa.bean.vo;

import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * <b> 树形基础对象
 * <p>
 *
 * @author He.hp
 * @version 1.0
 * @date 2020年5月18日 下午2:12:39
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel
@NoArgsConstructor
public class BaseTreeVo extends BaseVo implements Comparable<BaseTreeVo> {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2151235468126855310L;

    private Object pId;
    private String name = "--";
    @ApiModelProperty("唯一编号")
    private Object code;
    private String value = "--";
    @ApiModelProperty("节点数量")
    private long count;
    @ApiModelProperty("是否节点")
    private boolean node;
    @ApiModelProperty("是否在线")
    private boolean online;
    private Double longitude;
    private Double latitude;
    @ApiModelProperty("状态表示")
    private int state;

    List<BaseTreeVo> children;

    // 顶层
    public BaseTreeVo(Object id, String name) {
        super(id);
        this.name = StrUtil.isAllBlank(name) ? "--" : name;
        this.node = true;
        this.code = UUID.randomUUID();
    }

    // 下层
    public BaseTreeVo(Object pId, Object id, String name) {
        super(id);
        this.pId = pId;
        this.name = StrUtil.isAllBlank(name) ? "--" : name;
        this.code = UUID.randomUUID();
    }

    // 下层
//    public BaseTreeVo(Object pId, Object id, String name, boolean online) {
//        super(id);
//        this.pId = pId;
//        this.name = name;
//        this.online = online;
//        this.code = UUID.randomUUID();
//    }

    // 下层
    public BaseTreeVo(Object pId, Object id, String name, Double longitude, Double latitude) {
        super(id);
        this.pId = pId;
        this.name = StrUtil.isAllBlank(name) ? "--" : name;
        this.count = 1L;
        this.code = UUID.randomUUID();
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * @param o
     * @return
     * @author he.hp
     * @time 2019年3月4日 下午6:10:19
     * @do >>> 倒序排序
     */
    @Override
    public int compareTo(BaseTreeVo o) {
        return (getName().compareTo(o.getName())) > 0 ? -1 : 1;
    }

}
