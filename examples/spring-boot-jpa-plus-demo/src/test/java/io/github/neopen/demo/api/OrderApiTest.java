package io.github.neopen.demo.api;

import io.github.neopen.demo.support.TestDataFactory;
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
 * 订单接口测试。
 *
 * <p>启动完整 Web 容器并使用 MockMvc 调用 HTTP 接口。</p>
 */
@Tag("api")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrderApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void createQueryAndDetailShouldWork() throws Exception {
        String body = objectMapper.writeValueAsString(
                TestDataFactory.orderParam("API-001", "PAID")
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNo").value("API-001"))
                .andExpect(jsonPath("$.data.orderProducts.length()").value(2));

        mockMvc.perform(get("/order/hql/page").param("orderNo", "API-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].orderNo").value("API-001"))
                .andExpect(jsonPath("$.data[0].orderProducts.length()").value(2));

        mockMvc.perform(get("/order/page").param("orderNo", "API-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].orderNo").value("API-001"));

        mockMvc.perform(get("/order/hql/list").param("orderNo", "API-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/order/sql/page").param("orderNo", "API-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].orderNo").value("API-001"))
                .andExpect(jsonPath("$.data[0].orderProducts.length()").value(2));

        mockMvc.perform(get("/order/sql/list").param("orderNo", "API-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/order/hql/API-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("API-001"))
                .andExpect(jsonPath("$.data.orderProducts.length()").value(2));

        mockMvc.perform(get("/order/sql/API-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value("API-001"))
                .andExpect(jsonPath("$.data.orderProducts.length()").value(2));

        mockMvc.perform(get("/order/API-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverName").value("测试收货人"))
                .andExpect(jsonPath("$.data.orderProducts.length()").value(2));
    }
}
