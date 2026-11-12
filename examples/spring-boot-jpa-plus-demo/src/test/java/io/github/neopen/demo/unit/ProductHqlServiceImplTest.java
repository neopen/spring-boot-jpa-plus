package io.github.neopen.demo.unit;

import io.github.neopen.core.persistence.hql.HqlWrapper;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.demo.bean.entity.Product;
import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.demo.persistence.repo.ProductRepo;
import io.github.neopen.demo.service.impl.ProductHqlServiceImpl;
import io.github.neopen.demo.support.TestDataFactory;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HQL 商品服务单元测试。
 *
 * <p>不启动 Spring 容器，只验证业务规则与 HQL 执行器调用。</p>
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ProductHqlServiceImplTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private JpaPlusHqlExecutor hqlExecutor;

    @Mock
    private JpaPlusSqlExecutor sqlExecutor;

    @Mock
    private JsonPageConverter jsonPageConverter;

    private ProductHqlServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductHqlServiceImpl(
                productRepo,
                jsonPageConverter,
                hqlExecutor,
                sqlExecutor
        );
    }

    @Test
    void addShouldDeriveLowStockStatus() throws Exception {
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(1L);

            return product;
        });

        ProductVo vo = productService.add(
                TestDataFactory.productParam("机械键盘", 5, 10, "输入设备", "示例品牌")
        );

        assertThat(vo.getStockStatus()).isEqualTo("LOWSTOCK");
        assertThat(vo.getStockQuantity()).isEqualTo(5);
        verify(productRepo).save(any(Product.class));
    }

    @Test
    void addShouldDeriveOutOfStockStatus() throws Exception {
        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(2L);

            return product;
        });

        ProductVo vo = productService.add(
                TestDataFactory.productParam("缺货商品", 0, 10, "输入设备", "示例品牌")
        );

        assertThat(vo.getStockStatus()).isEqualTo("OUTOFSTOCK");
    }

    @Test
    void getPageShouldPassWrapperToHqlExecutor() throws Exception {
        Product product = new Product();
        product.setId(10L);
        product.setProductName("机械键盘");
        product.setUnitPrice(new BigDecimal("59.90"));
        product.setStockQuantity(20);
        product.setSafetyStock(10);
        product.setStockStatus("INSTOCK");
        product.setCreateTime(java.time.LocalDateTime.now());

        Page<Product> page = new PageImpl<>(List.of(product));
        when(hqlExecutor.queryForPage(any(HqlWrapper.class), any(Pageable.class)))
                .thenReturn(page);

        Json<List<ProductVo>> response = Json.<List<ProductVo>>getSucc(List.of());
        doReturn(response).when(jsonPageConverter).convert(anyList(), anyLong());

        ProductQueryParam param = new ProductQueryParam();
        param.setProductName("键盘");
        param.setActive(Boolean.TRUE);

        Json<List<ProductVo>> result = productService.getPage(param);

        assertThat(result).isSameAs(response);
        verify(hqlExecutor).queryForPage(any(HqlWrapper.class), any(Pageable.class));
    }

    @Test
    void getStockSummaryShouldConvertNativeRows() {
        List<?> rows = java.util.Collections.singletonList(
                new Object[]{"INSTOCK", 3L}
        );
        doReturn(rows).when(sqlExecutor).queryForList(anyString(), any(Map.class));

        List<ProductStockSummaryVo> result = productService.getStockSummary();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStockStatus()).isEqualTo("INSTOCK");
        assertThat(result.get(0).getTotal()).isEqualTo(3L);
    }

    @Test
    void restockShouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> productService.restock(List.of(1L), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("补货数量");
    }

    @Test
    void restockShouldDelegateToHqlExecutor() {
        when(hqlExecutor.executeUpdate(contains("update Product"), any(Map.class)))
                .thenReturn(2);

        int updated = productService.restock(List.of(1L, 1L, 2L), 20);

        assertThat(updated).isEqualTo(2);

        ArgumentCaptor<Map<String, Object>> captor =
                ArgumentCaptor.forClass(Map.class);

        verify(hqlExecutor).executeUpdate(contains("update Product"), captor.capture());

        assertThat(captor.getValue())
                .containsEntry("quantity", 20)
                .containsKey("now");
        assertThat((List<?>) captor.getValue().get("ids")).hasSize(2);
    }
}
