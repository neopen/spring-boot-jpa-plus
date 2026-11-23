package io.github.neopen.core.persistence.dialect;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

/**
 * PostgreSQL 方言集成测试。
 *
 * <p>JSON 字段在 PostgreSQL 下映射为 jsonb；未安装或未启动 Docker 时
 * 整类跳过，不影响本地构建。</p>
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlDialectIntegrationTest
        extends AbstractDialectIntegrationTest {

    /** PostgreSQL 容器。 */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
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
                "org.postgresql.Driver",
                "jakarta.persistence.jdbc.url",
                POSTGRES.getJdbcUrl(),
                "jakarta.persistence.jdbc.user",
                POSTGRES.getUsername(),
                "jakarta.persistence.jdbc.password",
                POSTGRES.getPassword()
        );
    }
}
