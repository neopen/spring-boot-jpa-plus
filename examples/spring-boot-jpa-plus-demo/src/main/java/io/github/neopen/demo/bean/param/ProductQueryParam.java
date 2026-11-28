/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
package io.github.neopen.demo.bean.param;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import lombok.Data;

/**
 * 商品查询参数。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
public class ProductQueryParam extends PageParam {

    /** 商品名称，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String productName;

    /** 产品类型，支持精确查询。 */
    @QueryCondition
    private String productType;

    /** 产品品牌，支持模糊查询。 */
    @QueryCondition(operator = ConditionOperator.LIKE)
    private String productBrand;

    /** SKU 编码，支持精确查询。 */
    @QueryCondition
    private String skuCode;

    /** 上架状态，为 {@code null} 时不过滤。 */
    @QueryCondition("isActive")
    private Boolean active;
}
