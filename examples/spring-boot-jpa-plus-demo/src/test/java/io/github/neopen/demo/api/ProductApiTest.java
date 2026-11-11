package io.github.neopen.demo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.neopen.demo.support.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品接口测试。
 *
 * <p>默认接口验证 JPA 实现，{@code /hql} 接口验证 HQL 实现。</p>
 */
@Tag("api")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Test
    void jpaEndpointsShouldWork() throws Exception {
        long id = createProduct("JPA 商品", 5, 10);

        mockMvc.perform(get("/product/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("JPA 商品"));

        mockMvc.perform(get("/product/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("JPA 商品"));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(post("/product/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "],\"quantity\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/product/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(25))
                .andExpect(jsonPath("$.data.stockStatus").value("INSTOCK"));

        mockMvc.perform(get("/product/stock-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].stockStatus").value("INSTOCK"));
    }

    @Test
    void hqlEndpointsShouldWork() throws Exception {
        long id = createProduct("HQL 商品", 5, 10);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/product/hql/page").param("productName", "HQL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("HQL 商品"));

        mockMvc.perform(get("/product/hql/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockStatus").value("LOWSTOCK"));

        mockMvc.perform(get("/product/hql/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("HQL 商品"));

        mockMvc.perform(post("/product/hql/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[" + id + "],\"quantity\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/product/hql/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockQuantity").value(35))
                .andExpect(jsonPath("$.data.stockStatus").value("INSTOCK"));

        mockMvc.perform(get("/product/hql/stock-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockStatus").value("INSTOCK"));
    }

    /**
     * 通过 JPA 接口创建商品。
     *
     * @param name   商品名称
     * @param stock  库存数量
     * @param safety 安全库存
     * @return 商品主键
     * @throws Exception 创建失败时抛出
     */
    private long createProduct(String name, int stock, int safety) throws Exception {
        String body = objectMapper.writeValueAsString(
                TestDataFactory.productParam(name, stock, safety, "输入设备", "品牌A")
        );

        String response = mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stockStatus").value("LOWSTOCK"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }
}
