package io.github.neopen.demo.service.support;

import io.github.neopen.convert.converter.AbstractBaseService;
import io.github.neopen.convert.converter.BaseVoCommom;
import io.github.neopen.demo.bean.entity.Product;
import io.github.neopen.demo.bean.param.ProductParam;
import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.demo.persistence.repo.ProductRepo;
import io.github.neopen.demo.service.ProductService;
import io.github.neopen.extension.response.JsonPageConverter;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品服务公共实现。
 *
 * <p>负责商品写入、字段复制、库存状态计算和视图转换；查询读取由
 * JPA、HQL 与原生 SQL 三个子类分别实现。</p>
 */
public abstract class AbstractProductService
        extends AbstractBaseService<ProductVo, Product, ProductQueryParam>
        implements ProductService {

    /** 有库存状态。 */
    protected static final String STOCK_STATUS_INSTOCK = "INSTOCK";

    /** 库存不足状态。 */
    protected static final String STOCK_STATUS_LOWSTOCK = "LOWSTOCK";

    /** 缺货状态。 */
    protected static final String STOCK_STATUS_OUTOFSTOCK = "OUTOFSTOCK";

    /** 商品数据仓库。 */
    protected final ProductRepo productRepo;

    /** 分页响应转换器。 */
    protected final JsonPageConverter jsonPageConverter;

    /**
     * 创建商品服务公共实现。
     *
     * @param productRepo       商品数据仓库
     * @param jsonPageConverter 分页响应转换器
     */
    protected AbstractProductService(
            ProductRepo productRepo,
            JsonPageConverter jsonPageConverter
    ) {
        this.productRepo = productRepo;
        this.jsonPageConverter = jsonPageConverter;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVo add(ProductParam param) throws Exception {
        Product product = new Product();
        LocalDateTime now = LocalDateTime.now();
        product.setCreateTime(now);
        product.setUpdateTime(now);

        apply(product, param);

        return parseVo(productRepo.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductVo update(Long id, ProductParam param) throws Exception {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        apply(product, param);
        product.setUpdateTime(LocalDateTime.now());

        return parseVo(productRepo.save(product));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (!productRepo.existsById(id)) {
            throw new IllegalArgumentException("商品不存在");
        }

        productRepo.deleteById(id);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends ProductVo> BaseVoCommom<Product, S> baseVo(Product entity) {
        return (t) -> {
            if (t == null) {
                return null;
            }

            ProductVo vo = new ProductVo();
            vo.setId(t.getId());
            vo.setProductName(t.getProductName());
            vo.setUnitPrice(t.getUnitPrice());
            vo.setStockQuantity(t.getStockQuantity());
            vo.setSafetyStock(t.getSafetyStock());
            vo.setProductCode(t.getProductCode());
            vo.setSkuCode(t.getSkuCode());
            vo.setProductType(t.getProductType());
            vo.setProductSpec(t.getProductSpec());
            vo.setProductBrand(t.getProductBrand());
            vo.setManufacturer(t.getManufacturer());
            vo.setProductImage(t.getProductImage());
            vo.setProductDescription(t.getProductDescription());
            vo.setWeight(t.getWeight());
            vo.setVolume(t.getVolume());
            vo.setStockStatus(t.getStockStatus());
            vo.setCreateTime(t.getCreateTime());
            vo.setUpdateTime(t.getUpdateTime());
            vo.setActive(t.getIsActive());
            vo.setLastStockInTime(t.getLastStockInTime());
            vo.setLastStockOutTime(t.getLastStockOutTime());

            return (S) vo;
        };
    }

    /**
     * 将参数复制到实体，并根据库存数量重算库存状态。
     *
     * @param product 目标实体
     * @param param   商品参数
     */
    protected void apply(Product product, ProductParam param) {
        if (param.getProductName() != null) {
            product.setProductName(param.getProductName());
        }
        if (param.getUnitPrice() != null) {
            product.setUnitPrice(param.getUnitPrice());
        }
        if (param.getStockQuantity() != null) {
            product.setStockQuantity(param.getStockQuantity());
        }
        if (param.getSafetyStock() != null) {
            product.setSafetyStock(param.getSafetyStock());
        }
        if (param.getProductCode() != null) {
            product.setProductCode(param.getProductCode());
        }
        if (param.getSkuCode() != null) {
            product.setSkuCode(param.getSkuCode());
        }
        if (param.getProductType() != null) {
            product.setProductType(param.getProductType());
        }
        if (param.getProductSpec() != null) {
            product.setProductSpec(param.getProductSpec());
        }
        if (param.getProductBrand() != null) {
            product.setProductBrand(param.getProductBrand());
        }
        if (param.getManufacturer() != null) {
            product.setManufacturer(param.getManufacturer());
        }
        if (param.getProductImage() != null) {
            product.setProductImage(param.getProductImage());
        }
        if (param.getProductDescription() != null) {
            product.setProductDescription(param.getProductDescription());
        }
        if (param.getWeight() != null) {
            product.setWeight(param.getWeight());
        }
        if (param.getVolume() != null) {
            product.setVolume(param.getVolume());
        }
        if (param.getActive() != null) {
            product.setIsActive(param.getActive());
        } else if (product.getIsActive() == null) {
            product.setIsActive(Boolean.TRUE);
        }

        Integer stock = product.getStockQuantity() == null
                ? 0
                : product.getStockQuantity();
        Integer safety = product.getSafetyStock() == null
                ? 0
                : product.getSafetyStock();

        product.setStockQuantity(stock);
        product.setSafetyStock(safety);
        product.setStockStatus(resolveStockStatus(stock, safety));
    }

    /**
     * 将按库存状态聚合的查询结果转换为视图列表。
     *
     * <p>结果行固定为两列：库存状态和商品数量。</p>
     *
     * @param rows 聚合查询结果
     * @return 库存状态统计列表
     */
    protected List<ProductStockSummaryVo> toStockSummary(List<?> rows) {
        List<ProductStockSummaryVo> result = new ArrayList<>(rows.size());

        for (Object row : rows) {
            Object[] values = (Object[]) row;
            result.add(new ProductStockSummaryVo(
                    values[0] == null ? null : values[0].toString(),
                    ((Number) values[1]).longValue()
            ));
        }

        return result;
    }

    /**
     * 根据库存数量计算库存状态。
     *
     * @param stock  库存数量
     * @param safety 安全库存
     * @return 库存状态
     */
    protected String resolveStockStatus(Integer stock, Integer safety) {
        if (stock == null || stock <= 0) {
            return STOCK_STATUS_OUTOFSTOCK;
        }

        if (safety != null && stock <= safety) {
            return STOCK_STATUS_LOWSTOCK;
        }

        return STOCK_STATUS_INSTOCK;
    }
}
