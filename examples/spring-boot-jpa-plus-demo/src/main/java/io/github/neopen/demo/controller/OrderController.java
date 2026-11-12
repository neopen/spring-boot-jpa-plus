package io.github.neopen.demo.controller;

import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口控制器。
 *
 * <p>默认接口走 JPA 实现，{@code /hql} 前缀走 HQL 实现，
 * {@code /sql} 前缀走原生 SQL 实现。</p>
 */
@RestController
@RequestMapping("/order")
public class OrderController implements BaseController<OrderQueryParam, OrderVo> {

    /** 基于 JPA 的订单业务服务。 */
    private final OrderService orderJpaService;

    /** 基于 HQL 的订单业务服务。 */
    private final OrderService orderHqlService;

    /** 基于原生 SQL 的订单业务服务。 */
    private final OrderService orderSqlService;

    /**
     * 创建订单控制器。
     *
     * @param orderJpaService 基于 JPA 的订单业务服务
     * @param orderHqlService 基于 HQL 的订单业务服务
     * @param orderSqlService 基于原生 SQL 的订单业务服务
     */
    public OrderController(
            @Qualifier("orderJpaService") OrderService orderJpaService,
            @Qualifier("orderHqlService") OrderService orderHqlService,
            @Qualifier("orderSqlService") OrderService orderSqlService
    ) {
        this.orderJpaService = orderJpaService;
        this.orderHqlService = orderHqlService;
        this.orderSqlService = orderSqlService;
    }

    /**
     * 新增订单。
     *
     * @param param 订单新增参数
     * @return 新增订单详情
     * @throws Exception 订单保存或转换失败时抛出
     */
    @PostMapping
    @ApiOperation("订单新增")
    @SysControllerLog("订单新增")
    public Json<OrderVo> addOrder(@RequestBody OrderParam param) throws Exception {
        return Json.getSucc(orderJpaService.add(param));
    }

    /**
     * 随机生成并保存示例订单。
     *
     * @return 生成的订单详情
     * @throws Exception 订单保存或转换失败时抛出
     */
    @GetMapping
    @ApiOperation("随机生成订单")
    @SysControllerLog("随机生成订单")
    public Json<OrderVo> generateOrder() throws Exception {
        return Json.getSucc(orderJpaService.generateOrder());
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        return orderJpaService.getPage(param);
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {
        return orderJpaService.getList(param);
    }

    /** {@inheritDoc} */
    @Override
    public Json<OrderVo> getDetail(Object orderNo) throws Exception {
        return Json.getSucc(orderJpaService.getDetail(orderNo.toString()));
    }

    /**
     * 使用 HQL 分页获取订单列表。
     *
     * @param param 订单查询参数
     * @return 订单分页响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/hql/page")
    @ApiOperation("HQL 分页列表")
    @SysControllerLog("HQL 分页列表")
    public Json<List<OrderVo>> getHqlPage(OrderQueryParam param) throws Exception {
        return orderHqlService.getPage(param);
    }

    /**
     * 使用 HQL 获取全部订单。
     *
     * @param param 订单查询参数
     * @return 订单列表响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/hql/list")
    @ApiOperation("HQL 所有列表")
    @SysControllerLog("HQL 所有列表")
    public Json<List<OrderVo>> getHqlList(OrderQueryParam param) throws Exception {
        return orderHqlService.getList(param);
    }

    /**
     * 使用 HQL 获取订单详情。
     *
     * @param orderNo 订单编号
     * @return 订单详情
     * @throws Exception 订单不存在或转换失败时抛出
     */
    @GetMapping("/hql/{orderNo}")
    @ApiOperation("HQL 详情")
    @SysControllerLog("HQL 详情")
    public Json<OrderVo> getHqlDetail(@PathVariable("orderNo") String orderNo) throws Exception {
        return Json.getSucc(orderHqlService.getDetail(orderNo));
    }

    /**
     * 使用原生 SQL 分页获取订单列表。
     *
     * @param param 订单查询参数
     * @return 订单分页响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/sql/page")
    @ApiOperation("SQL 分页列表")
    @SysControllerLog("SQL 分页列表")
    public Json<List<OrderVo>> getSqlPage(OrderQueryParam param) throws Exception {
        return orderSqlService.getPage(param);
    }

    /**
     * 使用原生 SQL 获取全部订单。
     *
     * @param param 订单查询参数
     * @return 订单列表响应
     * @throws Exception 查询或转换失败时抛出
     */
    @GetMapping("/sql/list")
    @ApiOperation("SQL 所有列表")
    @SysControllerLog("SQL 所有列表")
    public Json<List<OrderVo>> getSqlList(OrderQueryParam param) throws Exception {
        return orderSqlService.getList(param);
    }

    /**
     * 使用原生 SQL 获取订单详情。
     *
     * @param orderNo 订单编号
     * @return 订单详情
     * @throws Exception 订单不存在或转换失败时抛出
     */
    @GetMapping("/sql/{orderNo}")
    @ApiOperation("SQL 详情")
    @SysControllerLog("SQL 详情")
    public Json<OrderVo> getSqlDetail(@PathVariable("orderNo") String orderNo) throws Exception {
        return Json.getSucc(orderSqlService.getDetail(orderNo));
    }
}
