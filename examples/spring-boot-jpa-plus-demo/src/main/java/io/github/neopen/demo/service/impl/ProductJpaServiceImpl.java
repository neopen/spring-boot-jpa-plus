package io.github.neopen.demo.service.impl;

import io.github.neopen.core.persistence.sort.Sorts;
import io.github.neopen.core.persistence.spec.SpecWrapper;
import io.github.neopen.demo.bean.entity.Product;
import io.github.neopen.demo.bean.param.PageParam;
import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.demo.persistence.repo.ProductRepo;
import io.github.neopen.demo.service.support.AbstractProductService;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 基于 Spring Data JPA 的商品服务实现。
 *
 * <p>使用 {@code Specification} 完成动态查询，低库存与补货均基于实体操作。</p>
 */
@Service("productJpaService")
@Primary
public class ProductJpaServiceImpl extends AbstractProductService {

    /**
     * 创建 JPA 商品服务。
     *
     * @param productRepo       商品数据仓库
     * @param jsonPageConverter 分页响应转换器
     */
    public ProductJpaServiceImpl(
            ProductRepo productRepo,
            JsonPageConverter jsonPageConverter
    ) {
        super(productRepo, jsonPageConverter);
    }

    /** {@inheritDoc} */
    @Override
    public ProductVo getDetail(Long id) throws Exception {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        return parseVo(product);
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<ProductVo>> getPage(ProductQueryParam param) throws Exception {
        PageRequest pageable = param == null
                ? PageRequest.of(0, PageParam.DEFAULT_ROW)
                : param.pageRequest(Sorts.parse(
                        param.getSort() + ","
                                + (param.getOrder() > 0 ? "asc" : "desc"),
                        Product.class
                ));

        Page<Product> productPage = productRepo.findAll(
                buildSpecification(param),
                pageable
        );

        return jsonPageConverter.convert(
                batchParseVo(productPage.getContent()),
                productPage.getTotalElements()
        );
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<ProductVo>> getList(ProductQueryParam param) throws Exception {
        return jsonPageConverter.convert(
                batchParseVo(productRepo.findAll(
                        buildSpecification(param),
                        Sort.by(Sort.Direction.ASC, "id")
                ))
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductVo> getLowStockProducts() throws Exception {
        SpecWrapper<Product> wrapper = SpecWrapper.create(Product.class)
                .raw((root, query, cb) -> cb.lessThanOrEqualTo(
                        root.<Integer>get("stockQuantity"),
                        root.<Integer>get("safetyStock")
                ));

        return batchParseVo(productRepo.findAll(
                wrapper.toSpecification(),
                Sort.by(Sort.Direction.ASC, "stockQuantity", "id")
        ));
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductStockSummaryVo> getStockSummary() {
        Map<String, Long> counts = productRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        product -> Objects.toString(product.getStockStatus(), "UNKNOWN"),
                        TreeMap::new,
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .map(entry -> new ProductStockSummaryVo(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int restock(Collection<Long> ids, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("补货数量必须大于 0");
        }

        List<Long> productIds = ids == null
                ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();

        if (productIds.isEmpty()) {
            throw new IllegalArgumentException("商品主键不能为空");
        }

        List<Product> products = productRepo.findAllById(productIds);
        LocalDateTime now = LocalDateTime.now();

        for (Product product : products) {
            int stock = (product.getStockQuantity() == null
                    ? 0
                    : product.getStockQuantity()) + quantity;

            product.setStockQuantity(stock);
            product.setStockStatus(resolveStockStatus(stock, product.getSafetyStock()));
            product.setLastStockInTime(now);
            product.setUpdateTime(now);
        }

        productRepo.saveAll(products);

        return products.size();
    }

    /**
     * 构建商品查询条件。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return JPA 查询规范
     */
    private Specification<Product> buildSpecification(ProductQueryParam param) {
        return SpecWrapper.create(Product.class)
                .apply(param)
                .toSpecification();
    }
}
