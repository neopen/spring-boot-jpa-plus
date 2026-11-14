package io.github.neopen.demo.integration;

import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import io.github.neopen.core.persistence.spec.SpecWrapper;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.demo.support.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v0.5.0 查询扩展集成测试。
 *
 * <p>覆盖原生 SQL / HQL 的 DTO 投影、逻辑删除写侧能力以及审计字段
 * 自动填充。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class QueryExtensionIntegrationTest {

    /** 订单服务。 */
    @Autowired
    @Qualifier("orderJpaService")
    private OrderService orderService;

    /** 实体管理器。 */
    @Autowired
    private EntityManager entityManager;

    /**
     * 恢复审计默认配置。
     */
    @AfterEach
    void resetAudits() {
        Audits.setAuditorSupplier(null);
        Audits.setClock(null);
    }

    /**
     * 验证原生 SQL 结果映射为 DTO。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldMapNativeSqlToDto() throws Exception {
        orderService.add(TestDataFactory.orderParam("DTO-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("DTO-002", "PAID"));

        List<OrderSummaryDto> result = new JpaPlusSqlExecutor(entityManager)
                .queryForDtoList(
                        "select id, order_no as orderNo,"
                                + " total_amount as totalAmount"
                                + " from t_order"
                                + " where order_no like :prefix"
                                + " order by order_no",
                        Map.of("prefix", "DTO-%"),
                        OrderSummaryDto.class
                );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderNo()).isEqualTo("DTO-001");
        assertThat(result.get(0).getTotalAmount())
                .isEqualByComparingTo("199.00");
    }

    /**
     * 验证原生 SQL 分页并映射为 DTO。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldPageNativeSqlDto() throws Exception {
        orderService.add(TestDataFactory.orderParam("DTO-P-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("DTO-P-002", "PAID"));
        orderService.add(TestDataFactory.orderParam("DTO-P-003", "PAID"));

        Page<OrderSummaryDto> page = new JpaPlusSqlExecutor(entityManager)
                .queryForDtoPage(
                        "select order_no as orderNo from t_order"
                                + " where order_no like :prefix"
                                + " order by order_no",
                        Map.of("prefix", "DTO-P-%"),
                        OrderSummaryDto.class,
                        PageRequest.of(0, 2)
                );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(OrderSummaryDto::getOrderNo)
                .containsExactly("DTO-P-001", "DTO-P-002");
    }

    /**
     * 验证 HQL 结果映射为 DTO。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldMapHqlToDto() throws Exception {
        orderService.add(TestDataFactory.orderParam("DTO-H-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("DTO-H-002", "PAID"));

        List<OrderSummaryDto> result = new JpaPlusHqlExecutor(entityManager)
                .queryForDtoList(
                        "select o.orderNo as orderNo,"
                                + " o.totalAmount as totalAmount"
                                + " from Order o"
                                + " where o.orderNo like :prefix"
                                + " order by o.orderNo",
                        Map.of("prefix", "DTO-H-%"),
                        OrderSummaryDto.class
                );

        assertThat(result).hasSize(2);
        assertThat(result.get(1).getOrderNo()).isEqualTo("DTO-H-002");
    }

    /**
     * 验证 HQL 分页并映射为 DTO。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldPageHqlDto() throws Exception {
        orderService.add(TestDataFactory.orderParam("DTO-HP-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("DTO-HP-002", "PAID"));

        Page<OrderSummaryDto> page = new JpaPlusHqlExecutor(entityManager)
                .queryForDtoPage(
                        "select o.orderNo as orderNo from Order o"
                                + " where o.orderNo like :prefix"
                                + " order by o.orderNo",
                        Map.of("prefix", "DTO-HP-%"),
                        OrderSummaryDto.class,
                        PageRequest.of(1, 1)
                );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(OrderSummaryDto::getOrderNo)
                .containsExactly("DTO-HP-002");
    }

    /**
     * 验证按主键逻辑删除后查询不到记录。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldLogicallyDeleteById() throws Exception {
        orderService.add(TestDataFactory.orderParam("LD-W-001", "PAID"));

        JpaPlusSpecExecutor<Order> executor =
                JpaPlusSpecExecutor.of(entityManager, Order.class);
        Order order = findOrder(executor, "LD-W-001");

        assertThat(executor.deleteById(order.getId())).isEqualTo(1);
        assertThat(executor.findById(order.getId())).isNull();
        assertThat(executor.count(
                SpecWrapper.create(Order.class)
                        .like("orderNo", "LD-W-%")
                        .toSpecification()
        )).isZero();
    }

    /**
     * 验证插入时自动填充审计字段。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldFillAuditFieldsOnCreate() throws Exception {
        Audits.setAuditorSupplier(() -> "demo-user");

        orderService.add(TestDataFactory.orderParam("AUDIT-001", "PAID"));

        Order order = findOrder(
                JpaPlusSpecExecutor.of(entityManager, Order.class),
                "AUDIT-001"
        );

        assertThat(order.getCreateTime()).isNotNull();
        assertThat(order.getUpdateTime()).isNotNull();
        assertThat(order.getCreatedBy()).isEqualTo("demo-user");
        assertThat(order.getUpdatedBy()).isEqualTo("demo-user");
    }

    /**
     * 按订单号查询订单。
     *
     * @param executor 执行器
     * @param orderNo  订单号
     * @return 订单实体
     */
    private Order findOrder(
            JpaPlusSpecExecutor<Order> executor,
            String orderNo
    ) {
        return executor.findAll(
                SpecWrapper.create(Order.class)
                        .eq("orderNo", orderNo)
                        .toSpecification()
        ).get(0);
    }

    /** 订单摘要 DTO。 */
    public static class OrderSummaryDto {

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
}
