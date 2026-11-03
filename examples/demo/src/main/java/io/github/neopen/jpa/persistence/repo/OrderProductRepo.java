package io.github.neopen.jpa.persistence.repo;

import io.github.neopen.jpa.bean.entity.Order;
import io.github.neopen.jpa.bean.entity.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 17:46
 */
@Repository
public interface OrderProductRepo extends JpaRepository<OrderProduct, Long> {

    List<OrderProduct> findByOrder(Order order);
}
