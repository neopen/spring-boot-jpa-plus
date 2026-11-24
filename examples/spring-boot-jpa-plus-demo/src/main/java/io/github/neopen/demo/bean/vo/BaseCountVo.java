/*
 * Copyright 2026 neopen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
