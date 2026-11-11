package io.github.neopen.demo.service;

import io.github.neopen.demo.bean.param.ProductParam;
import io.github.neopen.demo.bean.param.ProductQueryParam;
import io.github.neopen.demo.bean.vo.ProductStockSummaryVo;
import io.github.neopen.demo.bean.vo.ProductVo;
import io.github.neopen.extension.response.Json;

import java.util.Collection;
import java.util.List;

/**
 * 商品库存业务服务。
 */
public interface ProductService {

    /**
     * 新增商品。
     *
     * @param param 商品参数
     * @return 新增后的商品视图
     * @throws Exception 保存或转换失败时抛出
     */
    ProductVo add(ProductParam param) throws Exception;

    /**
     * 编辑商品资料。
     *
     * @param id    商品主键
     * @param param 商品参数
     * @return 编辑后的商品视图
     * @throws Exception 商品不存在、保存或转换失败时抛出
     */
    ProductVo update(Long id, ProductParam param) throws Exception;

    /**
     * 删除商品。
     *
     * @param id 商品主键
     * @throws IllegalArgumentException 商品不存在
     */
    void delete(Long id);

    /**
     * 获取商品详情。
     *
     * @param id 商品主键
     * @return 商品视图
     * @throws Exception 商品不存在或转换失败时抛出
     */
    ProductVo getDetail(Long id) throws Exception;

    /**
     * 使用 HQL 链式构造器分页查询商品。
     *
     * @param param 商品查询参数
     * @return 商品分页响应
     * @throws Exception 查询或转换失败时抛出
     */
    Json<List<ProductVo>> getPage(ProductQueryParam param) throws Exception;

    /**
     * 查询符合条件的全部商品。
     *
     * @param param 商品查询参数
     * @return 商品列表响应
     * @throws Exception 查询或转换失败时抛出
     */
    Json<List<ProductVo>> getList(ProductQueryParam param) throws Exception;

    /**
     * 查询库存低于安全库存的商品。
     *
     * <p>该查询演示原生 HQL 的字段间比较能力。</p>
     *
     * @return 低库存商品列表
     * @throws Exception 查询或转换失败时抛出
     */
    List<ProductVo> getLowStockProducts() throws Exception;

    /**
     * 使用原生 SQL 统计各库存状态的商品数量。
     *
     * @return 库存状态统计列表
     */
    List<ProductStockSummaryVo> getStockSummary();

    /**
     * 批量补货，并同步刷新库存状态。
     *
     * <p>该操作使用 HQL 批量更新，不会逐条加载实体。</p>
     *
     * @param ids      商品主键集合
     * @param quantity 每个商品增加的库存数量，必须大于 0
     * @return 受影响的商品数量
     * @throws IllegalArgumentException 参数不合法
     */
    int restock(Collection<Long> ids, int quantity);
}
