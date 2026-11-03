package io.github.neopen.jpa.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * <b> 统计基础对象
 * <p>
 *
 * @author He.hp
 * @version 1.0
 * @date 2020年5月18日 下午2:12:39
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
public class BaseListVo extends BaseVo {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2151235468126855310L;

    private boolean type;
    private String name;

    private Object code;


}
