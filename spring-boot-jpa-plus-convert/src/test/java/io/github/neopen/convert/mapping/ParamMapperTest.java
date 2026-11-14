package io.github.neopen.convert.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ParamMapper} 单元测试。
 */
class ParamMapperTest {

    /**
     * 验证只拷贝非空字段，并完成常见类型转换。
     */
    @Test
    void shouldCopyNonNullFieldsAndConvertTypes() {
        OrderParam param = new OrderParam();
        param.orderNo = "NO-1";
        param.amount = "12.50";
        param.quantity = 3;
        param.status = "PAID";
        param.note = "加急";
        param.token = "secret";
        param.absent = "ignored";

        Order order = ParamMapper.toEntity(param, Order.class);

        assertThat(order.orderNo).isEqualTo("NO-1");
        assertThat(order.amount).isEqualByComparingTo("12.50");
        assertThat(order.quantity).isEqualTo(3);
        assertThat(order.status).isEqualTo(Status.PAID);
        assertThat(order.remark).isEqualTo("加急");
        assertThat(order.token).isNull();
        assertThat(order.createdAt).isEqualTo("keep");
        assertThat(order.id).isEqualTo(100L);
    }

    /**
     * 验证参数中的空字段不会覆盖实体已有值。
     */
    @Test
    void shouldKeepExistingValuesWhenParamFieldIsNull() {
        Order order = new Order();
        order.orderNo = "OLD";
        order.quantity = 9;

        OrderParam param = new OrderParam();
        param.amount = "1.00";

        ParamMapper.apply(param, order);

        assertThat(order.orderNo).isEqualTo("OLD");
        assertThat(order.quantity).isEqualTo(9);
        assertThat(order.amount).isEqualByComparingTo("1.00");
    }

    /**
     * 验证可以通过工厂创建实体。
     */
    @Test
    void shouldCreateEntityThroughFactory() {
        OrderParam param = new OrderParam();
        param.orderNo = "NO-2";

        Order order = ParamMapper.toEntity(param, Order::new);

        assertThat(order.orderNo).isEqualTo("NO-2");
    }

    /**
     * 验证批量转换会跳过空参数。
     */
    @Test
    void shouldConvertCollectionAndSkipNulls() {
        OrderParam first = new OrderParam();
        first.orderNo = "NO-1";

        List<Order> orders = ParamMapper.toEntities(
                Arrays.asList(first, null),
                Order.class
        );

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).orderNo).isEqualTo("NO-1");
        assertThat(ParamMapper.toEntities(null, Order.class)).isEmpty();
    }

    /**
     * 验证实体缺少无参构造器时快速失败。
     */
    @Test
    void shouldRejectEntityWithoutNoArgConstructor() {
        OrderParam param = new OrderParam();
        param.orderNo = "NO-3";

        assertThatThrownBy(
                () -> ParamMapper.toEntity(param, ImmutableOrder.class)
        )
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("a no-arg constructor is required");
    }

    /**
     * 验证无法转换的值抛出明确异常。
     */
    @Test
    void shouldRejectInvalidEnumConversion() {
        OrderParam param = new OrderParam();
        param.status = "UNKNOWN";

        assertThatThrownBy(
                () -> ParamMapper.toEntity(param, Order.class)
        )
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("Cannot convert value to");
    }

    /** 订单状态。 */
    enum Status {
        /** 已支付。 */
        PAID,
        /** 已取消。 */
        CANCELLED
    }

    /** 订单请求参数。 */
    public static class OrderParam {

        /** 订单号。 */
        private String orderNo;

        /** 金额，使用字符串模拟前端传值。 */
        private String amount;

        /** 数量。 */
        private Integer quantity;

        /** 状态。 */
        private String status;

        /** 备注，映射到实体 remark 字段。 */
        @ParamField("remark")
        private String note;

        /** 令牌，不写入实体。 */
        @ParamIgnore
        private String token;

        /** 实体中不存在的字段，应被忽略。 */
        private String absent;
    }

    /** 订单实体。 */
    public static class Order {

        /** 主键。 */
        private Long id = 100L;

        /** 创建时间占位值。 */
        private String createdAt = "keep";

        /** 订单号。 */
        private String orderNo;

        /** 金额。 */
        private BigDecimal amount;

        /** 数量。 */
        private Integer quantity;

        /** 状态。 */
        private Status status;

        /** 备注。 */
        private String remark;

        /** 令牌。 */
        private String token;
    }

    /** 没有无参构造器的实体。 */
    public static class ImmutableOrder {

        /** 订单号。 */
        private final String orderNo;

        /**
         * 创建不可变订单。
         *
         * @param orderNo 订单号
         */
        public ImmutableOrder(String orderNo) {
            this.orderNo = orderNo;
        }
    }
}
