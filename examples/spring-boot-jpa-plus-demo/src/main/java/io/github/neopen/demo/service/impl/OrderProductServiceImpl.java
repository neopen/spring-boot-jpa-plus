package io.github.neopen.demo.service.impl;

import io.github.neopen.convert.convert.AbstractBaseService;
import io.github.neopen.convert.convert.BaseVoCommom;
import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import io.github.neopen.demo.bean.vo.OrderProductVo;
import io.github.neopen.demo.persistence.repo.OrderProductRepo;
import io.github.neopen.demo.service.OrderProductService;
import jakarta.validation.constraints.Null;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @email helpenx@gmail.com
 * @date 2026/11/4 18:51
 */
@Service
public class OrderProductServiceImpl extends AbstractBaseService<OrderProductVo, OrderProduct, Null> implements OrderProductService {
    private final OrderProductRepo orderProductRepo;

    public OrderProductServiceImpl(OrderProductRepo orderProductRepo) {
        this.orderProductRepo = orderProductRepo;
    }

    @Override
    public <S extends OrderProductVo> BaseVoCommom<OrderProduct, S> baseVo(OrderProduct entity) throws Exception {
        return (t) -> {
            if (t == null)
                return null;

            OrderProductVo vo = new OrderProductVo();
            vo.setId(t.getId());
            vo.setProductId(entity.getProductId());
            vo.setProductName(entity.getProductName());
            vo.setQuantity(entity.getQuantity());
            vo.setUnitPrice(entity.getUnitPrice());
            vo.setProductImage(entity.getProductImage());

            return (S) vo;
        };
    }


    @Override
    public List<OrderProductVo> getOrderProductsByOrder(Order order) throws Exception {

        return batchParseVo(orderProductRepo.findByOrder(order));
    }
}
