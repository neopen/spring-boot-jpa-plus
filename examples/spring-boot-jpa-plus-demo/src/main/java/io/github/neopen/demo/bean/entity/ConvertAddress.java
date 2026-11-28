package io.github.neopen.demo.bean.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数映射示例中的嵌入地址。
 *
 * <p>用于演示 {@code @ParamField("address.city")} 形式的嵌套路径映射。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Data
@NoArgsConstructor
@Embeddable
public class ConvertAddress {

    /** 城市。 */
    @Column(name = "city", length = 64)
    private String city;

    /** 详细地址。 */
    @Column(name = "detail", length = 200)
    private String detail;
}
