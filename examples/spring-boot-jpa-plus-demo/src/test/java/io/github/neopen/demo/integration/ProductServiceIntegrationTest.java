package io.github.neopen.demo.integration;

import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.demo.service.ProductService;
import io.github.neopen.demo.support.TestDataFactory;
import io.github.neopen.extension.response.Json;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品服务集成测试。
 *
 * <p>同一组断言分别运行在 JPA 与 HQL 两个实现上，确保行为一致。</p>
 */
@Tag("integration")
@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    @Qualifier("productJpaService")
    private ProductService productJpaService;

    @Autowired
    @Qualifier("productHqlService")
    private ProductService productHqlService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void pageShouldFilterByProductNameAndBrandForBothImplementations() throws Exception {
        createProduct("机械键盘", 20, 10, "输入设备", "品牌A");
        createProduct("机械键盘 Pro", 20, 10, "输入设备", "品牌A");
        createProduct("无线鼠标", 20, 10, "输入设备", "品牌A");
        createProduct("机械显示器", 20, 10, "显示设备", "品牌B");

        ProductQueryParam param = new ProductQueryParam();
        param.setProductName("机械");
        param.setProductBrand("品牌A");

        assertPage(productJpaService, param);
        assertPage(productHqlService, param);
    }

    @Test
    void lowStockShouldReturnProductsAtOrBelowSafetyStockForBoth() throws Exception {
        createProduct("低库存商品", 5, 10, "输入设备", "品牌A");
        createProduct("正常库存商品", 20, 10, "输入设备", "品牌A");
        createProduct("缺货商品", 0, 10, "输入设备", "品牌A");

        assertLowStock(productJpaService);
        assertLowStock(productHqlService);
    }

    @Test
    void restockShouldIncreaseStockAndRefreshStatusForBoth() throws Exception {
        Long jpaProductId = createProduct("JPA 补货商品", 5, 10, "输入设备", "品牌A");
        Long hqlProductId = createProduct("HQL 补货商品", 5, 10, "输入设备", "品牌A");

        entityManager.flush();
        entityManager.clear();

        assertThat(productJpaService.restock(List.of(jpaProductId), 20)).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        assertThat(productHqlService.restock(List.of(hqlProductId), 20)).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        assertRestocked(productJpaService.getDetail(jpaProductId));
        assertRestocked(productHqlService.getDetail(hqlProductId));
    }

    @Test
    void stockSummaryShouldCountByStatusForBoth() throws Exception {
        createProduct("缺货商品", 0, 10, "输入设备", "品牌A");
        createProduct("低库存商品", 5, 10, "输入设备", "品牌A");
        createProduct("正常商品A", 20, 10, "输入设备", "品牌A");
        createProduct("正常商品B", 30, 10, "输入设备", "品牌A");

        entityManager.flush();
        entityManager.clear();

        assertSummary(productJpaService);
        assertSummary(productHqlService);
    }

    /**
     * 断言分页过滤结果。
     *
     * @param service 商品服务实现
     * @param param   查询参数
     * @throws Exception 查询失败时抛出
     */
    private void assertPage(ProductService service, ProductQueryParam param) throws Exception {
        Json<List<ProductVo>> result = service.getPage(param);

        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result.getData())
                .extracting(ProductVo::getProductName)
                .containsExactlyInAnyOrder("机械键盘", "机械键盘 Pro");
    }

    /**
     * 断言低库存查询结果。
     *
     * @param service 商品服务实现
     * @throws Exception 查询失败时抛出
     */
    private void assertLowStock(ProductService service) throws Exception {
        List<ProductVo> products = service.getLowStockProducts();

        assertThat(products)
                .extracting(ProductVo::getProductName)
                .containsExactlyInAnyOrder("低库存商品", "缺货商品");
    }

    /**
     * 断言补货后的库存与状态。
     *
     * @param detail 商品详情
     */
    private void assertRestocked(ProductVo detail) {
        assertThat(detail.getStockQuantity()).isEqualTo(25);
        assertThat(detail.getStockStatus()).isEqualTo("INSTOCK");
        assertThat(detail.getLastStockInTime()).isNotNull();
    }

    /**
     * 断言库存状态统计结果。
     *
     * @param service 商品服务实现
     */
    private void assertSummary(ProductService service) {
        Map<String, Long> summary = service.getStockSummary().stream()
                .collect(Collectors.toMap(
                        ProductStockSummaryVo::getStockStatus,
                        ProductStockSummaryVo::getTotal
                ));

        assertThat(summary)
                .containsEntry("OUTOFSTOCK", 1L)
                .containsEntry("LOWSTOCK", 1L)
                .containsEntry("INSTOCK", 2L);
    }

    /**
     * 创建商品并返回主键。
     *
     * @param name   商品名称
     * @param stock  库存数量
     * @param safety 安全库存
     * @param type   产品类型
     * @param brand  产品品牌
     * @return 商品主键
     * @throws Exception 保存失败时抛出
     */
    private Long createProduct(
            String name,
            int stock,
            int safety,
            String type,
            String brand
    ) throws Exception {
        ProductVo vo = productJpaService.add(
                TestDataFactory.productParam(name, stock, safety, type, brand)
        );

        return ((Number) vo.getId()).longValue();
    }
}
