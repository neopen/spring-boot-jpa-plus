/*
 * Copyright 2026 neopen
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
package io.github.neopen.convert.mapping;

import io.github.neopen.core.exception.JpaPlusException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.Type;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ParamMapping} 扩展能力单元测试。
 */
class ParamMappingTest {

    /**
     * 验证参数可以通过 JPA 列名别名匹配实体字段。
     */
    @Test
    void shouldMatchColumnAlias() {
        ColumnParam param = new ColumnParam();
        param.orderNo = "NO-1";

        ColumnOrder order = ParamMapper.toEntity(
                param,
                ColumnOrder.class
        );

        assertThat(order.number).isEqualTo("NO-1");
    }

    /**
     * 验证点号路径可以写入嵌套对象。
     */
    @Test
    void shouldMapNestedPath() {
        NestedParam param = new NestedParam();
        param.city = "Shanghai";

        NestedOrder order = ParamMapper.toEntity(
                param,
                NestedOrder.class
        );

        assertThat(order.address).isNotNull();
        assertThat(order.address.city).isEqualTo("Shanghai");
    }

    /**
     * 验证 SET_NULL 策略会清空实体已有值。
     */
    @Test
    void shouldClearValuesWithSetNullPolicy() {
        SimpleOrder order = new SimpleOrder();
        order.orderNo = "OLD";
        order.quantity = 9;

        ParamMapper.mapper(SimpleParam.class, SimpleOrder.class)
                .nullPolicy(NullPolicy.SET_NULL)
                .apply(new SimpleParam(), order);

        assertThat(order.orderNo).isNull();
        assertThat(order.quantity).isNull();
    }

    /**
     * 验证严格模式会在存在未匹配字段时失败。
     */
    @Test
    void shouldRejectUnknownFieldsInStrictMode() {
        SimpleParam param = new SimpleParam();
        param.orderNo = "NO-1";
        param.unknown = "x";

        assertThatThrownBy(() -> ParamMapper
                .mapper(SimpleParam.class, SimpleOrder.class)
                .strict(true)
                .apply(param, new SimpleOrder()))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("unknown");
    }

    /**
     * 验证映射结果记录未匹配字段与转换字段。
     */
    @Test
    void shouldReportMappingDetails() {
        SimpleParam param = new SimpleParam();
        param.orderNo = "NO-1";
        param.amount = "12.50";
        param.status = "PAID";
        param.quantity = 3;
        param.unknown = "x";

        MappingResult<SimpleOrder> result = ParamMapper
                .mapper(SimpleParam.class, SimpleOrder.class)
                .map(param, new SimpleOrder());

        assertThat(result.entity().orderNo).isEqualTo("NO-1");
        assertThat(result.entity().amount).isEqualByComparingTo("12.50");
        assertThat(result.entity().status).isEqualTo(Status.PAID);
        assertThat(result.unknownFields()).containsExactly("unknown");
        assertThat(result.convertedFields())
                .contains("amount", "status")
                .doesNotContain("orderNo", "quantity");
    }

    /**
     * 验证字符串可以按注解格式与默认格式写入时间字段。
     */
    @Test
    void shouldConvertStringToTemporalTypes() {
        DateParam param = new DateParam();
        param.orderDate = "2026-01-02";
        param.createdAt = "2026-01-02T03:04:05";

        DateOrder order = ParamMapper.toEntity(
                param,
                DateOrder.class
        );

        assertThat(order.orderDate).isEqualTo(LocalDate.of(2026, 1, 2));
        assertThat(order.createdAt).isEqualTo(
                LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        );
    }

    /**
     * 验证集合元素会按泛型类型转换。
     */
    @Test
    void shouldConvertCollectionElements() {
        TagParam param = new TagParam();
        param.tagIds = List.of("1", "2");

        TagOrder order = ParamMapper.toEntity(param, TagOrder.class);

        assertThat(order.tagIds).containsExactly(1L, 2L);
    }

    /**
     * 验证开启 ignoreJpaManaged 后主键与版本号不会被覆盖。
     */
    @Test
    void shouldIgnoreJpaManagedFields() {
        VersionedParam param = new VersionedParam();
        param.id = 99L;
        param.version = 99L;
        param.orderNo = "NEW";

        VersionedOrder order = new VersionedOrder();

        ParamMapper.mapper(VersionedParam.class, VersionedOrder.class)
                .ignoreJpaManaged(true)
                .apply(param, order);

        assertThat(order.id).isEqualTo(1L);
        assertThat(order.version).isEqualTo(2L);
        assertThat(order.orderNo).isEqualTo("NEW");
    }

    /**
     * 验证 JPA {@link Convert} 优先于内置转换。
     */
    @Test
    void shouldUseJpaAttributeConverter() {
        CodeParam param = new CodeParam();
        param.code = "abc";

        CodeOrder order = ParamMapper.toEntity(param, CodeOrder.class);

        assertThat(order.code).isEqualTo("ABC");
    }

