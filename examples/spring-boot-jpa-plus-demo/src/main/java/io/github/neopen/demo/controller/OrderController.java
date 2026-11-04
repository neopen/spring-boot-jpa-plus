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
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 16:46
 */
@RestController
@RequestMapping("/order")
public class OrderController implements BaseController<OrderQueryParam, OrderVo> {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ApiOperation("订单新增")
    @SysControllerLog("订单新增")
    public Json<OrderVo> addOrder(@RequestBody OrderParam param) throws Exception {
        return Json.getSucc(orderService.add(param));
    }

    @GetMapping
    @ApiOperation("随机生成订单")
    @SysControllerLog("随机生成订单")
    public Json<OrderVo> generateOrder() throws Exception {
        return Json.getSucc(orderService.generateOrder());
    }

    @Override
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        return orderService.getPage(param);
    }

    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {
        return orderService.getList(param);
    }

    @Override
    public Json<OrderVo> getDetail(Object orderNo) throws Exception {
        return Json.getSucc(orderService.getDetail(orderNo.toString()));
    }


}
