package io.github.neopen.demo.service;

import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.vo.OrderProductVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    /**
     * 根据多个订单批量获取订单项视图，按订单主键分组返回。
     *
     * @param orders 订单集合
     * @return 订单主键到订单项视图列表的映射；无数据时返回空映射
     * @throws Exception 查询或转换失败时抛出
     */
    Map<Long, List<OrderProductVo>> getOrderProductsByOrders(Collection<Order> orders) throws Exception;
}
