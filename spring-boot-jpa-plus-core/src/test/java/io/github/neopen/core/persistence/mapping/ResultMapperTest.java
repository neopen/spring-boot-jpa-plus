package io.github.neopen.core.persistence.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ResultMapper} 单元测试。
 */
class ResultMapperTest {

    /**
     * 验证下划线列名可以映射到驼峰属性。
     */
    @Test
    void shouldMapSnakeCaseAliasToCamelCaseField() {
        OrderDto dto = ResultMapper.mapRow(
                new Object[]{1L, "DS-001", new BigDecimal("12.50")},
                new String[]{"id", "order_no", "TOTAL_AMOUNT"},
                OrderDto.class
        );

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getOrderNo()).isEqualTo("DS-001");
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("12.50");
    }

    /**
     * 验证未知列被忽略，不会影响其余字段。
     */
    @Test
    void shouldIgnoreUnknownColumns() {
        OrderDto dto = ResultMapper.mapRow(
                new Object[]{"DS-002", "ignored"},
                new String[]{"orderNo", "unknown_column"},
                OrderDto.class
        );

        assertThat(dto.getOrderNo()).isEqualTo("DS-002");
        assertThat(dto.getId()).isNull();
    }

    /**
     * 验证没有列名时按属性顺序映射。
     */
    @Test
    void shouldMapPositionallyWithoutAliases() {
        OrderDto dto = ResultMapper.mapRow(
                new Object[]{2L, "DS-003", new BigDecimal("1.00")},
                OrderDto.class
        );

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getOrderNo()).isEqualTo("DS-003");
    }

    /**
     * 验证列数与属性数不一致时快速失败。
     */
    @Test
    void shouldRejectPositionalMappingWithMismatchedColumns() {
        assertThatThrownBy(
                () -> ResultMapper.mapRow(
                        new Object[]{1L},
                        OrderDto.class
                )
        )
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("expected 3 columns");
    }

    /**
     * 验证数值转布尔与字符串转枚举。
     */
    @Test
    void shouldConvertCommonTypes() {
        ConvertedDto dto = ResultMapper.mapRow(
                new Object[]{1, "PAID"},
                new String[]{"active", "status"},
                ConvertedDto.class
        );

        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getStatus()).isEqualTo(Status.PAID);
    }

    /**
     * 验证 setter 优先于字段直接写入。
     */
    @Test
    void shouldPreferSetterWhenPresent() {
        UpperCaseDto dto = ResultMapper.mapRow(
                new Object[]{"ds-004"},
                new String[]{"orderNo"},
                UpperCaseDto.class
        );

        assertThat(dto.getOrderNo()).isEqualTo("DS-004");
    }

    /**
     * 验证单列标量结果可以映射到单字段 DTO。
     */
    @Test
    void shouldMapScalarToSingleFieldDto() {
        CountDto dto = ResultMapper.mapRow(9L, CountDto.class);

        assertThat(dto.getTotal()).isEqualTo(9L);
    }

    /**
     * 验证 Tuple 结果按别名映射。
     */
    @Test
    void shouldMapTupleByAlias() {
        Tuple tuple = mock(Tuple.class);
        TupleElement<?> idElement = mock(TupleElement.class);
        TupleElement<?> orderNoElement = mock(TupleElement.class);

        when(idElement.getAlias()).thenReturn("id");
        when(orderNoElement.getAlias()).thenReturn("order_no");
        when(tuple.getElements()).thenReturn(List.of(idElement, orderNoElement));
        doReturn(3L).when(tuple).get(idElement);
        doReturn("DS-005").when(tuple).get(orderNoElement);

        OrderDto dto = ResultMapper.mapRow(tuple, OrderDto.class);

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getOrderNo()).isEqualTo("DS-005");
    }

    /**
     * 验证缺少无参构造器时快速失败。
     */
    @Test
    void shouldRejectDtoWithoutNoArgConstructor() {
        assertThatThrownBy(
                () -> ResultMapper.mapRow(
                        new Object[]{"x"},
                        new String[]{"orderNo"},
                        NoDefaultConstructorDto.class
                )
        )
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("no-arg constructor");
    }

    /** 订单 DTO。 */
    public static class OrderDto {

        /** 主键。 */
        private Long id;

        /** 订单号。 */
        private String orderNo;

        /** 总金额。 */
        private BigDecimal totalAmount;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }
    }

    /** 类型转换 DTO。 */
    public static class ConvertedDto {

        /** 是否有效。 */
        private boolean active;

        /** 状态。 */
        private Status status;

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
    }

    /** 使用 setter 转换的 DTO。 */
    public static class UpperCaseDto {

        /** 订单号。 */
        private String orderNo;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo == null ? null : orderNo.toUpperCase();
        }
    }

    /** 单字段 DTO。 */
    public static class CountDto {

        /** 总数。 */
        private Long total;

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }
    }

    /** 缺少无参构造器的 DTO。 */
    public static class NoDefaultConstructorDto {

        /** 订单号。 */
        private final String orderNo;

        /**
         * 创建 DTO。
         *
         * @param orderNo 订单号
         */
        public NoDefaultConstructorDto(String orderNo) {
            this.orderNo = orderNo;
        }

        public String getOrderNo() {
            return orderNo;
        }
    }

    /** 状态枚举。 */
    public enum Status {
        /** 新建。 */
        NEW,
        /** 已支付。 */
        PAID
    }
}
