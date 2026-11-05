package io.github.neopen.demo.persistence.repo;

import io.github.neopen.demo.bean.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 订单数据仓库。
 *
 * <p>同时支持通用数据访问和动态条件查询。</p>
 */
@Repository
public interface OrderRepo extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * 根据订单编号查询订单。
     *
     * @param orderNo 订单编号
     * @return 包含订单的可选值；未找到时返回空可选值
     */
    Optional<Order> findByOrderNo(String orderNo);
}
