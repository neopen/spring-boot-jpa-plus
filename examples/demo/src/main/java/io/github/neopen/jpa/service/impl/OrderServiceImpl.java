package io.github.neopen.jpa.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.neopen.jpa.bean.Json;
import io.github.neopen.jpa.bean.entity.Order;
import io.github.neopen.jpa.bean.entity.OrderProduct;
import io.github.neopen.jpa.bean.param.OrderParam;
import io.github.neopen.jpa.bean.vo.OrderDetailVo;
import io.github.neopen.jpa.bean.vo.OrderVo;
import io.github.neopen.jpa.convert.AbstractBaseService;
import io.github.neopen.jpa.convert.BaseVoCommom;
import io.github.neopen.jpa.persistence.repo.OrderProductRepo;
import io.github.neopen.jpa.persistence.repo.OrderRepo;
import io.github.neopen.jpa.persistence.repo.ProductRepo;
import io.github.neopen.jpa.service.OrderService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 17:05
 */
@Service
public class OrderServiceImpl extends AbstractBaseService<OrderVo, Order, OrderParam> implements OrderService {
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private OrderProductRepo orderProductRepo;
    @Autowired
    private ProductRepo productRepo;

    @Override
    public <S extends OrderParam> List<Predicate> getPredicates(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb, S param) throws Exception {
        List<Predicate> predicates = super.getPredicates(root, query, cb, param, param);

        if (ObjectUtil.isNotNull(param.getStatus()))
            predicates.add(cb.equal(root.<String>get("status"), param.getStatus()));

        if (ObjectUtil.isNotNull(param.getSenderPhone()))
            predicates.add(cb.equal(root.<String>get("senderPhone"), param.getSenderPhone()));

        if (StrUtil.isNotEmpty(param.getOrderNo()))
            predicates.add(cb.like(root.get("orderNo"), param.getOrderNo() + "%"));

        return predicates;
    }

    @Override
    public <S extends OrderVo> BaseVoCommom<Order, S> baseVo(Order entity) throws Exception {
        return (t) -> {
            if (t == null)
                return null;

            OrderVo vo = new OrderVo();
            vo.setCreateTime(t.getCreateTime());
            vo.setDiscountAmount(t.getDiscountAmount());
            vo.setOrderNo(t.getOrderNo());
            vo.setPaymentTime(t.getPaymentTime());
            vo.setReceiverAddress(t.getReceiverAddress());
            vo.setReceiverName(t.getReceiverName());
            vo.setReceiverPhone(t.getReceiverPhone());
            vo.setRemark(t.getRemark());
            vo.setShippingAddress(t.getShippingAddress());

            vo.setShippingFee(t.getShippingFee());
            vo.setStatus(t.getStatus());
            vo.setTotalAmount(t.getTotalAmount().toPlainString());
            vo.setId(t.getId());
            vo.setSenderName(t.getSenderName());
            vo.setSenderPhone(t.getSenderPhone());
            vo.setShippingAddress(t.getShippingAddress());

            return (S) vo;
        };
    }

    @Override
    public Json<List<OrderVo>> getPage(OrderParam param) throws Exception {
        return getCommonPage(orderRepo.findAll(getSpecExecutor(param), param.pageRequest()));
    }

    @Override
    public <S extends Order> OrderDetailVo parseVo(S entity) throws Exception {
        OrderDetailVo vo = (OrderDetailVo) super.parseVo(entity);

        vo.setReceivingBranchAddress(entity.getReceivingBranchAddress());
        vo.setReceivingBranchName(entity.getReceivingBranchName());
        vo.setReceivingBranchPhone(entity.getReceivingBranchPhone());
        vo.setShippingBranchAddress(entity.getShippingBranchAddress());
        vo.setShippingBranchName(entity.getShippingBranchName());
        vo.setShippingBranchPhone(entity.getShippingBranchPhone());
        vo.setTrackingNumber(entity.getTrackingNumber());
        vo.setLogisticsCompany(entity.getLogisticsCompany());
        vo.setPaymentMethod(entity.getPaymentMethod());
        vo.setEstimatedDeliveryTime(entity.getEstimatedDeliveryTime());
        vo.setActualDeliveryTime(entity.getActualDeliveryTime());

        List<OrderProduct> products = orderProductRepo.findByOrder(entity);

        vo.setOrderProducts();

        return vo;
    }

    @Override
    public OrderDetailVo getDetail(String orderNo) throws Exception {
        return this.parseVo(orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new Exception("订单不存在")));
    }


}
