package io.github.neopen.demo.controller;

import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.vo.OrderDetailVo;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.extension.response.Json;
import org.springframework.web.bind.annotation.RestController;

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
public class OrderController implements BaseController<OrderParam, OrderVo> {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public Json<List<OrderVo>> getPage(OrderParam param) throws Exception {
        return orderService.getPage(param);
    }

    @Override
    public Json<OrderDetailVo> getDetail(Object orderNo) throws Exception {
        return Json.getSucc(orderService.getDetail(orderNo.toString()));
    }


}
