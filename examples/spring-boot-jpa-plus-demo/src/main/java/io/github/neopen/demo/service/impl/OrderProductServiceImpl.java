package io.github.neopen.demo.service.impl;

import io.github.neopen.convert.converter.AbstractBaseService;
import io.github.neopen.convert.converter.BaseVoCommom;
import io.github.neopen.core.persistence.hql.HqlWrapper;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.vo.OrderProductVo;
import io.github.neopen.demo.persistence.repo.OrderProductRepo;
import io.github.neopen.demo.service.OrderProductService;
import jakarta.validation.constraints.Null;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单项业务服务实现。
 */
@Service
public class OrderProductServiceImpl extends AbstractBaseService<OrderProductVo, OrderProduct, Null> implements OrderProductService {
    /** 订单项数据仓库。 */
    private final OrderProductRepo orderProductRepo;

    /** HQL 执行器。 */
    private final JpaPlusHqlExecutor hqlExecutor;

    /**
     * 创建订单项业务服务。
     *
     * @param orderProductRepo 订单项数据仓库
     * @param hqlExecutor      HQL 执行器
     */
    public OrderProductServiceImpl(
            OrderProductRepo orderProductRepo,
            JpaPlusHqlExecutor hqlExecutor
    ) {
        this.orderProductRepo = orderProductRepo;
        this.hqlExecutor = hqlExecutor;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<OrderProduct> saveOrderProduct(List<OrderProductParam> orderProducts, Order order) throws Exception {
        List<OrderProduct> orderProductList = new ArrayList<>();
        for (OrderProductParam orderProductParam : orderProducts) {
            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrder(order);
            orderProduct.setCreateTime(java.time.LocalDateTime.now());
            orderProduct.setUpdateTime(java.time.LocalDateTime.now());
            orderProduct.setProductId(orderProductParam.getProductId());
            orderProduct.setProductName(orderProductParam.getProductName());
            orderProduct.setQuantity(orderProductParam.getQuantity());
            orderProduct.setUnitPrice(orderProductParam.getUnitPrice());
            orderProductList.add(orderProduct);
        }
        return orderProductRepo.saveAll(orderProductList);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends OrderProductVo> BaseVoCommom<OrderProduct, S> baseVo(OrderProduct entity) throws Exception {
        return (t) -> {
            if (t == null)
                return null;

            OrderProductVo vo = new OrderProductVo();
            vo.setId(t.getId());
            vo.setProductId(entity.getProductId());
            vo.setProductName(entity.getProductName());
            vo.setProductCode(entity.getProductCode());
            vo.setQuantity(entity.getQuantity());
            vo.setUnitPrice(entity.getUnitPrice());
            vo.setProductImage(entity.getProductImage());

            return (S) vo;
        };
    }


    /** {@inheritDoc} */
    @Override
    public List<OrderProductVo> getOrderProductsByOrder(Order order) throws Exception {

        return batchParseVo(orderProductRepo.findByOrder(order));
    }

    /** {@inheritDoc} */
    @Override
    public Map<Long, List<OrderProductVo>> getOrderProductsByOrders(
            Collection<Order> orders
    ) throws Exception {

        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> orderIds = orders.stream()
                .filter(Objects::nonNull)
                .map(Order::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 一次查询回填全部订单项，避免订单列表逐条查询。
        HqlWrapper<OrderProduct> wrapper =
                HqlWrapper.create(OrderProduct.class)
                        .in("order.id", orderIds);

        List<OrderProduct> products =
                hqlExecutor.queryForList(wrapper);

        Map<Long, List<OrderProductVo>> result =
                new HashMap<>(orderIds.size());

        for (OrderProduct product : products) {
            OrderProductVo vo = parseVo(product);

            result.computeIfAbsent(
                    product.getOrder().getId(),
                    key -> new ArrayList<>()
            ).add(vo);
        }

        return result;
    }
}
