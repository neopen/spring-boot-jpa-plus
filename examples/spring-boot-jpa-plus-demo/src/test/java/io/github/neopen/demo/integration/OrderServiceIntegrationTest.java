package io.github.neopen.demo.integration;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 订单服务集成测试。
 *
 * <p>同一组断言分别运行在 JPA 与 HQL 两个实现上，确保查询行为一致。</p>
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
    void pageShouldFilterOrdersAndBatchLoadProductsForBothImplementations() throws Exception {
        orderJpaService.add(TestDataFactory.orderParam("INT-PAGE-001", "PAID"));
        orderJpaService.add(TestDataFactory.orderParam("INT-PAGE-002", "PENDING"));
        orderJpaService.add(TestDataFactory.orderParam("OTHER-001", "PAID"));

        OrderQueryParam param = new OrderQueryParam();
        param.setOrderNo("INT-PAGE-");

        assertPage(orderJpaService, param);
        assertPage(orderHqlService, param);
    }

    @Test
    void getDetailShouldReturnProductsForBothImplementations() throws Exception {
        orderJpaService.add(TestDataFactory.orderParam("INT-DETAIL-001", "SHIPPED"));

        assertDetail(orderJpaService);
        assertDetail(orderHqlService);
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
