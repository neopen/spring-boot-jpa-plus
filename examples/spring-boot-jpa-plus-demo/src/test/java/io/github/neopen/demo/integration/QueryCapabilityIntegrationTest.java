package io.github.neopen.demo.integration;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import io.github.neopen.core.persistence.spec.SpecWrapper;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.demo.support.TestDataFactory;
import io.github.neopen.extension.response.Json;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 查询能力集成测试。
 *
 * <p>覆盖逻辑删除过滤、Specification 查询执行器、条件分组与嵌套
 * 参数对象。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class QueryCapabilityIntegrationTest {

    /** 订单服务。 */
    @Autowired
    @Qualifier("orderJpaService")
    private OrderService orderService;

    /** 订单仓库。 */
    @Autowired
    private OrderRepo orderRepo;

    /** 实体管理器。 */
    @Autowired
    private EntityManager entityManager;

    /**
     * 验证 JPA 查询自动排除逻辑删除的订单。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldExcludeLogicallyDeletedOrders() throws Exception {
        orderService.add(TestDataFactory.orderParam("LD-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("LD-002", "PAID"));

        Order deleted = orderRepo.findByOrderNo("LD-001")
                .orElseThrow();
        deleted.setDeleted(Boolean.TRUE);
        orderRepo.saveAndFlush(deleted);
        entityManager.clear();

        OrderQueryParam param = new OrderQueryParam();
        param.setOrderNo("LD-");

        Json<List<OrderVo>> result = orderService.getPage(param);

        assertThat(result.getCount()).isEqualTo(1);
        assertThat(result.getData())
                .extracting(OrderVo::getOrderNo)
                .containsExactly("LD-002");
    }

    /**
     * 验证查询执行器的分页与切片。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldPageAndSliceWithSpecExecutor() throws Exception {
        orderService.add(TestDataFactory.orderParam("EX-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("EX-002", "PAID"));
        orderService.add(TestDataFactory.orderParam("EX-003", "PAID"));

        JpaPlusSpecExecutor<Order> executor =
                JpaPlusSpecExecutor.of(entityManager, Order.class, "id");
        Specification<Order> specification = SpecWrapper
                .create(Order.class)
                .like("orderNo", "EX-")
                .toSpecification();

        Page<Order> page = executor.findPage(
                specification,
                PageRequest.of(1, 2, Sort.by("orderNo"))
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(Order::getOrderNo)
                .containsExactly("EX-003");

        Slice<Order> slice = executor.findSlice(
                specification,
                PageRequest.of(0, 2, Sort.by("orderNo"))
        );

        assertThat(slice.getContent())
                .extracting(Order::getOrderNo)
                .containsExactly("EX-001", "EX-002");
        assertThat(slice.hasNext()).isTrue();
    }

    /**
     * 验证同组条件按 OR 组合、分组之间按 AND 组合。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldApplyGroupedConditionsAsOrBranch() throws Exception {
        orderService.add(TestDataFactory.orderParam("GP-001", "PAID"));

        OrderParam special = TestDataFactory.orderParam("GP-002", "PAID");
        special.setReceiverName("特殊收货人");
        orderService.add(special);

        OrderParam cancelled = TestDataFactory.orderParam("GP-003", "CANCELLED");
        cancelled.setReceiverName("特殊收货人");
        orderService.add(cancelled);

        GroupOrderParam param = new GroupOrderParam();
        param.status = "PAID";
        param.orderNo = "GP-001";
        param.receiverName = "特殊";

        List<Order> orders = orderRepo.findAll(
                SpecWrapper.create(Order.class)
                        .apply(param)
                        .toSpecification()
        );

        assertThat(orders)
                .extracting(Order::getOrderNo)
                .containsExactlyInAnyOrder("GP-001", "GP-002");
    }

    /**
     * 验证嵌套参数对象中的条件被识别。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldApplyNestedParamConditions() throws Exception {
        orderService.add(TestDataFactory.orderParam("NP-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("NP-002", "PAID"));
        orderService.add(TestDataFactory.orderParam("NP-003", "CANCELLED"));

        NestedOrderParam param = new NestedOrderParam();
        param.status = "PAID";
        param.keyword = new KeywordPart();
        param.keyword.orderNo = "NP-00";

        List<Order> orders = orderRepo.findAll(
                SpecWrapper.create(Order.class)
                        .apply(param)
                        .toSpecification()
        );

        assertThat(orders)
                .extracting(Order::getOrderNo)
                .containsExactlyInAnyOrder("NP-001", "NP-002");
    }

    /** 带条件分组的订单查询参数。 */
    private static class GroupOrderParam {

        /** 订单状态。 */
        @QueryCondition
        private String status;

        /** 订单编号，与收货人同组。 */
        @QueryCondition(
                group = "keyword",
                operator = ConditionOperator.LIKE
        )
        private String orderNo;

        /** 收货人，与订单编号同组。 */
        @QueryCondition(
                group = "keyword",
                operator = ConditionOperator.LIKE
        )
        private String receiverName;
    }

    /** 嵌套订单查询参数。 */
    private static class NestedOrderParam {

        /** 订单状态。 */
        @QueryCondition
        private String status;

        /** 关键字条件。 */
        private KeywordPart keyword;
    }

    /** 关键字条件。 */
    private static class KeywordPart {

        /** 订单编号。 */
        @QueryCondition(operator = ConditionOperator.LIKE)
        private String orderNo;
    }
}
