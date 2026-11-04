package io.github.neopen.demo.service.impl;

import cn.hutool.core.util.RandomUtil;
import io.github.neopen.convert.convert.AbstractBaseService;
import io.github.neopen.convert.convert.BaseVoCommom;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.OrderProductService;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.extension.response.Json;
import io.github.neopen.extension.response.JsonPageConverter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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
public class OrderServiceImpl extends AbstractBaseService<OrderVo, Order, OrderQueryParam> implements OrderService {
    private final OrderRepo orderRepo;
    private final OrderProductService orderProductService;
    private final JsonPageConverter jsonPageConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVo add(OrderParam param) throws Exception {
        Order order = new Order();
        order.setCreateTime(java.time.LocalDateTime.now());
        order.setUpdateTime(java.time.LocalDateTime.now());
        order.setOrderNo(param.getOrderNo());
        order.setTotalAmount(param.getTotalAmount());
        order.setStatus(param.getStatus());
        order.setUserId(param.getUserId());
        order.setShippingAddress(param.getShippingAddress());
        order.setSenderName(param.getSenderName());
        order.setSenderPhone(param.getSenderPhone());
        order.setReceiverName(param.getReceiverName());
        order.setReceiverAddress(param.getReceiverAddress());
        order.setReceiverPhone(param.getReceiverPhone());
        order.setRemark(param.getRemark());
        order.setShippingFee(param.getShippingFee());
        order.setDiscountAmount(param.getDiscountAmount());
        order.setPaymentMethod(param.getPaymentMethod());
        order.setTrackingNumber(param.getTrackingNumber());
        Order save = orderRepo.save(order);
        save.setOrderProducts(orderProductService.saveOrderProduct(param.getOrderProducts(), save));
        return parseVo(save);
    }

    @Override
    public OrderVo generateOrder() throws Exception {
        OrderParam order = new OrderParam();
        order.setOrderNo("DS" + System.currentTimeMillis());
        order.setTotalAmount(BigDecimal.valueOf(100.00));
        order.setStatus("待支付");
        order.setUserId(RandomUtil.randomLong(888));
        order.setShippingAddress("上海");
        order.setSenderName("张三");
        order.setSenderPhone("12345678901");
        order.setReceiverName("李四");
        order.setReceiverAddress("北京");
        order.setReceiverPhone("12345678901");
        order.setRemark("无");
        order.setShippingFee(BigDecimal.valueOf(RandomUtil.randomDouble(1, 100)));
        order.setDiscountAmount(BigDecimal.valueOf(0.00));
        order.setPaymentMethod("支付宝");
        order.setTrackingNumber("202512230001");

        order.setOrderProducts(List.of(
                new OrderProductParam(RandomUtil.randomLong(999999999), String.valueOf(RandomUtil.randomChinese()), BigDecimal.valueOf(RandomUtil.randomDouble(1, 100)), RandomUtil.randomInt(1, 100), RandomUtil.randomString(16)),
                new OrderProductParam(RandomUtil.randomLong(999999999), String.valueOf(RandomUtil.randomChinese()), BigDecimal.valueOf(RandomUtil.randomDouble(1, 100)), RandomUtil.randomInt(1, 100), RandomUtil.randomString(16))
        ));
        return add(order);
    }

    @Override
    public List<Predicate> buildPredicates(
            Root<Order> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            OrderQueryParam param) throws Exception {

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
    public Json<List<OrderVo>> getPage(OrderQueryParam param) throws Exception {
        Page<Order> orderPage = orderRepo.findAll(getSpecExecutor(param), param.pageRequest());

        return jsonPageConverter.convert(batchParseVo(orderPage.getContent()), orderPage.getTotalElements());
    }

    @Override
    public <S extends Order> OrderVo parseVo(S entity) throws Exception {
        OrderVo vo = super.parseVo(entity);

        vo.setTrackingNumber(entity.getTrackingNumber());
        vo.setPaymentMethod(entity.getPaymentMethod());

//        获取订单产品列表
        vo.setOrderProducts(orderProductService.getOrderProductsByOrder(entity));

        return vo;
    }

    @Override
    public OrderVo getDetail(String orderNo) throws Exception {
        return this.parseVo(orderRepo.findByOrderNo(orderNo).orElseThrow(() -> new IllegalArgumentException("订单不存在")));
    }


    @Override
    public Json<List<OrderVo>> getList(OrderQueryParam param) throws Exception {

        return jsonPageConverter.convert(batchParseVo(orderRepo.findAll(getSpecExecutor(param))));
    }

    public OrderServiceImpl(OrderRepo orderRepo, OrderProductService orderProductService, JsonPageConverter jsonPageConverter) {
        this.orderRepo = orderRepo;
        this.orderProductService = orderProductService;
        this.jsonPageConverter = jsonPageConverter;
    }
}
