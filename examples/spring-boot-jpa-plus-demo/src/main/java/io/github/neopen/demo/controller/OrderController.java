package io.github.neopen.demo.controller;

import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.response.Json;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单接口控制器。
 */
@RestController
@RequestMapping("/order")
public class OrderController implements BaseController<OrderQueryParam, OrderVo> {

    /** 订单业务服务。 */
    private final OrderService orderService;

    /**
     * 创建订单控制器。
     *
     * @param orderService 订单业务服务
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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
        return Json.getSucc(orderService.add(param));
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
        return Json.getSucc(orderService.generateOrder());
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        return orderService.getPage(param);
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {
        return orderService.getList(param);
    }

    /** {@inheritDoc} */
    @Override
    public Json<OrderVo> getDetail(Object orderNo) throws Exception {
        return Json.getSucc(orderService.getDetail(orderNo.toString()));
    }


}