    /**
     * 验证外键 ID 会转换为实体引用。
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldResolveParamReference() {
        EntityManager manager = mock(EntityManager.class);
        Metamodel metamodel = mock(Metamodel.class);
        EntityType entityType = mock(EntityType.class);
        Type idType = mock(Type.class);

        when(manager.getMetamodel()).thenReturn(metamodel);
        when(metamodel.entity(Customer.class)).thenReturn(entityType);
        when(entityType.getIdType()).thenReturn(idType);
        when(idType.getJavaType()).thenReturn(Long.class);

        Customer customer = new Customer();

        when(manager.getReference(Customer.class, 7L))
                .thenReturn(customer);

        RefOrder order = ParamMapper.toEntity(
                new RefParam(7L),
                RefOrder.class,
                manager
        );

        assertThat(order.customer).isSameAs(customer);
    }

    /**
     * 验证缺少 EntityManager 时关联映射快速失败。
     */
    @Test
    void shouldRejectReferenceWithoutEntityManager() {
        assertThatThrownBy(() -> ParamMapper.toEntity(
                new RefParam(7L),
                RefOrder.class
        ))
                .isInstanceOf(JpaPlusException.class)
                .hasMessageContaining("EntityManager");
    }

    /** 列名别名订单。 */
    public static class ColumnOrder {

        /** 订单号，数据库列名为 order_no。 */
        @Column(name = "order_no")
        private String number;
    }

    /** 列名别名参数。 */
    public static class ColumnParam {

        /** 订单号。 */
        private String orderNo;
    }

    /** 嵌套地址。 */
    public static class Address {

        /** 城市。 */
        private String city;
    }

    /** 嵌套订单。 */
    public static class NestedOrder {

        /** 地址。 */
        private Address address;
    }

    /** 嵌套参数。 */
    public static class NestedParam {

        /** 城市。 */
        @ParamField("address.city")
        private String city;
    }

    /** 简单订单。 */
    public static class SimpleOrder {

        /** 订单号。 */
        private String orderNo;

        /** 金额。 */
        private BigDecimal amount;

        /** 状态。 */
        private Status status;

        /** 数量。 */
        private Integer quantity;
    }

    /** 简单参数。 */
    public static class SimpleParam {

        /** 订单号。 */
        private String orderNo;

        /** 金额。 */
        private String amount;

        /** 状态。 */
        private String status;

        /** 数量。 */
        private Integer quantity;

        /** 实体中不存在的字段。 */
        private String unknown;
    }

    /** 状态枚举。 */
    public enum Status {

        /** 已支付。 */
        PAID,

        /** 已取消。 */
        CANCELLED
    }

    /** 时间订单。 */
    public static class DateOrder {

        /** 订单日期。 */
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate orderDate;

        /** 创建时间。 */
        private LocalDateTime createdAt;
    }

    /** 时间参数。 */
    public static class DateParam {

        /** 订单日期。 */
        private String orderDate;

        /** 创建时间。 */
        private String createdAt;
    }

    /** 标签订单。 */
    public static class TagOrder {

        /** 标签 ID。 */
        private List<Long> tagIds;
    }

    /** 标签参数。 */
    public static class TagParam {

        /** 标签 ID。 */
        private List<String> tagIds;
    }

    /** 带版本号的订单。 */
    public static class VersionedOrder {

        /** 主键。 */
        @Id
        private Long id = 1L;

        /** 版本号。 */
        @Version
        private Long version = 2L;

        /** 订单号。 */
        private String orderNo;
    }

    /** 带版本号的参数。 */
    public static class VersionedParam {

        /** 主键。 */
        private Long id;

        /** 版本号。 */
        private Long version;

        /** 订单号。 */
        private String orderNo;
    }

    /** 编码订单。 */
    public static class CodeOrder {

        /** 编码。 */
        @Convert(converter = UpperConverter.class)
        private String code;
    }

    /** 编码参数。 */
    public static class CodeParam {

        /** 编码。 */
        private String code;
    }

    /**
     * 转大写的 JPA 属性转换器。
     */
    public static class UpperConverter
            implements AttributeConverter<String, String> {

        /**
         * 写入数据库。
         *
         * @param attribute 属性值
         * @return 数据库值
         */
        @Override
        public String convertToDatabaseColumn(String attribute) {
            return attribute;
        }

        /**
         * 从数据库读取。
         *
         * @param dbData 数据库值
         * @return 属性值
         */
        @Override
        public String convertToEntityAttribute(String dbData) {
            return dbData == null ? null : dbData.toUpperCase();
        }
    }

    /** 客户实体。 */
    public static class Customer {

        /** 主键。 */
        private Long id;
    }

    /** 关联订单。 */
    public static class RefOrder {

        /** 客户。 */
        private Customer customer;
    }

    /** 关联参数。 */
    public static class RefParam {

        /** 客户 ID。 */
        @ParamReference("customer")
        private Long customerId;

        /**
         * 创建关联参数。
         *
         * @param customerId 客户 ID
         */
        public RefParam(Long customerId) {
            this.customerId = customerId;
        }
    }
}
