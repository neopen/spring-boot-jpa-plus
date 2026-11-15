package io.github.neopen.core.persistence.convert;

import io.github.neopen.core.exception.JpaPlusException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EnumValues} 与 {@link EnumValueConverter} 单元测试。
 */
class EnumValuesTest {

    /** 使用字段声明持久化值的枚举。 */
    enum Status {

        /** 已支付。 */
        @EnumValue("PAID")
        PAID,

        /** 已取消。 */
        @EnumValue("CANCELLED")
        CANCELLED,

        /** 未标注注解，使用名称。 */
        NEW
    }

    /** 使用无参方法声明持久化值的枚举。 */
    enum Channel {

        /** 支付宝。 */
        ALIPAY(1),

        /** 微信。 */
        WECHAT(2);

        /** 渠道编码。 */
        private final int code;

        /**
         * 创建渠道枚举。
         *
         * @param code 渠道编码
         */
        Channel(int code) {
            this.code = code;
        }

        /**
         * 获取持久化值。
         *
         * @return 渠道编码
         */
        @EnumValue
        public String persistedValue() {
            return String.valueOf(code);
        }
    }

    /** 状态转换器。 */
    static class StatusConverter extends EnumValueConverter<Status> {
    }

    /**
     * 验证标注字段和未标注常量的取值规则。
     */
    @Test
    void shouldResolveAnnotatedAndDefaultValues() {
        assertThat(EnumValues.toValue(Status.PAID)).isEqualTo("PAID");
        assertThat(EnumValues.toValue(Status.NEW)).isEqualTo("NEW");
        assertThat(EnumValues.toValue(null)).isNull();
        assertThat(EnumValues.fromValue(Status.class, "PAID"))
                .isEqualTo(Status.PAID);
        assertThat(EnumValues.fromValue(Status.class, "new"))
                .isEqualTo(Status.NEW);
        assertThat(EnumValues.fromValue(Status.class, null)).isNull();
        assertThat(EnumValues.fromValue(Status.class, "")).isNull();
    }

    /**
     * 验证无参方法形式的持久化值。
     */
    @Test
    void shouldResolveMethodBasedValue() {
        assertThat(EnumValues.toValue(Channel.WECHAT)).isEqualTo("2");
        assertThat(EnumValues.fromValue(Channel.class, "1"))
                .isEqualTo(Channel.ALIPAY);
    }

    /**
     * 验证未知值会抛出异常。
     */
    @Test
    void shouldRejectUnknownValue() {
        assertThatThrownBy(() -> EnumValues.fromValue(Status.class, "NOPE"))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("NOPE");
    }

    /**
     * 验证转换器的双向转换与泛型解析。
     */
    @Test
    void shouldConvertWithAttributeConverter() {
        StatusConverter converter = new StatusConverter();

        assertThat(converter.getEnumType()).isEqualTo(Status.class);
        assertThat(converter.convertToDatabaseColumn(Status.CANCELLED))
                .isEqualTo("CANCELLED");
        assertThat(converter.convertToEntityAttribute("CANCELLED"))
                .isEqualTo(Status.CANCELLED);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    /**
     * 验证未声明泛型的转换器在实例化时失败。
     */
    @Test
    void shouldRejectConverterWithoutGenericType() {
        assertThatThrownBy(RawConverter::new)
                .isInstanceOf(JpaPlusException.class);
    }

    /** 未声明泛型的转换器。 */
    static class RawConverter extends EnumValueConverter {
    }
}
