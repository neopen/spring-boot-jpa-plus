package io.github.neopen.demo.controller;

import io.github.neopen.demo.bean.param.ProductParam;
import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.param.ProductRestockParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.demo.service.ProductService;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品库存接口控制器。
 *
 * <p>默认接口走 JPA 实现，{@code /hql} 前缀走 HQL 实现，
 * {@code /sql} 前缀走原生 SQL 实现，便于直接对比三种方式。</p>
 */
@RestController
@RequestMapping("/product")
public class ProductController implements BaseController<ProductQueryParam, ProductVo> {

    /** 基于 JPA 的商品业务服务。 */
    private final ProductService productJpaService;

    /** 基于 HQL 的商品业务服务。 */
    private final ProductService productHqlService;

    /** 基于原生 SQL 的商品业务服务。 */
    private final ProductService productSqlService;

    /**
     * 创建商品控制器。
     *
     * @param productJpaService 基于 JPA 的商品业务服务
     * @param productHqlService 基于 HQL 的商品业务服务
     * @param productSqlService 基于原生 SQL 的商品业务服务
     */
    public ProductController(
            @Qualifier("productJpaService") ProductService productJpaService,
            @Qualifier("productHqlService") ProductService productHqlService,
            @Qualifier("productSqlService") ProductService productSqlService
    ) {
        this.productJpaService = productJpaService;
        this.productHqlService = productHqlService;
        this.productSqlService = productSqlService;
    }

    /**
     * 新增商品。
     *
     * @param param 商品参数
     * @return 新增后的商品
     * @throws Exception 保存或转换失败时抛出
     */
    @PostMapping
    @ApiOperation("商品新增")
    @SysControllerLog("商品新增")
    public Json<ProductVo> addProduct(@RequestBody ProductParam param) throws Exception {
        return Json.getSucc(productJpaService.add(param));
    }

    /**
     * 编辑商品。
     *
     * @param id    商品主键
     * @param param 商品参数
     * @return 编辑后的商品
     * @throws Exception 商品不存在、保存或转换失败时抛出
     */
    @PutMapping("/{id}")
    @ApiOperation("商品编辑")
    @SysControllerLog("商品编辑")
    public Json<ProductVo> updateProduct(
            @PathVariable("id") Long id,
            @RequestBody ProductParam param
    ) throws Exception {
        return Json.getSucc(productJpaService.update(id, param));
    }

    /**
     * 删除商品。
     *
     * @param id 商品主键
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    @ApiOperation("商品删除")
    @SysControllerLog("商品删除")
    public Json<Boolean> deleteProduct(@PathVariable("id") Long id) {
        productJpaService.delete(id);

        return Json.getSucc(Boolean.TRUE);
    }

    /**
     * 查询低库存商品。
     *
     * @return 低库存商品列表
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/low-stock")
    @ApiOperation("低库存商品")
    @SysControllerLog("低库存商品")
    public Json<List<ProductVo>> getLowStockProducts() throws Exception {
        return Json.getSuccs(productJpaService.getLowStockProducts());
    }

    /**
     * 统计各库存状态的商品数量。
     *
     * @return 库存状态统计
     */
    @GetMapping("/stock-summary")
    @ApiOperation("库存状态统计")
    @SysControllerLog("库存状态统计")
    public Json<List<ProductStockSummaryVo>> getStockSummary() {
        return Json.getSuccs(productJpaService.getStockSummary());
    }

