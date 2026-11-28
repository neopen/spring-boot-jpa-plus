/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.demo.service;


import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.extension.response.Json;

import java.util.List;

/**
 * 订单业务服务。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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
