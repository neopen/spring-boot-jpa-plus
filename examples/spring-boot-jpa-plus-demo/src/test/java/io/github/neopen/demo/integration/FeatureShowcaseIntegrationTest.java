package io.github.neopen.demo.integration;

import io.github.neopen.core.persistence.encrypt.EncryptedFields;
import io.github.neopen.demo.bean.entity.CustomerSecret;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.vo.OrderSummaryVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.CustomerSecretService;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.demo.service.QueryFeatureService;
import io.github.neopen.demo.support.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 0.5 新增能力示例集成测试。
 *
 * <p>覆盖请求分页参数、union 合并、数据源作用域与字段加密。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class FeatureShowcaseIntegrationTest {

    /** 查询能力示例服务。 */
    @Autowired
    private QueryFeatureService queryFeatureService;

    /** 客户敏感信息服务。 */
    @Autowired
    private CustomerSecretService customerSecretService;

    /** 基于 JPA 的订单业务服务。 */
    @Autowired
    @Qualifier("orderJpaService")
    private OrderService orderService;

    /** 订单数据仓库。 */
    @Autowired
    private OrderRepo orderRepo;

    /** 实体管理器。 */
    @Autowired
    private EntityManager entityManager;

    /**
     * 验证 PageParams 页码、每页数量与排序校验。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldPageWithPageParams() throws Exception {
        orderService.add(TestDataFactory.orderParam("PP-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("PP-002", "PAID"));
        orderService.add(TestDataFactory.orderParam("PP-003", "PAID"));

        Page<OrderSummaryVo> page = queryFeatureService.getPage(
                2,
                2,
                "orderNo,asc",
                "PP-"
        );

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent())
                .extracting(OrderSummaryVo::getOrderNo)
                .containsExactly("PP-003");
    }

    /**
     * 验证未配置校验依据外的排序字段被拒绝。
     */
    @Test
    void shouldRejectUnknownSortField() {
        assertThatThrownBy(() -> queryFeatureService.getPage(
                1,
                10,
                "notExists,desc",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证 union 合并已支付与已取消订单。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldUnionPaidAndCancelledOrders() throws Exception {
        orderService.add(TestDataFactory.orderParam("UNI-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("UNI-002", "CANCELLED"));
        orderService.add(TestDataFactory.orderParam("UNI-003", "PENDING"));

        List<OrderSummaryVo> result =
                queryFeatureService.getPaidAndCancelled();

        assertThat(result)
                .filteredOn(item -> item.getOrderNo().startsWith("UNI-"))
                .extracting(OrderSummaryVo::getOrderNo)
                .containsExactlyInAnyOrder("UNI-001", "UNI-002");
    }

    /**
     * 验证归档作用域可以读取已逻辑删除的订单。
     *
     * @throws Exception 新增订单失败时抛出
     */
    @Test
    void shouldIncludeDeletedOrdersInsideScope() throws Exception {
        orderService.add(TestDataFactory.orderParam("SD-001", "PAID"));
        orderService.add(TestDataFactory.orderParam("SD-002", "PAID"));

        Order deleted = orderRepo.findByOrderNo("SD-001").orElseThrow();
        deleted.setDeleted(Boolean.TRUE);
        orderRepo.saveAndFlush(deleted);
        entityManager.clear();

        Page<OrderSummaryVo> filtered = queryFeatureService.getPage(
                1,
                10,
                "orderNo,asc",
                "SD-"
        );

        assertThat(filtered.getContent())
                .extracting(OrderSummaryVo::getOrderNo)
                .containsExactly("SD-002");

        List<OrderSummaryVo> scoped =
                queryFeatureService.listIncludingDeleted("SD-");

        assertThat(scoped)
                .extracting(OrderSummaryVo::getOrderNo)
                .containsExactlyInAnyOrder("SD-001", "SD-002");
    }

    /**
     * 验证随机 IV 与确定性加密在数据库中的表现。
     */
    @Test
    void shouldEncryptSecretFields() {
        String idCard = "110101199001011234";
        String phone = "13800001234";

        CustomerSecret first = customerSecretService.save(
                "C-001",
                idCard,
                phone
        );
        CustomerSecret second = customerSecretService.save(
                "C-002",
                idCard,
                phone
        );

        Object[] firstRow = storedSecret(first.getId());
        Object[] secondRow = storedSecret(second.getId());

        assertThat(firstRow[0]).isNotEqualTo(idCard);
        assertThat(firstRow[0]).isNotEqualTo(secondRow[0]);
        assertThat(firstRow[1])
                .isEqualTo(EncryptedFields.encryptDeterministic(phone));
        assertThat(firstRow[1]).isEqualTo(secondRow[1]);

        List<CustomerSecret> found = customerSecretService
                .findByPhone(phone);

        assertThat(found).hasSize(2);
        assertThat(found)
                .allSatisfy(secret -> {
                    assertThat(secret.getIdCard()).isEqualTo(idCard);
                    assertThat(secret.getPhone()).isEqualTo(phone);
                });
    }

    /**
     * 读取指定客户在数据库中保存的原始密文。
     *
     * @param id 客户主键
     * @return 身份证号密文与手机号密文
     */
    private Object[] storedSecret(Long id) {
        return (Object[]) entityManager
                .createNativeQuery(
                        "select id_card, phone"
                                + " from t_customer_secret where id = :id"
                )
                .setParameter("id", id)
                .getSingleResult();
    }
}
