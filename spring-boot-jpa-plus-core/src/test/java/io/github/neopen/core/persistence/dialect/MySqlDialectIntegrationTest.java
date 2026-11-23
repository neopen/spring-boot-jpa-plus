package io.github.neopen.core.persistence.dialect;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

/**
 * MySQL 方言集成测试。
 *
 * <p>未安装或未启动 Docker 时整类跳过，不影响本地构建。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class MySqlDialectIntegrationTest extends AbstractDialectIntegrationTest {

    /** MySQL 容器。 */
    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("jpa_plus")
                    .withUsername("jpa")
                    .withPassword("jpa");

    /**
     * 提供容器连接信息。
     *
     * @return JDBC 连接属性
     */
    @Override
    protected Map<String, Object> connectionProperties() {
        return Map.of(
                "jakarta.persistence.jdbc.driver",
                "com.mysql.cj.jdbc.Driver",
                "jakarta.persistence.jdbc.url",
                MYSQL.getJdbcUrl(),
                "jakarta.persistence.jdbc.user",
                MYSQL.getUsername(),
                "jakarta.persistence.jdbc.password",
                MYSQL.getPassword()
        );
    }
}
