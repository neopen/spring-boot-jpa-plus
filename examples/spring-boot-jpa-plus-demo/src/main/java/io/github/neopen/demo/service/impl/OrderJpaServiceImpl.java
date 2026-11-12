package io.github.neopen.demo.service.impl;

import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.OrderProductService;
import io.github.neopen.demo.service.support.AbstractOrderService;
import io.github.neopen.core.persistence.spec.SpecWrapper;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import jakarta.persistence.criteria.*;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基于 Spring Data JPA 的订单服务实现。
 *
 * <p>使用 {@code Specification} 动态拼接查询条件，通过仓库完成分页和列表查询。
 * 除基础字段过滤外，还演示订单项连表查询、去重和金额区间过滤。</p>
 */
@Service("orderJpaService")
@Primary
public class OrderJpaServiceImpl extends AbstractOrderService {

    /**
     * 创建 JPA 订单服务。
     *
     * @param orderRepo           订单数据仓库
     * @param orderProductService 订单项业务服务
     * @param jsonPageConverter   分页响应转换器
     */
    public OrderJpaServiceImpl(
            OrderRepo orderRepo,
            OrderProductService orderProductService,
            JsonPageConverter jsonPageConverter
    ) {
        super(orderRepo, orderProductService, jsonPageConverter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Predicate> buildPredicates(
            Root<Order> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            OrderQueryParam param
    ) {
        if (param == null) {
            return List.of();
        }

        SpecWrapper<Order> wrapper = SpecWrapper.create(Order.class);

        if (hasText(param.getOrderNo())) {
            wrapper.like("orderNo", param.getOrderNo());
        }

        if (hasText(param.getStatus())) {
            wrapper.eq("status", param.getStatus());
        }

        if (hasText(param.getSenderPhone())) {
            wrapper.like("senderPhone", param.getSenderPhone());
        }

        if (param.getMinTotalAmount() != null) {
            wrapper.ge("totalAmount", param.getMinTotalAmount());
        }

        if (param.getMaxTotalAmount() != null) {
            wrapper.le("totalAmount", param.getMaxTotalAmount());
        }

        // 订单项条件存在时才建立连表，避免无谓的 join 和重复行。
        if (hasText(param.getProductName()) || param.getMinQuantity() != null) {
            wrapper.join("orderProducts");

            if (hasText(param.getProductName())) {
                wrapper.like("orderProducts.productName", param.getProductName());
            }

            if (param.getMinQuantity() != null) {
                wrapper.ge("orderProducts.quantity", param.getMinQuantity());
            }
        }

        return List.of(wrapper.toPredicate(root, query, cb));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        Page<Order> orderPage = orderRepo.findAll(
                getSpecExecutor(param),
                param.pageRequest()
        );

        return jsonPageConverter.convert(
                parseVoList(orderPage.getContent()),
                orderPage.getTotalElements()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {
        return jsonPageConverter.convert(
                parseVoList(orderRepo.findAll(getSpecExecutor(param)))
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrderVo getDetail(String orderNo) throws Exception {
        Order order = orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        OrderVo vo = parseVo(order);
        vo.setOrderProducts(orderProductService.getOrderProductsByOrder(order));

        return vo;
    }

    /**
     * 判断字符串是否包含有效文本。
     *
     * @param value 待判断字符串
     * @return 非空白时返回 {@code true}
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
