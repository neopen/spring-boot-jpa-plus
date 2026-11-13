package io.github.neopen.core.persistence.condition;

import io.github.neopen.core.persistence.hql.HqlWrapper;
import io.github.neopen.core.persistence.sql.SqlWrapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link QueryConditionMapper} 单元测试。
 */
class QueryConditionMapperTest {

    /**
     * 验证注解字段按声明顺序映射为条件。
     */
    @Test
    void shouldMapAnnotatedFieldsInOrder() {
        SampleParam param = new SampleParam();
        param.setName("机械");
        param.setStatus("PAID");
        param.setMinAmount(10);
        param.setMaxAmount(20);
        param.setIds(List.of(1L, 2L));
        param.setHasRemark(true);
        param.setTenantId(9L);

        assertThat(collect(param)).containsExactlyInAnyOrder(
                "name|LIKE|机械|null",
                "status_code|EQ|PAID|null",
                "minAmount|BETWEEN|10|20",
                "ids|IN|[1, 2]|null",
                "hasRemark|IS_NOT_NULL|null|null",
                "tenant_id|EQ|9|null"
        );
    }

    /**
     * 验证空值、空白、空集合与关闭状态被忽略。
     */
    @Test
    void shouldSkipEmptyValues() {
        SampleParam param = new SampleParam();
        param.setName("  ");
        param.setIds(List.of());
        param.setHasRemark(false);

        assertThat(collect(param)).isEmpty();
    }

    /**
     * 验证参数为空时不做任何处理。
     */
    @Test
    void shouldIgnoreNullParam() {
        List<String> conditions = new ArrayList<>();

        QueryConditionMapper.apply(
                null,
                (field, operator, value, second) ->
                        conditions.add(field)
        );

        assertThat(conditions).isEmpty();
    }

    /**
     * 验证 BETWEEN 未配置结束字段时抛出异常。
     */
    @Test
    void shouldRejectBetweenWithoutEndField() {
        InvalidParam param = new InvalidParam();
        param.setAmount(1);

        assertThatThrownBy(() -> collect(param))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endField");
    }

    /**
     * 验证 HQL 构造器接入注解映射并补齐模糊通配符。
     */
    @Test
    void shouldApplyConditionsToHqlWrapper() {
        RangeParam param = new RangeParam();
        param.setName("机械");
        param.setMinAmount(10);
        param.setMaxAmount(20);

        var query = HqlWrapper.create(SampleEntity.class)
                .apply(param)
                .build();

        assertThat(query.getHql()).isEqualTo(
                "select o from SampleEntity o"
                        + " where o.name like :p1"
                        + " and o.minAmount between :p2 and :p3"
        );
        assertThat(query.getParameters())
                .containsEntry("p1", "%机械%")
                .containsEntry("p2", 10)
                .containsEntry("p3", 20);
    }

    /**
     * 验证 SQL 构造器接入注解映射并自动转换为下划线列名。
     */
    @Test
    void shouldApplyConditionsToSqlWrapperWithSnakeCase() {
        SqlParam param = new SqlParam();
        param.setOrderNo("SO-1");
        param.setMinTotalAmount(100);
        param.setMaxTotalAmount(200);

        var query = SqlWrapper.create("t_order")
                .apply(param)
                .build();

        assertThat(query.getSql()).isEqualTo(
                "select o.* from t_order o"
                        + " where o.order_no like :p1"
                        + " and o.total_amount between :p2 and :p3"
        );
        assertThat(query.getParameters())
                .containsEntry("p1", "%SO-1%")
                .containsEntry("p2", 100)
                .containsEntry("p3", 200);
    }

    /**
     * 验证嵌套参数对象中的条件被递归解析。
     */
    @Test
    void shouldResolveNestedParamConditions() {
        NestedParam param = new NestedParam();
        param.keyword = "机械";

        InnerParam inner = new InnerParam();
        inner.tenantId = 9L;
        DeepParam deep = new DeepParam();
        deep.status = "PAID";
        inner.deep = deep;
        param.inner = inner;

        assertThat(collect(param)).containsExactly(
                "keyword|LIKE|机械|null",
                "tenant_id|EQ|9|null",
                "status_code|EQ|PAID|null"
        );
    }

    /**
     * 验证循环引用的参数对象不会无限递归。
     */
    @Test
    void shouldStopRecursionOnCyclicParam() {
        CyclicParam root = new CyclicParam();
        root.name = "root";
        CyclicParam child = new CyclicParam();
        child.name = "child";
        root.child = child;
        child.child = root;

        assertThat(collect(root)).containsExactly(
                "name|EQ|root|null",
                "name|EQ|child|null"
        );
    }

    /**
     * 验证注解分组名被正确解析。
     */
    @Test
    void shouldExposeGroupNameFromAnnotation() {
        GroupParam param = new GroupParam();
        param.status = "PAID";
        param.orderNo = "SO";
        param.receiver = "张";

        assertThat(QueryConditionMapper.resolve(param))
                .extracting(QueryConditionEntry::group)
                .containsExactly("", "keyword", "keyword");
    }

    /**
     * 验证同组条件在 HQL 构造器中组合为 OR 分组。
     */
    @Test
    void shouldApplyGroupedConditionsAsOrBranch() {
        GroupParam param = new GroupParam();
        param.status = "PAID";
        param.orderNo = "SO";
        param.receiver = "张";

        var query = HqlWrapper.create(SampleEntity.class)
                .apply(param)
                .build();

        assertThat(query.getHql()).isEqualTo(
                "select o from SampleEntity o"
                        + " where o.status = :p1"
                        + " and (o.orderNo like :p2"
                        + " or o.receiver like :p3)"
        );
        assertThat(query.getParameters())
                .containsEntry("p2", "%SO%")
                .containsEntry("p3", "%张%");
    }

