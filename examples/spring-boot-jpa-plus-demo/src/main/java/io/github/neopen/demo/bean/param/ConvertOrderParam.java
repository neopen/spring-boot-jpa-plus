package io.github.neopen.demo.bean.param;

import io.github.neopen.convert.annotation.DateStrFormat;
import io.github.neopen.convert.mapping.ParamField;
import io.github.neopen.convert.mapping.ParamIgnore;
import io.github.neopen.convert.mapping.ParamReference;
import lombok.Data;

/**
 * 参数映射示例参数。
 *
 * <p>与 {@link io.github.neopen.demo.bean.entity.ConvertOrder} 配合，
 * 演示列名别名、嵌套路径、外键引用、字段重命名、日期转换和 JPA
 * 托管字段跳过等能力。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
public class ConvertOrderParam {

    /** 主键。示例中传入非空值，用于验证 ignoreJpaManaged 不会覆盖它。 */
    private Long id;

    /** 版本号。示例中传入非空值，用于验证 ignoreJpaManaged 不会覆盖它。 */
    private Long version;

    /** 订单编号，通过列名别名命中实体字段 number。 */
    private String orderNo;

    /** 订单总价，字符串会按目标类型转换为 BigDecimal。 */
    @ParamField("totalAmount")
    private String amount;

    /**
     * 订单日期。
     *
     * <p>{@code @DateStrFormat} 用于切面校验，
     * 实体字段上的 {@code @DateTimeFormat} 用于映射时解析。</p>
     */
    @DateStrFormat(value = "yyyy-MM-dd")
    private String orderDate;

    /** 城市，写入嵌入对象的 address.city。 */
    @ParamField("address.city")
    private String city;

    /** 详细地址，写入嵌入对象的 address.detail。 */
    @ParamField("address.detail")
    private String detail;

    /** 商品 ID，通过 getReference 转为商品关联。 */
    @ParamReference("product")
    private Long productId;

    /** 备注。示例中通过构建器重命名为 remark，并追加一层转换。 */
    private String memo;

    /** 客户端版本号，演示 {@code @ParamIgnore}。 */
    @ParamIgnore
    private String clientVersion;

    /** 实体中不存在的字段，演示未匹配字段报告与严格模式。 */
    private String unknownField;
}
