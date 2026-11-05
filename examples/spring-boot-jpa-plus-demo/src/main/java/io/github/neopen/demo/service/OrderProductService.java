package io.github.neopen.demo.service;

import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.vo.OrderProductVo;

import java.util.List;

/**
 * 订单项业务服务。
 */
public interface OrderProductService {

    /**
     * 保存订单项列表。
     *
     * @param orderProducts 订单项参数列表
     * @param order         所属订单
     * @return 保存后的订单项实体列表
     * @throws Exception 保存失败时抛出
     */
    List<OrderProduct> saveOrderProduct(List<OrderProductParam> orderProducts, Order order) throws Exception;

    /**
     * 根据订单获取订单项视图列表。
     *
     * @param order 订单实体
     * @return 订单项视图列表
     * @throws Exception 查询或转换失败时抛出
     */
    List<OrderProductVo> getOrderProductsByOrder(Order order) throws Exception;
}
