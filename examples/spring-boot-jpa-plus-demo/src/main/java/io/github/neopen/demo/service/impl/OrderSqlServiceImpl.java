package io.github.neopen.demo.service.impl;

import io.github.neopen.core.persistence.manager.JpaPlusSqlExecutor;
import io.github.neopen.core.persistence.sql.SqlWrapper;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.param.PageParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.OrderProductService;
import io.github.neopen.demo.service.support.AbstractOrderService;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于原生 SQL 的订单服务实现。
 *
 * <p>查询全部通过 {@link JpaPlusSqlExecutor} 与 {@link SqlWrapper} 完成，
 * 写入仍复用 JPA 仓库。</p>
 */
@Service("orderSqlService")
public class OrderSqlServiceImpl extends AbstractOrderService {

    /** 实体属性到数据库列的排序映射。 */
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("orderNo", "order_no"),
            Map.entry("totalAmount", "total_amount"),
            Map.entry("status", "status"),
            Map.entry("createTime", "create_time"),
            Map.entry("updateTime", "update_time"),
            Map.entry("userId", "user_id"),
            Map.entry("receiverName", "receiver_name"),
            Map.entry("paymentTime", "payment_time")
    );

    /** 原生 SQL 执行器。 */
    private final JpaPlusSqlExecutor sqlExecutor;

    /**
     * 创建 SQL 订单服务。
     *
     * @param orderRepo           订单数据仓库
     * @param orderProductService 订单项业务服务
     * @param jsonPageConverter   分页响应转换器
     * @param sqlExecutor         原生 SQL 执行器
     */
    public OrderSqlServiceImpl(
            OrderRepo orderRepo,
            OrderProductService orderProductService,
            JsonPageConverter jsonPageConverter,
            JpaPlusSqlExecutor sqlExecutor
    ) {
        super(orderRepo, orderProductService, jsonPageConverter);
        this.sqlExecutor = sqlExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        Page<Order> orderPage = sqlExecutor.queryForPage(
                buildWrapper(param),
                Order.class,
                pageable(param)
        );

        return jsonPageConverter.convert(
                parseVoList(orderPage.getContent()),
                orderPage.getTotalElements()
        );
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {
        SqlWrapper wrapper = buildWrapper(param).orderByDesc("id");

        return jsonPageConverter.convert(
                parseVoList(sqlExecutor.queryForList(wrapper, Order.class))
        );
    }

    /** {@inheritDoc} */
    @Override
    public OrderVo getDetail(String orderNo) throws Exception {
        Order order = sqlExecutor.queryForObject(
                "select o.* from t_order o where o.order_no = :orderNo",
                Map.of("orderNo", orderNo),
                Order.class
        );

        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        OrderVo vo = parseVo(order);
        vo.setOrderProducts(orderProductService.getOrderProductsByOrder(order));

        return vo;
    }

    /**
     * 构建订单 SQL 查询条件。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return SQL 查询构造器
     */
    private SqlWrapper buildWrapper(OrderQueryParam param) {
        SqlWrapper wrapper = SqlWrapper.create("t_order", "o");

        if (param == null) {
            return wrapper;
        }

        if (param.getOrderNo() != null && !param.getOrderNo().isBlank()) {
            wrapper.like("order_no", param.getOrderNo());
        }

        if (param.getStatus() != null && !param.getStatus().isBlank()) {
            wrapper.eq("status", param.getStatus());
        }

        return wrapper;
    }

    /**
     * 构建分页请求，并将实体属性排序转换为数据库列排序。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return 分页请求
     * @throws Exception 分页参数不合法时抛出
     */
    private PageRequest pageable(OrderQueryParam param) throws Exception {
        PageRequest pageable = param == null
                ? PageRequest.of(0, PageParam.DEFAULT_ROW)
                : param.pageRequest();

        return withSqlSort(pageable);
    }

    /**
     * 将 Spring Data 排序中的实体属性转换为数据库列。
     *
     * @param pageable 原分页请求
     * @return 转换排序后的分页请求
     */
    private PageRequest withSqlSort(PageRequest pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        List<Sort.Order> orders = new ArrayList<>();

        for (Sort.Order order : pageable.getSort()) {
            orders.add(new Sort.Order(
                    order.getDirection(),
                    SORT_COLUMNS.getOrDefault(
                            order.getProperty(),
                            order.getProperty()
                    )
            ));
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(orders)
        );
    }
}
