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

import io.github.neopen.core.persistence.batch.BatchOperation;
import io.github.neopen.core.persistence.datascope.DataRule;
import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.manager.JpaPlusHqlExecutor;
import io.github.neopen.core.persistence.spec.JpaPlusSpecExecutor;
import io.github.neopen.core.persistence.tenant.Tenants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL / PostgreSQL 方言集成测试基类。
 *
 * <p>用真实数据库验证逻辑删除、租户与数据权限过滤、批量 upsert、
 * 流式分批以及 JSON 字段映射。子类提供容器连接信息；未安装 Docker
 * 时由 {@code @Testcontainers(disabledWithoutDocker = true)} 跳过。</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractDialectIntegrationTest {

    /** 测试用持久化单元。 */
    private static final String PERSISTENCE_UNIT = "jpa-plus-dialect-test";

    /** 当前租户。 */
    private static final String TENANT = "tenant-a";

    /** 实体管理器工厂。 */
    private EntityManagerFactory entityManagerFactory;

    /**
     * 提供数据库连接信息。
     *
     * @return JDBC 连接属性
     */
    protected abstract Map<String, Object> connectionProperties();

    /**
     * 创建实体管理器工厂并注册过滤提供者。
     */
    @BeforeAll
    void createEntityManagerFactory() {
        Tenants.setProvider(() -> TENANT);
        DataScopes.setProvider(type -> List.of(
                DataRule.of("deptId", 1L, 2L)
        ));

        Map<String, Object> overrides =
                new HashMap<>(connectionProperties());
        overrides.put("hibernate.hbm2ddl.auto", "create-drop");

        entityManagerFactory = Persistence.createEntityManagerFactory(
                PERSISTENCE_UNIT,
                overrides
        );
    }

    /**
     * 关闭实体管理器工厂并清理全局提供者。
     */
    @AfterAll
    void closeEntityManagerFactory() {
        Tenants.setProvider(null);
        DataScopes.setProvider(null);

        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    /**
     * 重建测试数据。
     *
     * <p>覆盖五种被过滤的组合：部门越权、逻辑删除、跨租户，
     * 以及两条正常可见的数据。</p>
     */
    @BeforeEach
    void seed() {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.createQuery(
                    "delete from DialectDocument"
            ).executeUpdate();

            entityManager.persist(document("doc-1", 1L, "ACTIVE", false));
            entityManager.persist(document("doc-2", 2L, "ACTIVE", false));
            entityManager.persist(document("doc-3", 3L, "ACTIVE", false));
            entityManager.persist(document("doc-4", 1L, "ACTIVE", true));
            entityManager.persist(new DialectDocument(
                    "doc-5",
                    "tenant-b",
                    1L,
                    "ACTIVE",
                    "payload",
                    false,
                    null
            ));
            entityManager.persist(document("doc-6", 2L, "CLOSED", false));

            entityManager.getTransaction().commit();
        }
    }

    /**
     * 验证逻辑删除、租户与数据权限过滤在真实方言下的组合结果。
     */
    @Test
    void shouldApplyLogicDeleteTenantAndDataScopeFilters() {
        assertThat(visibleDocNos())
                .containsExactlyInAnyOrder("doc-1", "doc-2", "doc-6");
    }

    /**
     * 验证可以按需关闭逻辑删除过滤。
     */
    @Test
    void shouldDisableLogicDeleteFilterOnDemand() {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            List<String> docNos = JpaPlusSpecExecutor
                    .of(entityManager, DialectDocument.class)
                    .logicDelete(false)
                    .findAll(null)
                    .stream()
                    .map(DialectDocument::getDocNo)
                    .toList();

            assertThat(docNos).containsExactlyInAnyOrder(
                    "doc-1", "doc-2", "doc-4", "doc-6"
            );
        }
    }

    /**
     * 验证批量 upsert 在真实方言下的新增与更新计数。
     */
    @Test
    void shouldUpsertBatchOfDocuments() {
        List<DialectDocument> batch = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            batch.add(document("upsert-" + i, 1L, "ACTIVE", false));
        }

        BatchOperation.UpsertResult inserted = upsert(batch);

        assertThat(inserted.inserted()).isEqualTo(100);
        assertThat(inserted.updated()).isZero();
        assertThat(visibleDocNos()).hasSize(103);

        BatchOperation.UpsertResult updated = upsert(batch);

        assertThat(updated.inserted()).isZero();
        assertThat(updated.updated()).isEqualTo(100);
    }

    /**
     * 验证 HQL 流式分批消费在真实方言下的批次边界。
     */
    @Test
    void shouldConsumeHqlResultsInBatches() {
        List<Integer> sizes = new ArrayList<>();

        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();

            new JpaPlusHqlExecutor(entityManager).forEachBatch(
                    "select d from DialectDocument d order by d.id",
                    null,
                    DialectDocument.class,
                    4,
                    batch -> sizes.add(batch.size())
            );

            entityManager.getTransaction().commit();
        }

        assertThat(sizes).containsExactly(4, 2);
    }

    /**
     * 验证 JSON 字段写入后可按原结构读回。
     */
    @Test
    void shouldRoundTripJsonAttributes() {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();
            entityManager.persist(new DialectDocument(
                    "json-1",
                    TENANT,
                    1L,
                    "ACTIVE",
                    "payload",
                    false,
                    Map.of("color", "red", "size", 3)
            ));
            entityManager.getTransaction().commit();
        }

        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            DialectDocument loaded = entityManager.createQuery(
                            "select d from DialectDocument d"
                                    + " where d.docNo = :docNo",
                            DialectDocument.class
                    )
                    .setParameter("docNo", "json-1")
                    .getSingleResult();

            assertThat(loaded.getAttributes())
                    .containsExactlyInAnyOrderEntriesOf(
                            Map.of("color", "red", "size", 3)
                    );
        }
    }

    /**
     * 创建当前租户下的测试实体。
     *
     * @param docNo   业务键
     * @param deptId  部门
     * @param status  状态
     * @param deleted 是否已删除
     * @return 测试实体
     */
    private DialectDocument document(
            String docNo,
            Long deptId,
            String status,
            boolean deleted
    ) {
        return new DialectDocument(
                docNo,
                TENANT,
                deptId,
                status,
                "payload",
                deleted,
                null
        );
    }

    /**
     * 查询当前可见的业务键。
     *
     * @return 业务键列表
     */
    private List<String> visibleDocNos() {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            return JpaPlusSpecExecutor
                    .of(entityManager, DialectDocument.class)
                    .findAll(null)
                    .stream()
                    .map(DialectDocument::getDocNo)
                    .toList();
        }
    }

    /**
     * 执行一次批量 upsert。
     *
     * @param batch 待处理实体
     * @return 处理结果
     */
    private BatchOperation.UpsertResult upsert(
            List<DialectDocument> batch
    ) {
        try (EntityManager entityManager =
                     entityManagerFactory.createEntityManager()) {
            entityManager.getTransaction().begin();

            BatchOperation.UpsertResult result = BatchOperation.upsertAll(
                    entityManager,
                    DialectDocument.class,
                    batch,
                    DialectDocument::getDocNo,
                    "docNo",
                    50
            );

            entityManager.getTransaction().commit();

            return result;
        }
    }
}
