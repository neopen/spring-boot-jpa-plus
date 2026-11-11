package io.github.neopen.demo.service.support;

import cn.hutool.core.util.RandomUtil;
import io.github.neopen.convert.converter.AbstractBaseService;
import io.github.neopen.convert.converter.BaseVoCommom;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.param.OrderParam;
import io.github.neopen.demo.bean.param.OrderProductParam;
import io.github.neopen.demo.bean.param.OrderQueryParam;
import io.github.neopen.demo.bean.vo.OrderProductVo;
import io.github.neopen.demo.bean.vo.OrderVo;
import io.github.neopen.demo.persistence.repo.OrderRepo;
import io.github.neopen.demo.service.OrderProductService;
import io.github.neopen.demo.service.OrderService;
import io.github.neopen.extension.response.JsonPageConverter;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 订单服务公共实现。
 *
 * <p>负责订单保存、示例数据生成和视图转换；查询读取由 JPA 与 HQL
 * 两个子类分别实现。</p>
 */
public abstract class AbstractOrderService
        extends AbstractBaseService<OrderVo, Order, OrderQueryParam>
        implements OrderService {

    /** 订单数据仓库。 */
    protected final OrderRepo orderRepo;

    /** 订单项业务服务。 */
    protected final OrderProductService orderProductService;

    /** 分页响应转换器。 */
    protected final JsonPageConverter jsonPageConverter;

    /**
     * 创建订单服务公共实现。
     *
     * @param orderRepo           订单数据仓库
     * @param orderProductService 订单项业务服务
     * @param jsonPageConverter   分页响应转换器
     */
    protected AbstractOrderService(
            OrderRepo orderRepo,
            OrderProductService orderProductService,
            JsonPageConverter jsonPageConverter
    ) {
        this.orderRepo = orderRepo;
        this.orderProductService = orderProductService;
        this.jsonPageConverter = jsonPageConverter;
    }

    /** {@inheritDoc} */
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
        save.setOrderProducts(
                orderProductService.saveOrderProduct(param.getOrderProducts(), save)
        );

        OrderVo vo = parseVo(save);
        vo.setOrderProducts(orderProductService.getOrderProductsByOrder(save));

        return vo;
    }

    /** {@inheritDoc} */
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
                new OrderProductParam(
                        RandomUtil.randomLong(999999999),
                        String.valueOf(RandomUtil.randomChinese()),
                        BigDecimal.valueOf(RandomUtil.randomDouble(1, 100)),
                        RandomUtil.randomInt(1, 100),
                        RandomUtil.randomString(16)
                ),
                new OrderProductParam(
                        RandomUtil.randomLong(999999999),
                        String.valueOf(RandomUtil.randomChinese()),
                        BigDecimal.valueOf(RandomUtil.randomDouble(1, 100)),
                        RandomUtil.randomInt(1, 100),
                        RandomUtil.randomString(16)
                )
        ));

        return add(order);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends OrderVo> BaseVoCommom<Order, S> baseVo(Order entity) {
        return (t) -> {
            if (t == null) {
                return null;
            }

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

            return (S) vo;
        };
    }

    /** {@inheritDoc} */
    @Override
    public <S extends Order> OrderVo parseVo(S entity) throws Exception {
        OrderVo vo = super.parseVo(entity);

        vo.setTrackingNumber(entity.getTrackingNumber());
        vo.setPaymentMethod(entity.getPaymentMethod());

        return vo;
    }

    /**
     * 批量转换订单，并一次查询回填全部订单项，避免列表查询产生 N+1。
     *
     * @param orders 订单实体列表
     * @return 订单视图列表
     * @throws Exception 转换失败时抛出
     */
    protected List<OrderVo> parseVoList(List<Order> orders) throws Exception {
        List<OrderVo> vos = batchParseVo(orders);

        Map<Long, List<OrderProductVo>> productsByOrder =
                orderProductService.getOrderProductsByOrders(orders);

        for (OrderVo vo : vos) {
            vo.setOrderProducts(
                    productsByOrder.getOrDefault(vo.getId(), List.of())
            );
        }

        return vos;
    }
}
