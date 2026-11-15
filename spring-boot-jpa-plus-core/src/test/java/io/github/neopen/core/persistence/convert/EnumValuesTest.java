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

    /**
     * 验证转换器对未标注常量与大小写不敏感名称的处理。
     */
    @Test
    void shouldConvertWithFallbackNameMatching() {
        StatusConverter converter = new StatusConverter();

        assertThat(converter.convertToDatabaseColumn(Status.NEW))
                .isEqualTo("NEW");
        assertThat(converter.convertToEntityAttribute("paid"))
                .isEqualTo(Status.PAID);
        assertThat(converter.convertToEntityAttribute("New"))
                .isEqualTo(Status.NEW);
    }

    /**
     * 验证转换器遇到未知数据库值时的异常。
     */
    @Test
    void shouldRejectUnknownDatabaseValue() {
        StatusConverter converter = new StatusConverter();

        assertThatThrownBy(() ->
                converter.convertToEntityAttribute("UNKNOWN"))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("UNKNOWN");
    }

    /**
     * 验证方法形式的持久化值同样支持忽略大小写的名称回退。
     */
    @Test
    void shouldFallbackToNameIgnoreCaseForMethodValue() {
        assertThat(EnumValues.fromValue(Channel.class, "alipay"))
                .isEqualTo(Channel.ALIPAY);
        assertThat(EnumValues.fromValue(Channel.class, "WeChat"))
                .isEqualTo(Channel.WECHAT);
    }

    /**
     * 验证枚举类型为空时参数校验。
     */
    @Test
    void shouldRejectNullEnumType() {
        assertThatThrownBy(() -> EnumValues.<Status>fromValue(null, "PAID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Enum type");
    }

    /**
     * 验证重复持久化值在解析阶段被拒绝。
     */
    @Test
    void shouldRejectDuplicatedValues() {
        assertThatThrownBy(() -> EnumValues.toValue(Duplicated.FIRST))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("Duplicated enum value");
    }

    /**
     * 验证常量注解与方法注解不能同时使用。
     */
    @Test
    void shouldRejectMixedDeclarations() {
        assertThatThrownBy(() -> EnumValues.toValue(Conflict.A))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("not both");
    }

    /**
     * 验证空白持久化值被拒绝。
     */
    @Test
    void shouldRejectBlankValue() {
        assertThatThrownBy(() -> EnumValues.toValue(BlankValue.A))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("must not be blank");
    }

    /**
     * 验证多个值方法被拒绝。
     */
    @Test
    void shouldRejectMultipleValueMethods() {
        assertThatThrownBy(() -> EnumValues.toValue(MultipleMethods.A))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("Only one @EnumValue method");
    }

    /**
     * 验证值方法返回 {@code null} 被拒绝。
     */
    @Test
    void shouldRejectNullMethodResult() {
        assertThatThrownBy(() -> EnumValues.toValue(NullValueMethod.A))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("must not return null");
    }

    /** 存在重复持久化值的枚举。 */
    enum Duplicated {

        /** 与下一个常量值重复。 */
        @EnumValue("SAME")
        FIRST,

        /** 与上一个常量值重复。 */
        @EnumValue("SAME")
        SECOND
    }

    /** 同时使用常量注解与方法注解的枚举。 */
    enum Conflict {

        /** 同时带有两种声明。 */
        @EnumValue("A")
        A;

        /**
         * 与常量注解冲突的持久化值方法。
         *
         * @return 持久化值
         */
        @EnumValue
        public String persistedValue() {
            return "M";
        }
    }

    /** 持久化值为空白的枚举。 */
    enum BlankValue {

        /** 声明了空白值。 */
        @EnumValue(" ")
        A
    }

    /** 声明了多个值方法的枚举。 */
    enum MultipleMethods {

        /** 存在两个值方法。 */
        A;

        /**
         * 第一个值方法。
         *
         * @return 持久化值
         */
        @EnumValue
        public String first() {
            return "1";
        }

        /**
         * 第二个值方法。
         *
         * @return 持久化值
         */
        @EnumValue
        public String second() {
            return "2";
        }
    }

    /** 值方法返回 {@code null} 的枚举。 */
    enum NullValueMethod {

        /** 返回空值。 */
        A;

        /**
         * 返回 {@code null} 的持久化值方法。
         *
         * @return 空值
         */
        @EnumValue
        public String persistedValue() {
            return null;
        }
    }
}
