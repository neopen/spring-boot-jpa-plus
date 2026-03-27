package com.hengline.jpa.persistence.repo;

import com.hengline.jpa.bean.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <b>
 * <p>
 *
 * @author HengLine
 * @email haeng2030@gmail.com
 * @date 2025/12/23 16:42
 */
@Repository
public interface OrderRepo extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * 根据订单编号查询订单
     *
     * @param orderNo 订单编号
     * @return 订单
     */
    Optional<Order> findByOrderNo(String orderNo);
}
