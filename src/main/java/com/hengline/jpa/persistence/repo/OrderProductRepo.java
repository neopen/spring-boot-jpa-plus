package com.hengline.jpa.persistence.repo;

import com.hengline.jpa.bean.entity.Order;
import com.hengline.jpa.bean.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author HengLine
 * @email haeng2030@gmail.com
 * @date 2025/12/23 17:46
 */
@Repository
public interface OrderProductRepo extends JpaRepository<OrderProduct, Long> {

    List<OrderProduct> findByOrder(Order order);
}