    /**
     * 批量补货。
     *
     * @param param 补货参数
     * @return 受影响的商品数量
     */
    @PostMapping("/restock")
    @ApiOperation("商品批量补货")
    @SysControllerLog("商品批量补货")
    public Json<Integer> restock(@RequestBody ProductRestockParam param) {
        return Json.getSucc(
                productJpaService.restock(param.getIds(), param.getQuantity())
        );
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<ProductVo>> getPage(ProductQueryParam param) throws Exception {
        return productJpaService.getPage(param);
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<ProductVo>> getList(ProductQueryParam param) throws Exception {
        return productJpaService.getList(param);
    }

    /** {@inheritDoc} */
    @Override
    public Json<ProductVo> getDetail(Object id) throws Exception {
        return Json.getSucc(productJpaService.getDetail(Long.valueOf(id.toString())));
    }

    /**
     * 使用 HQL 分页查询商品。
     *
     * @param param 商品查询参数
     * @return 商品分页响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/hql/page")
    @ApiOperation("HQL 分页列表")
    @SysControllerLog("HQL 分页列表")
    public Json<List<ProductVo>> getHqlPage(ProductQueryParam param) throws Exception {
        return productHqlService.getPage(param);
    }

    /**
     * 使用 HQL 查询全部商品。
     *
     * @param param 商品查询参数
     * @return 商品列表响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/hql/list")
    @ApiOperation("HQL 所有列表")
    @SysControllerLog("HQL 所有列表")
    public Json<List<ProductVo>> getHqlList(ProductQueryParam param) throws Exception {
        return productHqlService.getList(param);
    }

    /**
     * 使用 HQL 查询低库存商品。
     *
     * @return 低库存商品列表
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/hql/low-stock")
    @ApiOperation("HQL 低库存商品")
    @SysControllerLog("HQL 低库存商品")
    public Json<List<ProductVo>> getHqlLowStockProducts() throws Exception {
        return Json.getSuccs(productHqlService.getLowStockProducts());
    }

    /**
     * 使用 HQL 批量补货。
     *
     * @param param 补货参数
     * @return 受影响的商品数量
     */
    @PostMapping("/hql/restock")
    @ApiOperation("HQL 商品批量补货")
    @SysControllerLog("HQL 商品批量补货")
    public Json<Integer> restockByHql(@RequestBody ProductRestockParam param) {
        return Json.getSucc(
                productHqlService.restock(param.getIds(), param.getQuantity())
        );
    }

    /**
     * 使用原生 SQL 统计库存状态。
     *
     * @return 库存状态统计
     */
    @GetMapping("/hql/stock-summary")
    @ApiOperation("HQL 库存状态统计")
    @SysControllerLog("HQL 库存状态统计")
    public Json<List<ProductStockSummaryVo>> getHqlStockSummary() {
        return Json.getSuccs(productHqlService.getStockSummary());
    }

    /**
     * 使用 HQL 查询商品详情。
     *
     * @param id 商品主键
     * @return 商品详情
     * @throws Exception 商品不存在或转换失败时抛出
     */
    @GetMapping("/hql/{id}")
    @ApiOperation("HQL 详情")
    @SysControllerLog("HQL 详情")
    public Json<ProductVo> getHqlDetail(@PathVariable("id") Long id) throws Exception {
        return Json.getSucc(productHqlService.getDetail(id));
    }

    /**
     * 使用原生 SQL 分页查询商品。
     *
     * @param param 商品查询参数
     * @return 商品分页响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/sql/page")
    @ApiOperation("SQL 分页列表")
    @SysControllerLog("SQL 分页列表")
    public Json<List<ProductVo>> getSqlPage(ProductQueryParam param) throws Exception {
        return productSqlService.getPage(param);
    }

    /**
     * 使用原生 SQL 查询全部商品。
     *
     * @param param 商品查询参数
     * @return 商品列表响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/sql/list")
    @ApiOperation("SQL 所有列表")
    @SysControllerLog("SQL 所有列表")
    public Json<List<ProductVo>> getSqlList(ProductQueryParam param) throws Exception {
        return productSqlService.getList(param);
    }

    /**
     * 使用原生 SQL 查询低库存商品。
     *
     * @return 低库存商品列表
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/sql/low-stock")
    @ApiOperation("SQL 低库存商品")
    @SysControllerLog("SQL 低库存商品")
    public Json<List<ProductVo>> getSqlLowStockProducts() throws Exception {
        return Json.getSuccs(productSqlService.getLowStockProducts());
    }

    /**
     * 使用原生 SQL 批量补货。
     *
     * @param param 补货参数
     * @return 受影响的商品数量
     */
    @PostMapping("/sql/restock")
    @ApiOperation("SQL 商品批量补货")
    @SysControllerLog("SQL 商品批量补货")
    public Json<Integer> restockBySql(@RequestBody ProductRestockParam param) {
        return Json.getSucc(
                productSqlService.restock(param.getIds(), param.getQuantity())
        );
    }

    /**
     * 使用原生 SQL 统计库存状态。
     *
     * @return 库存状态统计
     */
    @GetMapping("/sql/stock-summary")
    @ApiOperation("SQL 库存状态统计")
    @SysControllerLog("SQL 库存状态统计")
    public Json<List<ProductStockSummaryVo>> getSqlStockSummary() {
        return Json.getSuccs(productSqlService.getStockSummary());
    }

    /**
     * 使用原生 SQL 查询商品详情。
     *
     * @param id 商品主键
     * @return 商品详情
     * @throws Exception 商品不存在或转换失败时抛出
     */
    @GetMapping("/sql/{id}")
    @ApiOperation("SQL 详情")
    @SysControllerLog("SQL 详情")
    public Json<ProductVo> getSqlDetail(@PathVariable("id") Long id) throws Exception {
        return Json.getSucc(productSqlService.getDetail(id));
    }
}
