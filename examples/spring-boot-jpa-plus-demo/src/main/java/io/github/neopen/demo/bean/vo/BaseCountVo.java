package io.github.neopen.demo.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <b> 统计基础对象
 * <p>
 *
 * @Author neopen
 * @version 1.0
 * @date 2020年5月18日 下午2:12:39
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BaseCountVo implements Comparable<BaseCountVo>, Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 2151235468126855310L;

    private String name;

    private Double value;

    /**
     * @param o
     * @return
     * @Author neopen
     * @time 2019年3月4日 下午6:10:19
     * @do >>> 排序
     */
    @Override
    public int compareTo(BaseCountVo o) {
//        if (getValue() == null || o.getValue() == null)
        if (getName() == null)
            return 0;

//        return (getValue().compareTo(o.getValue())) > 0 ? -1 : 1; // 倒序
        return (getName().compareTo(o.getName())) > 0 ? 1 : -1; // 正序
    }

}
