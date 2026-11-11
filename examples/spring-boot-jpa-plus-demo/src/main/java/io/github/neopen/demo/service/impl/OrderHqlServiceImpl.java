package io.github.neopen.demo.service.impl;

import io.github.neopen.core.persistence.hql.HqlWrapper;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 基于 HQL 的订单服务实现。
 *
 * <p>查询全部通过 {@link JpaPlusHqlExecutor} 与 {@link HqlWrapper} 完成，
 * 写入仍复用 JPA 仓库。</p>
 */
@Service("orderHqlService")
public class OrderHqlServiceImpl extends AbstractOrderService {

    /** HQL 执行器。 */
    private final JpaPlusHqlExecutor hqlExecutor;

    /**
     * 创建 HQL 订单服务。
     *
     * @param orderRepo           订单数据仓库
     * @param orderProductService 订单项业务服务
     * @param jsonPageConverter   分页响应转换器
     * @param hqlExecutor         HQL 执行器
     */
    public OrderHqlServiceImpl(
            OrderRepo orderRepo,
            OrderProductService orderProductService,
            JsonPageConverter jsonPageConverter,
            JpaPlusHqlExecutor hqlExecutor
    ) {
        super(orderRepo, orderProductService, jsonPageConverter);
        this.hqlExecutor = hqlExecutor;
    }

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        Page<Order> orderPage = hqlExecutor.queryForPage(
                buildWrapper(param),
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
        HqlWrapper<Order> wrapper = buildWrapper(param).orderByDesc("id");

        return jsonPageConverter.convert(
                parseVoList(hqlExecutor.queryForList(wrapper))
        );
    }

    /** {@inheritDoc} */
    @Override
    public OrderVo getDetail(String orderNo) throws Exception {
        Order order = hqlExecutor.queryForObject(
                "select o from Order o where o.orderNo = :orderNo",
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
     * 构建订单 HQL 查询条件。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return HQL 查询构造器
     */
    private HqlWrapper<Order> buildWrapper(OrderQueryParam param) {
        HqlWrapper<Order> wrapper = HqlWrapper.create(Order.class);

        if (param == null) {
            return wrapper;
        }

        if (param.getOrderNo() != null && !param.getOrderNo().isBlank()) {
            wrapper.like("orderNo", param.getOrderNo());
        }

        if (param.getStatus() != null && !param.getStatus().isBlank()) {
            wrapper.eq("status", param.getStatus());
        }

        return wrapper;
    }

    /**
     * 构建分页请求。
     *
     * @param param 查询参数，允许为 {@code null}
     * @return 分页请求
     * @throws Exception 分页参数不合法时抛出
     */
    private PageRequest pageable(OrderQueryParam param) throws Exception {
        return param == null
                ? PageRequest.of(0, PageParam.DEFAULT_ROW)
                : param.pageRequest();
    }
}
