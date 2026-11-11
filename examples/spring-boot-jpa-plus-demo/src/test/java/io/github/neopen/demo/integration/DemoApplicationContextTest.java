package io.github.neopen.demo.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 示例应用上下文测试。
 */
@Tag("integration")
@SpringBootTest
class DemoApplicationContextTest {

    /** 验证 Spring 应用上下文能够正常加载。 */
    @Test
    void contextLoads() {
    }
}
