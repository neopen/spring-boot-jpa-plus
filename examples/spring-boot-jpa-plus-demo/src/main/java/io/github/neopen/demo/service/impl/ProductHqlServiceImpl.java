package io.github.neopen.demo.service.impl;

import io.github.neopen.core.persistence.hql.HqlWrapper;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.core.persistence.sql.SqlQuery;
import io.github.neopen.core.persistence.sql.SqlWrapper;
import io.github.neopen.demo.bean.entity.Product;
import io.github.neopen.demo.bean.param.PageParam;
import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.demo.persistence.repo.ProductRepo;
import io.github.neopen.demo.service.support.AbstractProductService;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 HQL 的商品服务实现。
 *
 * <p>查询全部通过 {@link JpaPlusHqlExecutor} 与 {@link HqlWrapper} 完成，
 * 并演示原生 HQL 字段比较、批量更新和原生 SQL 统计。</p>
 */
@Service("productHqlService")
public class ProductHqlServiceImpl extends AbstractProductService {

    /**
     * 低库存查询。
     *
     * <p>库存数量与安全库存是同一张表的两个字段，HqlWrapper 的条件
     * 主要面向“字段与参数”比较，因此这里直接使用原生 HQL。</p>
     */
    private static final String LOW_STOCK_HQL =
            "select p from Product p "
                    + "where p.stockQuantity <= p.safetyStock "
                    + "order by p.stockQuantity asc, p.id asc";

    /** 库存状态统计，演示 SqlWrapper 与原生 SQL 执行器。 */
    private static final SqlQuery STOCK_SUMMARY_QUERY =
            SqlWrapper.create("t_product", "p")
                    .select("p.stock_status", "count(*) as total")
                    .groupBy("stock_status")
                    .orderByAsc("stock_status")
                    .build();

    /**
     * 批量补货。
     *
     * <p>单条 HQL 完成库存累加、库存状态重算和入库时间更新。</p>
     */
    private static final String RESTOCK_HQL =
            "update Product p "
                    + "set p.stockQuantity = p.stockQuantity + :quantity, "
                    + "p.stockStatus = case "
                    + "when p.stockQuantity + :quantity <= 0 then 'OUTOFSTOCK' "
                    + "when p.stockQuantity + :quantity <= p.safetyStock then 'LOWSTOCK' "
                    + "else 'INSTOCK' end, "
                    + "p.lastStockInTime = :now, "
                    + "p.updateTime = :now "
                    + "where p.id in :ids";

    /** HQL 执行器。 */
    private final JpaPlusHqlExecutor hqlExecutor;

    /** 原生 SQL 执行器。 */
    private final JpaPlusSqlExecutor sqlExecutor;

    /**
     * 创建 HQL 商品服务。
     *
     * @param productRepo       商品数据仓库
     * @param jsonPageConverter 分页响应转换器
     * @param hqlExecutor       HQL 执行器
     * @param sqlExecutor       原生 SQL 执行器
     */
    public ProductHqlServiceImpl(
            ProductRepo productRepo,
            JsonPageConverter jsonPageConverter,
            JpaPlusHqlExecutor hqlExecutor,
            JpaPlusSqlExecutor sqlExecutor
    ) {
        super(productRepo, jsonPageConverter);
        this.hqlExecutor = hqlExecutor;
        this.sqlExecutor = sqlExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public ProductVo getDetail(Long id) throws Exception {
        Product product = hqlExecutor.queryForObject(
                "select p from Product p where p.id = :id",
                Map.of("id", id),
                Product.class
        );

        if (product == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        return parseVo(product);
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<ProductVo>> getPage(ProductQueryParam param) throws Exception {
        PageRequest pageable = param == null
                ? PageRequest.of(0, PageParam.DEFAULT_ROW)
                : param.pageRequest();

        Page<Product> productPage = hqlExecutor.queryForPage(
                buildWrapper(param),
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
        HqlWrapper<Product> wrapper = buildWrapper(param).orderByAsc("id");

        return jsonPageConverter.convert(
                batchParseVo(hqlExecutor.queryForList(wrapper))
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductVo> getLowStockProducts() throws Exception {
        List<Product> products = hqlExecutor.queryForList(
                LOW_STOCK_HQL,
                null,
                Product.class
        );

        return batchParseVo(products);
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductStockSummaryVo> getStockSummary() {
        List<?> rows = sqlExecutor.queryForList(
                STOCK_SUMMARY_QUERY.getSql(),
                STOCK_SUMMARY_QUERY.getParameters()
        );

        return toStockSummary(rows);
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

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("quantity", quantity);
        parameters.put("now", LocalDateTime.now());
        parameters.put("ids", productIds);

        return hqlExecutor.executeUpdate(RESTOCK_HQL, parameters);
    }

    /**
     * 构建商品 HQL 查询条件。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return HQL 查询构造器
     */
    private HqlWrapper<Product> buildWrapper(ProductQueryParam param) {
        HqlWrapper<Product> wrapper = HqlWrapper.create(Product.class);

        if (param == null) {
            return wrapper;
        }

        if (param.getProductName() != null && !param.getProductName().isBlank()) {
            wrapper.like("productName", param.getProductName());
        }

        if (param.getProductType() != null && !param.getProductType().isBlank()) {
            wrapper.eq("productType", param.getProductType());
        }

        if (param.getProductBrand() != null && !param.getProductBrand().isBlank()) {
            wrapper.like("productBrand", param.getProductBrand());
        }

        if (param.getSkuCode() != null && !param.getSkuCode().isBlank()) {
            wrapper.eq("skuCode", param.getSkuCode());
        }

        if (param.getActive() != null) {
            wrapper.eq("isActive", param.getActive());
        }

        return wrapper;
    }
}
