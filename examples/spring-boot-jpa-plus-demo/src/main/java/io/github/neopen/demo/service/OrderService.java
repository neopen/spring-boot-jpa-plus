package io.github.neopen.demo.service;


import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.extension.response.Json;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 16:55
 */
public interface OrderService {

    /**
     * 新增订单
     *
     * @param param
     * @return
     * @throws Exception
     */
    OrderVo add(OrderParam param) throws Exception;

    /**
     * 随机生成订单
     *
     * @return
     * @throws Exception
     */
    OrderVo generateOrder() throws Exception;

    /**
     * 获取订单详情
     *
     * @param orderNo
     * @return
     * @throws Exception
     */
    OrderVo getDetail(String orderNo) throws Exception;

    /**
     * 获取订单列表
     *
     * @param param
     * @return
     * @throws Exception
     */
    Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception;

    /**
     * 获取订单列表
     *
     * @param param
     * @return
     * @throws Exception
     */
    Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception;
}
