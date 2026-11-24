/*
 * Copyright 2026 neopen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
