package io.github.neopen.jpa.persistence.repo;

import io.github.neopen.jpa.bean.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
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
