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
package io.github.neopen.demo.bean.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品库存实体。
 *
 * <p>维护商品资料、库存数量、库存状态和上下架状态。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_product")
public class Product {

    /** 商品主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 商品名称
     */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /**
     * 商品单价
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 库存数量
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    /**
     * 安全库存阈值
     */
    @Column(name = "safety_stock")
    private Integer safetyStock;

    /**
     * 商品编号
     */
    @Column(name = "product_code", length = 100)
    private String productCode;

    /**
     * SKU编码
     */
    @Column(name = "sku_code", length = 50)
    private String skuCode;

    /**
     * 产品类型
     */
    @Column(name = "product_type", length = 100)
    private String productType;

    /**
     * 产品规格
     */
    @Column(name = "product_spec", length = 200)
    private String productSpec;

    /**
     * 产品品牌
     */
    @Column(name = "product_brand", length = 100)
    private String productBrand;

    /**
     * 生产厂家
     */
    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    /**
     * 商品图片URL
     */
    @Column(name = "product_image", length = 500)
    private String productImage;

    /**
     * 商品描述
     */
    @Column(name = "product_description", length = 1000)
    private String productDescription;

    /**
     * 商品重量
     */
    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    /**
     * 商品体积
     */
    @Column(name = "volume", precision = 10, scale = 2)
    private BigDecimal volume;

    /**
     * 库存状态。
     *
     * <p>{@code INSTOCK} 表示有库存，{@code LOWSTOCK} 表示库存不足，
     * {@code OUTOFSTOCK} 表示缺货。</p>
     */
    @Column(name = "stock_status", length = 20)
    private String stockStatus;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 是否上架
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 最后入库时间
     */
    @Column(name = "last_stock_in_time")
    private LocalDateTime lastStockInTime;

    /**
     * 最后出库时间
     */
    @Column(name = "last_stock_out_time")
    private LocalDateTime lastStockOutTime;
}