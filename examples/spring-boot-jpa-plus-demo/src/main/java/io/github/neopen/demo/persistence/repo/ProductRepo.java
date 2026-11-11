package io.github.neopen.demo.persistence.repo;

import io.github.neopen.demo.bean.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 商品数据仓库。
 */
@Repository
public interface ProductRepo
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {


}
