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
package io.github.neopen.demo.bean.vo;

import io.github.neopen.demo.bean.entity.Product;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项视图对象。
 *
 * <p>包含订单项快照及商品扩展信息。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
public class OrderProductVo {

    /** 订单项标识。 */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 关联的商品库存信息
     */
    private Product product;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品单价
     */
    private BigDecimal unitPrice;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 商品图片URL
     */
    private String productImage;

    /**
     * 产品编号
     */
    private String productCode;

    /**
     * 产品类型
     */
    private String productType;

    // 以下为商品扩展信息。

    /**
     * SKU编码
     */
    private String skuCode;


    /**
     * 产品规格
     */
    private String productSpec;

    /**
     * 产品品牌
     */
    private String productBrand;

    /**
     * 生产厂家
     */
    private String manufacturer;


    /**
     * 商品描述
     */
    private String productDescription;

    /**
     * 商品重量
     */
    private BigDecimal weight;

    /**
     * 商品体积
     */
    private BigDecimal volume;


}

