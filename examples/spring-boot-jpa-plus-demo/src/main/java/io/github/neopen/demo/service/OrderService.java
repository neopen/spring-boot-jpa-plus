package io.github.neopen.demo.service;


import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.extension.response.Json;

import java.util.List;

/**
 * 订单业务服务。
 */
public interface OrderService {

    /**
     * 新增订单。
     *
     * @param param 订单新增参数
     * @return 订单详情
     * @throws Exception 保存或转换失败时抛出
     */
    OrderVo add(OrderParam param) throws Exception;

    /**
     * 随机生成并保存示例订单。
     *
     * @return 订单详情
     * @throws Exception 保存或转换失败时抛出
     */
    OrderVo generateOrder() throws Exception;

    /**
     * 获取订单详情。
     *
     * @param orderNo 订单编号
     * @return 订单详情
     * @throws Exception 订单不存在或转换失败时抛出
     */
    OrderVo getDetail(String orderNo) throws Exception;

    /**
     * 分页获取订单列表。
     *
     * @param param 订单查询参数
     * @return 订单分页响应
     * @throws Exception 查询或转换失败时抛出
     */
    Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception;

    /**
     * 获取符合条件的全部订单。
     *
     * @param param 订单查询参数
     * @return 订单列表响应
     * @throws Exception 查询或转换失败时抛出
     */
    Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception;
}
