package io.github.neopen.demo.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 基础列表选项视图对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class BaseListVo implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 2151235468126855310L;

    /** 选项类型标记。 */
    private boolean type;

    /** 选项名称。 */
    private String name;

    /** 选项编码。 */
    private Object code;


}
