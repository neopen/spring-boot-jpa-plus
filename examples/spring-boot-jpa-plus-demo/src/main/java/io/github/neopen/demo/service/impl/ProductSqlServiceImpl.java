package io.github.neopen.demo.service.impl;

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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于原生 SQL 的商品服务实现。
 *
 * <p>查询全部通过 {@link JpaPlusSqlExecutor} 与 {@link SqlWrapper} 完成，
 * 并演示原生 SQL 字段比较、批量更新和聚合统计。</p>
 */
@Service("productSqlService")
public class ProductSqlServiceImpl extends AbstractProductService {

    /** 原生低库存查询。 */
    private static final String LOW_STOCK_SQL =
            "select p.* from t_product p "
                    + "where p.stock_quantity <= p.safety_stock "
                    + "order by p.stock_quantity asc, p.id asc";

    /** 原生批量补货。 */
    private static final String RESTOCK_SQL =
            "update t_product "
                    + "set stock_quantity = stock_quantity + :quantity, "
                    + "stock_status = case "
                    + "when stock_quantity + :quantity <= 0 then 'OUTOFSTOCK' "
                    + "when stock_quantity + :quantity <= safety_stock then 'LOWSTOCK' "
                    + "else 'INSTOCK' end, "
                    + "last_stock_in_time = :now, "
                    + "update_time = :now "
                    + "where id in (:ids)";

    /** 库存状态统计。 */
    private static final SqlQuery STOCK_SUMMARY_QUERY =
            SqlWrapper.create("t_product", "p")
                    .select("p.stock_status", "count(*) as total")
                    .groupBy("stock_status")
                    .orderByAsc("stock_status")
                    .build();

    /** 实体属性到数据库列的排序映射。 */
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("productName", "product_name"),
            Map.entry("unitPrice", "unit_price"),
            Map.entry("stockQuantity", "stock_quantity"),
            Map.entry("safetyStock", "safety_stock"),
            Map.entry("productCode", "product_code"),
            Map.entry("skuCode", "sku_code"),
            Map.entry("productType", "product_type"),
            Map.entry("productBrand", "product_brand"),
            Map.entry("stockStatus", "stock_status"),
            Map.entry("createTime", "create_time"),
            Map.entry("updateTime", "update_time"),
            Map.entry("lastStockInTime", "last_stock_in_time")
    );

    /** 原生 SQL 执行器。 */
    private final JpaPlusSqlExecutor sqlExecutor;

    /**
     * 创建 SQL 商品服务。
     *
     * @param productRepo       商品数据仓库
     * @param jsonPageConverter 分页响应转换器
     * @param sqlExecutor       原生 SQL 执行器
     */
    public ProductSqlServiceImpl(
            ProductRepo productRepo,
            JsonPageConverter jsonPageConverter,
            JpaPlusSqlExecutor sqlExecutor
    ) {
        super(productRepo, jsonPageConverter);
        this.sqlExecutor = sqlExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public ProductVo getDetail(Long id) throws Exception {
        Product product = sqlExecutor.queryForObject(
                "select p.* from t_product p where p.id = :id",
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
        Page<Product> productPage = sqlExecutor.queryForPage(
                buildWrapper(param),
                Product.class,
                pageable(param)
        );

        return jsonPageConverter.convert(
                batchParseVo(productPage.getContent()),
                productPage.getTotalElements()
        );
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<ProductVo>> getList(ProductQueryParam param) throws Exception {
        SqlWrapper wrapper = buildWrapper(param).orderByAsc("id");

        return jsonPageConverter.convert(
                batchParseVo(sqlExecutor.queryForList(wrapper, Product.class))
        );
    }

    /** {@inheritDoc} */
    @Override
    public List<ProductVo> getLowStockProducts() throws Exception {
        List<Product> products = sqlExecutor.queryForEntities(
                LOW_STOCK_SQL,
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

        return sqlExecutor.executeUpdate(RESTOCK_SQL, parameters);
    }

    /**
     * 构建商品 SQL 查询条件。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return SQL 查询构造器
     */
    private SqlWrapper buildWrapper(ProductQueryParam param) {
        SqlWrapper wrapper = SqlWrapper.create("t_product", "p");

        if (param == null) {
            return wrapper;
        }

        if (param.getProductName() != null && !param.getProductName().isBlank()) {
            wrapper.like("product_name", param.getProductName());
        }

        if (param.getProductType() != null && !param.getProductType().isBlank()) {
            wrapper.eq("product_type", param.getProductType());
        }

        if (param.getProductBrand() != null && !param.getProductBrand().isBlank()) {
            wrapper.like("product_brand", param.getProductBrand());
        }

        if (param.getSkuCode() != null && !param.getSkuCode().isBlank()) {
            wrapper.eq("sku_code", param.getSkuCode());
        }

        if (param.getActive() != null) {
            wrapper.eq("is_active", param.getActive());
        }

        return wrapper;
    }

    /**
     * 构建分页请求，并将实体属性排序转换为数据库列排序。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return 分页请求
     * @throws Exception 分页参数不合法时抛出
     */
    private PageRequest pageable(ProductQueryParam param) throws Exception {
        PageRequest pageable = param == null
                ? PageRequest.of(0, PageParam.DEFAULT_ROW)
                : param.pageRequest();

        return withSqlSort(pageable);
    }

    /**
     * 将 Spring Data 排序中的实体属性转换为数据库列。
     *
     * @param pageable 原分页请求
     * @return 转换排序后的分页请求
     */
    private PageRequest withSqlSort(PageRequest pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            orders.add(new Sort.Order(
                    order.getDirection(),
                    SORT_COLUMNS.getOrDefault(
                            order.getProperty(),
                            order.getProperty()
                    )
            ));
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(orders)
        );
    }
}
