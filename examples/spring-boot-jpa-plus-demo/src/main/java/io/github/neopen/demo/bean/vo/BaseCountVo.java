package io.github.neopen.demo.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 基础统计视图对象。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BaseCountVo implements Comparable<BaseCountVo>, Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 2151235468126855310L;

    /** 统计项名称。 */
    private String name;

    /** 统计值。 */
    private Double value;

    /**
     * 按统计项名称升序比较。
     *
     * @param o 待比较对象
     * @return 比较结果；当前对象名称为 {@code null} 时返回 0
     */
    @Override
    public int compareTo(BaseCountVo o) {
        if (getName() == null)
            return 0;

        return (getName().compareTo(o.getName())) > 0 ? 1 : -1;
    }

}
