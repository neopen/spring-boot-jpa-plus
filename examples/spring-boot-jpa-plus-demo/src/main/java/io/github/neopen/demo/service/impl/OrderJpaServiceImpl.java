package io.github.neopen.demo.service.impl;

import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.OrderProductService;
import io.github.neopen.demo.service.support.AbstractOrderService;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 Spring Data JPA 的订单服务实现。
 *
 * <p>使用 {@code Specification} 动态拼接查询条件，通过仓库完成分页和列表查询。</p>
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

    /** {@inheritDoc} */
    @Override
    public List<Predicate> buildPredicates(
            Root<Order> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            OrderQueryParam param
    ) {
        List<Predicate> predicates = new ArrayList<>(16);

        if (param == null) {
            return predicates;
        }

        if (param.getOrderNo() != null && !param.getOrderNo().isBlank()) {
            predicates.add(cb.like(
                    root.get("orderNo"),
                    "%" + param.getOrderNo() + "%"
            ));
        }

        if (param.getStatus() != null && !param.getStatus().isBlank()) {
            predicates.add(cb.equal(root.get("status"), param.getStatus()));
        }

        return predicates;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {
        return jsonPageConverter.convert(
                parseVoList(orderRepo.findAll(getSpecExecutor(param)))
        );
    }

    /** {@inheritDoc} */
    @Override
    public OrderVo getDetail(String orderNo) throws Exception {
        Order order = orderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));

        OrderVo vo = parseVo(order);
        vo.setOrderProducts(orderProductService.getOrderProductsByOrder(order));

        return vo;
    }
}
