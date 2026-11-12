package io.github.neopen.demo.integration;

import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderProductVo;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.demo.support.TestDataFactory;
import io.github.neopen.extension.response.Json;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单服务集成测试。
 *
 * <p>同一组断言分别运行在 JPA、HQL 与原生 SQL 三个实现上，
 * 确保查询行为一致。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    @Qualifier("orderJpaService")
    private OrderService orderJpaService;

    @Autowired
    @Qualifier("orderHqlService")
    private OrderService orderHqlService;

    @Autowired
    @Qualifier("orderSqlService")
    private OrderService orderSqlService;

    @Test
    void addShouldSaveOrderWithProducts() throws Exception {
        OrderVo vo = orderJpaService.add(
                TestDataFactory.orderParam("INT-ADD-001", "PAID")
        );

        assertThat(vo.getId()).isNotNull();
        assertThat(vo.getOrderNo()).isEqualTo("INT-ADD-001");
        assertThat(vo.getOrderProducts()).hasSize(2);
    }

    @Test
    void pageShouldFilterOrdersAndBatchLoadProductsForAllImplementations() throws Exception {
        orderJpaService.add(TestDataFactory.orderParam("INT-PAGE-001", "PAID"));
        orderJpaService.add(TestDataFactory.orderParam("INT-PAGE-002", "PENDING"));
        orderJpaService.add(TestDataFactory.orderParam("OTHER-001", "PAID"));

        OrderQueryParam param = new OrderQueryParam();
        param.setOrderNo("INT-PAGE-");

        assertPage(orderJpaService, param);
        assertPage(orderHqlService, param);
        assertPage(orderSqlService, param);
    }

    @Test
    void getDetailShouldReturnProductsForAllImplementations() throws Exception {
        orderJpaService.add(TestDataFactory.orderParam("INT-DETAIL-001", "SHIPPED"));

        assertDetail(orderJpaService);
        assertDetail(orderHqlService);
        assertDetail(orderSqlService);
    }

    @Test
    void jpaBuildPredicatesShouldJoinOrderItemsAndFilterByAmount() throws Exception {
        OrderParam matching = TestDataFactory.orderParam("JPA-JOIN-001", "PAID");
        matching.setTotalAmount(new BigDecimal("500.00"));
        matching.setSenderPhone("13900000001");
        matching.setOrderProducts(List.of(
                new OrderProductParam(
                        1L,
                        "机械键盘",
                        new BigDecimal("99.00"),
                        5,
                        "P-001"
                ),
                new OrderProductParam(
                        2L,
                        "机械鼠标",
                        new BigDecimal("100.00"),
                        3,
                        "P-002"
                )
        ));
        orderJpaService.add(matching);

        OrderParam other = TestDataFactory.orderParam("JPA-JOIN-002", "PAID");
        other.setTotalAmount(new BigDecimal("100.00"));
        other.setSenderPhone("13900000002");
        other.setOrderProducts(List.of(
                new OrderProductParam(
                        3L,
                        "无线鼠标",
                        new BigDecimal("100.00"),
                        1,
                        "P-003"
                )
        ));
        orderJpaService.add(other);

        OrderQueryParam param = new OrderQueryParam();
        param.setProductName("机械");
        param.setMinQuantity(2);
        param.setMinTotalAmount(new BigDecimal("200.00"));
        param.setMaxTotalAmount(new BigDecimal("600.00"));
        param.setSenderPhone("13900000001");

        Json<List<OrderVo>> result = orderJpaService.getPage(param);

        assertThat(result.getCount()).isEqualTo(1);
        assertThat(result.getData())
                .extracting(OrderVo::getOrderNo)
                .containsExactly("JPA-JOIN-001");
        assertThat(result.getData().get(0).getOrderProducts()).hasSize(2);
    }

    /**
     * 断言分页过滤结果。
     *
     * @param service 订单服务实现
     * @param param   查询参数
     * @throws Exception 查询失败时抛出
     */
    private void assertPage(OrderService service, OrderQueryParam param) throws Exception {
        Json<List<OrderVo>> result = service.getPage(param);

        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.getData())
                .extracting(OrderVo::getOrderNo)
                .containsExactlyInAnyOrder("INT-PAGE-001", "INT-PAGE-002");
        assertThat(result.getData())
                .allSatisfy(vo -> assertThat(vo.getOrderProducts()).hasSize(2));
    }

    /**
     * 断言订单详情与订单项。
     *
     * @param service 订单服务实现
     * @throws Exception 查询失败时抛出
     */
    private void assertDetail(OrderService service) throws Exception {
        OrderVo detail = service.getDetail("INT-DETAIL-001");

        assertThat(detail.getOrderProducts()).hasSize(2);
        assertThat(detail.getOrderProducts())
                .extracting(OrderProductVo::getProductName)
                .containsExactlyInAnyOrder("机械键盘", "无线鼠标");
    }
}
