package io.github.neopen.demo.persistence.repo;

import io.github.neopen.demo.bean.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 商品数据仓库。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Repository
public interface ProductRepo
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {


}
