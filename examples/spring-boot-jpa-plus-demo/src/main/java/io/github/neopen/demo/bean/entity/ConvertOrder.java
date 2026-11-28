package io.github.neopen.demo.bean.entity;

import io.github.neopen.core.persistence.audit.CreatedAt;
import io.github.neopen.core.persistence.audit.UpdatedAt;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 参数映射示例实体。
 *
 * <p>集中演示参数到实体的各类映射能力：</p>
 *
 * <ul>
 *     <li>{@code @Column(name = "order_no")} 列名别名匹配；</li>
 *     <li>{@code @Embedded} 嵌套对象与点号路径映射；</li>
 *     <li>{@code @ManyToOne} 外键转为实体引用；</li>
 *     <li>{@code @Version}、{@code @CreatedAt}、{@code @UpdatedAt}
 *         在开启 {@code ignoreJpaManaged} 时不会被参数覆盖；</li>
 *     <li>{@code @DateTimeFormat} 指定字符串到日期的解析格式。</li>
 * </ul>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_convert_order")
public class ConvertOrder {

    /** 订单主键，JPA 托管字段。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 乐观锁版本号，JPA 托管字段。 */
    @Version
    @Column(name = "version")
    private Long version = 0L;

    /**
     * 订单编号。
     *
     * <p>字段名与参数不同，参数中的 {@code orderNo} 通过
     * {@code @Column(name = "order_no")} 别名命中。</p>
     */
    @Column(name = "order_no", length = 32)
    private String number;

    /** 订单总价。 */
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** 订单日期。 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "order_date")
    private LocalDate orderDate;

    /** 嵌套地址。 */
    @Embedded
    private ConvertAddress address;

    /** 关联商品，用于演示 {@code @ParamReference}。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** 备注。 */
    @Column(name = "remark", length = 200)
    private String remark;

    /** 创建时间，审计托管字段。 */
    @CreatedAt
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间，审计托管字段。 */
    @UpdatedAt
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
