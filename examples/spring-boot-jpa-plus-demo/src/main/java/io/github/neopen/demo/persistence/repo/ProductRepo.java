package io.github.neopen.demo.persistence.repo;

import io.github.neopen.demo.bean.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <b>
 * <p>
 *
 * @author neopen
 * @github https://github.com/neopen/spring-boot-jpa-plus
 * @date 2025/12/23 16:44
 */
@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {


}
