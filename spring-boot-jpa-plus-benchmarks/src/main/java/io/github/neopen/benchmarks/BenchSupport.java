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
package io.github.neopen.benchmarks;

import io.github.neopen.core.persistence.batch.BatchOperation;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基准测试公共工具。
 *
 * <p>每个基准类使用独立的 H2 内存数据库，避免共享静态状态与
 * 建表脚本互相影响。</p>
 *
 * @author neopen
 */
final class BenchSupport {

    /** 查询与流式基准的数据量。 */
    static final int DATASET_SIZE = 10_000;

    /** 批量 upsert 的批次数据量。 */
    static final int UPSERT_SIZE = 500;

    /** 每批 flush 的条数。 */
    static final int BATCH_SIZE = 500;

    /** 数据权限允许的部门。 */
    static final long[] VISIBLE_DEPTS = {1L, 2L};

    /** 工具类不允许实例化。 */
    private BenchSupport() {
    }

    /**
     * 创建独立的实体管理器工厂。
     *
     * @param database 内存库名称
     * @return 实体管理器工厂
     */
    static EntityManagerFactory createEntityManagerFactory(String database) {
        Map<String, Object> overrides = Map.of(
                "jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1;"
                        + "DB_CLOSE_ON_EXIT=FALSE"
        );

        return Persistence.createEntityManagerFactory(
                "jpa-plus-benchmarks",
                overrides
        );
    }

    /**
     * 创建绑定指定数据源的实体管理器工厂。
     *
     * @param dataSource 数据源
     * @param ddlAuto    建表策略
     * @return 实体管理器工厂
     */
    static EntityManagerFactory createEntityManagerFactory(
            DataSource dataSource,
            String ddlAuto
    ) {
        Map<String, Object> overrides = Map.<String, Object>of(
                "hibernate.connection.datasource", dataSource,
                "hibernate.hbm2ddl.auto", ddlAuto
        );

        return Persistence.createEntityManagerFactory(
                "jpa-plus-benchmarks",
                overrides
        );
    }

    /**
     * 生成数据集：租户、部门、状态与删除标记按固定规则分布。
     *
     * @param size 数据量
     * @return 实体列表
     */
    static List<BenchDocument> dataset(int size) {
        List<BenchDocument> documents = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            documents.add(new BenchDocument(
                    "seed-" + i,
                    i % 2 == 0 ? "tenant-a" : "tenant-b",
                    (long) (i % 4 + 1),
                    i % 5 == 0 ? "CLOSED" : "ACTIVE",
                    "payload-" + i,
                    i % 10 == 0
            ));
        }

        return documents;
    }

    /**
     * 生成统一前缀的实体，用于批量 upsert。
     *
     * @param prefix  业务键前缀
     * @param size    数量
     * @param payload 业务负载
     * @return 实体列表
     */
    static List<BenchDocument> documents(
            String prefix,
            int size,
            String payload
    ) {
        List<BenchDocument> documents = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            documents.add(new BenchDocument(
                    prefix + i,
                    "tenant-a",
                    (long) (i % 4 + 1),
                    "ACTIVE",
                    payload,
                    Boolean.FALSE
            ));
        }

        return documents;
    }

    /**
     * 批量写入实体。
     *
     * @param emf       实体管理器工厂
     * @param documents 实体列表
     */
    static void insert(
            EntityManagerFactory emf,
            List<BenchDocument> documents
    ) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            BatchOperation.persistAll(em, documents, BATCH_SIZE);
            em.getTransaction().commit();
        }
    }

    /**
     * 删除指定业务键前缀的数据。
     *
     * @param emf    实体管理器工厂
     * @param prefix 业务键前缀
     */
    static void deleteByPrefix(EntityManagerFactory emf, String prefix) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery(
                            "delete from BenchDocument e"
                                    + " where e.docNo like :prefix"
                    )
                    .setParameter("prefix", prefix + "%")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }
}
