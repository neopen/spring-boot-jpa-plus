package io.github.neopen.demo.integration;

import io.github.neopen.core.persistence.spec.FetchJoinPaging;
import io.github.neopen.core.persistence.spec.SpecWrapper;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.demo.support.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fetch join 分页集成测试。
 *
 * <p>验证先查主键再批量 fetch 关联的分页方式，分页发生在数据库侧，
 * 且返回的订单项已初始化。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class FetchJoinPagingIntegrationTest {

    /** 订单服务。 */
    @Autowired
    @Qualifier("orderJpaService")
    private OrderService orderService;

    /** 实体管理器。 */
    @Autowired
    private EntityManager entityManager;

    /**
     * 验证分页与集合关联 fetch 同时生效。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldPageAndFetchCollectionAssociation() throws Exception {
        orderService.add(TestDataFactory.orderParam("FETCH-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("FETCH-002", "PAID"));

        Page<Order> first = findPage(0, 1);
        Page<Order> second = findPage(1, 1);

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(1);
        assertThat(second.getContent()).hasSize(1);

        assertThat(List.of(
                first.getContent().get(0).getOrderNo(),
                second.getContent().get(0).getOrderNo()
        )).containsExactly("FETCH-001", "FETCH-002");

        assertThat(first.getContent().get(0).getOrderProducts()).hasSize(2);
        assertThat(second.getContent().get(0).getOrderProducts()).hasSize(2);
    }

    /**
     * 验证无匹配数据时返回空页。
     */
    @Test
    void shouldReturnEmptyPageWhenNoMatch() {
        Page<Order> page = findPage(0, 10, "NONE-");

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    /**
     * 按订单编号前缀分页查询并 fetch 订单项。
     *
     * @param page  页码
     * @param size  每页大小
     * @return 分页结果
     */
    private Page<Order> findPage(int page, int size) {
        return findPage(page, size, "FETCH-");
    }

    /**
     * 按订单编号前缀分页查询并 fetch 订单项。
     *
     * @param page          页码
     * @param size          每页大小
     * @param orderNoPrefix 订单编号前缀
     * @return 分页结果
     */
    private Page<Order> findPage(
            int page,
            int size,
            String orderNoPrefix
    ) {
        return FetchJoinPaging.findPage(
                entityManager,
                Order.class,
                "id",
                SpecWrapper.create(Order.class)
                        .like("orderNo", orderNoPrefix)
                        .toSpecification(),
                List.of("orderProducts"),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "id")
                )
        );
    }
}
