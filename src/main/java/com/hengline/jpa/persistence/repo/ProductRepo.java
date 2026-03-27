package com.hengline.jpa.persistence.repo;

import com.hengline.jpa.bean.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <b>
 * <p>
 *
 * @author HengLine
 * @email haeng2030@gmail.com
 * @date 2025/12/23 16:44
 */
@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {


}
