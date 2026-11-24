/*
 * Copyright 2026 neopen
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

import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.core.persistence.page.PageParams;
import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import io.github.neopen.core.persistence.spec.SpecWrapper;
import io.github.neopen.core.persistence.sql.SqlWrapper;
import io.github.neopen.core.scope.JpaPlusScope;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.vo.OrderSummaryVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 0.5 新增查询能力的示例服务。
 *
 * <p>集中演示请求分页参数、union 合并查询和数据源作用域隔离，
 * 与业务无关，便于对照文档直接运行。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Service
public class QueryFeatureService {

    /** 归档查询作用域名称，配置见 {@code neopen.jpa.scopes.archive-view}。 */
    public static final String ARCHIVE_SCOPE = "archive-view";

    /** 已支付状态。 */
    private static final String STATUS_PAID = "PAID";

    /** 已取消状态。 */
    private static final String STATUS_CANCELLED = "CANCELLED";

    /** 订单数据仓库。 */
    private final OrderRepo orderRepo;

    /** 实体管理器，用于构造 Specification 执行器。 */
    private final EntityManager entityManager;

    /** 原生 SQL 执行器。 */
    private final JpaPlusSqlExecutor sqlExecutor;

    /**
     * 创建查询能力示例服务。
     *
     * @param orderRepo   订单数据仓库
     * @param entityManager 实体管理器
     * @param sqlExecutor 原生 SQL 执行器
     */
    public QueryFeatureService(
            OrderRepo orderRepo,
            EntityManager entityManager,
            JpaPlusSqlExecutor sqlExecutor
    ) {
        this.orderRepo = orderRepo;
        this.entityManager = entityManager;
        this.sqlExecutor = sqlExecutor;
    }

    /**
     * 使用 {@link PageParams} 校验分页与排序参数后查询订单。
     *
     * <p>页码从 1 开始，每页上限 500，排序字段必须是实体属性。</p>
     *
     * @param page    页码，从 1 开始
     * @param size    每页数量
     * @param sort    排序表达式，例如 {@code id,desc}
     * @param orderNo 订单编号关键字，允许为空
     * @return 订单摘要分页
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryVo> getPage(
            int page,
            int size,
            String sort,
            String orderNo
    ) {
        Pageable pageable = PageParams.builder()
                .page(page)
                .size(size)
                .sort(sort)
                .entityType(Order.class)
                .build()
                .toPageable();

        SpecWrapper<Order> wrapper = SpecWrapper.create(Order.class);

        if (StringUtils.hasText(orderNo)) {
            wrapper.like("orderNo", orderNo);
        }

        return orderRepo.findAll(
                wrapper.filterDeleted().toSpecification(),
                pageable
        ).map(this::toSummary);
    }

    /**
     * 使用 union 合并已支付与已取消订单。
     *
     * <p>两段查询结构一致，参数由构建器统一重编号，排序作用于整个
     * union 结果。</p>
     *
     * @return 合并后的订单摘要列表
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryVo> getPaidAndCancelled() {
        SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
                .select(
                        "o.id",
                        "o.order_no as orderNo",
                        "o.status",
                        "o.total_amount as totalAmount"
                )
                .eq("status", STATUS_PAID)
                .union(
                        SqlWrapper.create("t_order", "a")
                                .select(
                                        "a.id",
                                        "a.order_no as orderNo",
                                        "a.status",
                                        "a.total_amount as totalAmount"
                                )
                                .eq("status", STATUS_CANCELLED)
                )
                .orderByAsc("id");

        return sqlExecutor.queryForDtoList(
                wrapper,
                OrderSummaryVo.class
        );
    }

    /**
     * 在归档作用域内查询订单，包含已逻辑删除的数据。
     *
     * <p>{@code archive-view} 作用域在配置中关闭了逻辑删除过滤，
     * 绑定后只影响当前线程，其他查询仍然过滤已删除数据。</p>
     *
     * @param orderNo 订单编号关键字，允许为空
     * @return 订单摘要列表
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryVo> listIncludingDeleted(String orderNo) {
        SpecWrapper<Order> wrapper = SpecWrapper.create(Order.class);

        if (StringUtils.hasText(orderNo)) {
            wrapper.like("orderNo", orderNo);
        }

        Specification<Order> specification = wrapper.toSpecification();
        JpaPlusSpecExecutor<Order> executor =
                JpaPlusSpecExecutor.of(entityManager, Order.class);

        List<Order> orders = JpaPlusScope.call(
                ARCHIVE_SCOPE,
                () -> executor.findAll(specification)
        );

        return orders.stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 把订单转换为摘要视图。
     *
     * @param order 订单实体
     * @return 订单摘要
     */
    private OrderSummaryVo toSummary(Order order) {
        return new OrderSummaryVo(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getTotalAmount()
        );
    }
}
