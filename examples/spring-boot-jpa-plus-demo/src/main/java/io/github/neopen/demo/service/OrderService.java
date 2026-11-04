package io.github.neopen.demo.service;


import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.vo.OrderDetailVo;
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
     * 获取订单详情
     *
     * @param orderNo
     * @return
     * @throws Exception
     */
    OrderDetailVo getDetail(String orderNo) throws Exception;

    /**
     * 获取订单列表
     *
     * @param param
     * @return
     * @throws Exception
     */
    Json<List<OrderVo>> getPage(OrderParam param) throws Exception;


}
