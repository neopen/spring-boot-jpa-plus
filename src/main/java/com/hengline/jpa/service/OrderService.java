package com.hengline.jpa.service;

import com.hengline.jpa.bean.Json;
import com.hengline.jpa.bean.param.OrderParam;
import com.hengline.jpa.bean.vo.OrderDetailVo;
import com.hengline.jpa.bean.vo.OrderVo;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author HengLine
 * @email haeng2030@gmail.com
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
