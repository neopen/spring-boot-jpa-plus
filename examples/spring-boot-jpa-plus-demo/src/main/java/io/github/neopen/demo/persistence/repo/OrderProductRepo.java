package io.github.neopen.demo.persistence.repo;

import io.github.neopen.demo.bean.entity.Order;
import io.github.neopen.demo.bean.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 订单项数据仓库。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Repository
public interface OrderProductRepo extends JpaRepository<OrderProduct, Long> {

    /**
     * 查询指定订单的全部订单项。
     *
     * @param order 订单实体
     * @return 订单项列表
     */
    List<OrderProduct> findByOrder(Order order);
}
