package io.github.neopen.demo.service;

import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import io.github.neopen.demo.bean.vo.OrderProductVo;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @email helpenx@gmail.com
 * @date 2026/11/4 18:50
 */
public interface OrderProductService {

    /**
     * 根据订单获取订单产品列表
     *
     * @param order
     * @return
     */
    List<OrderProductVo> getOrderProductsByOrder(Order order) throws Exception;
}