    /**
     * 收集参数映射出的条件描述。
     *
     * @param param 查询参数
     * @return 条件描述列表
     */
    private static List<String> collect(Object param) {
        List<String> conditions = new ArrayList<>();

        QueryConditionMapper.apply(
                param,
                (field, operator, value, second) -> conditions.add(
                        field + "|" + operator + "|" + value + "|" + second
                )
        );

        return conditions;
    }

    /** 继承自基类的查询参数。 */
    private static class BaseParam {

        /** 租户主键。 */
        @QueryCondition(value = "tenant_id")
        private Long tenantId;

        /**
         * 获取租户主键。
         *
         * @return 租户主键
         */
        public Long getTenantId() {
            return tenantId;
        }

        /**
         * 设置租户主键。
         *
         * @param tenantId 租户主键
         */
        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }
    }

    /** 完整查询参数。 */
    private static class SampleParam extends BaseParam {

        /** 名称，模糊匹配。 */
        @QueryCondition(operator = ConditionOperator.LIKE)
        private String name;

        /** 状态，显式指定列名。 */
        @QueryCondition(value = "status_code")
        private String status;

        /** 最小金额。 */
        @QueryCondition(
                operator = ConditionOperator.BETWEEN,
                endField = "maxAmount"
        )
        private Integer minAmount;

        /** 最大金额。 */
        private Integer maxAmount;

        /** 主键集合。 */
        @QueryCondition(operator = ConditionOperator.IN)
        private List<Long> ids;

        /** 是否存在备注。 */
        @QueryCondition(operator = ConditionOperator.IS_NOT_NULL)
        private Boolean hasRemark;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(Integer minAmount) {
            this.minAmount = minAmount;
        }

        public Integer getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(Integer maxAmount) {
            this.maxAmount = maxAmount;
        }

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public Boolean getHasRemark() {
            return hasRemark;
        }

        public void setHasRemark(Boolean hasRemark) {
            this.hasRemark = hasRemark;
        }
    }

    /** 非法的区间参数。 */
    private static class InvalidParam {

        /** 金额。 */
        @QueryCondition(operator = ConditionOperator.BETWEEN)
        private Integer amount;

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }
    }

    /** HQL 测试参数。 */
    private static class RangeParam {

        /** 名称。 */
        @QueryCondition(operator = ConditionOperator.LIKE)
        private String name;

        /** 最小金额。 */
        @QueryCondition(
                operator = ConditionOperator.BETWEEN,
                endField = "maxAmount"
        )
        private Integer minAmount;

        /** 最大金额。 */
        private Integer maxAmount;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getMinAmount() {
            return minAmount;
        }

        public void setMinAmount(Integer minAmount) {
            this.minAmount = minAmount;
        }

        public Integer getMaxAmount() {
            return maxAmount;
        }

        public void setMaxAmount(Integer maxAmount) {
            this.maxAmount = maxAmount;
        }
    }

    /** SQL 测试参数。 */
    private static class SqlParam {

        /** 订单编号。 */
        @QueryCondition(operator = ConditionOperator.LIKE)
        private String orderNo;

        /** 最小订单金额。 */
        @QueryCondition(
                value = "totalAmount",
                operator = ConditionOperator.BETWEEN,
                endField = "maxTotalAmount"
        )
        private Integer minTotalAmount;

        /** 最大订单金额。 */
        private Integer maxTotalAmount;

        public String getOrderNo() {
            return orderNo;
        }

        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }

        public Integer getMinTotalAmount() {
            return minTotalAmount;
        }

        public void setMinTotalAmount(Integer minTotalAmount) {
            this.minTotalAmount = minTotalAmount;
        }

        public Integer getMaxTotalAmount() {
            return maxTotalAmount;
        }

        public void setMaxTotalAmount(Integer maxTotalAmount) {
            this.maxTotalAmount = maxTotalAmount;
        }
    }

    /** 嵌套查询参数。 */
    private static class NestedParam {

        /** 关键字。 */
        @QueryCondition(operator = ConditionOperator.LIKE)
        private String keyword;

        /** 内层参数。 */
        private InnerParam inner;
    }

    /** 内层查询参数。 */
    private static class InnerParam {

        /** 租户主键。 */
        @QueryCondition(value = "tenant_id")
        private Long tenantId;

        /** 深层参数。 */
        private DeepParam deep;
    }

    /** 深层查询参数。 */
    private static class DeepParam {

        /** 状态。 */
        @QueryCondition(value = "status_code")
        private String status;
    }

    /** 循环引用查询参数。 */
    private static class CyclicParam {

        /** 名称。 */
        @QueryCondition
        private String name;

        /** 子节点。 */
        private CyclicParam child;
    }

    /** 带条件分组的查询参数。 */
    private static class GroupParam {

        /** 状态。 */
        @QueryCondition
        private String status;

        /** 订单编号，与收货人同组。 */
        @QueryCondition(group = "keyword", operator = ConditionOperator.LIKE)
        private String orderNo;

        /** 收货人，与订单编号同组。 */
        @QueryCondition(group = "keyword", operator = ConditionOperator.LIKE)
        private String receiver;
    }

    /** HQL 测试实体。 */
    private static class SampleEntity {
    }
}
