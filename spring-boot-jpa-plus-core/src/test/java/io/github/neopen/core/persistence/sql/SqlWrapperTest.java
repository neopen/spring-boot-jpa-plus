/*
 * Copyright 2026 neopen (余一叶知秋尽)
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
package io.github.neopen.core.persistence.sql;

import io.github.neopen.core.persistence.condition.QueryCondition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SQL 构造器单元测试。
 */
@Tag("unit")
class SqlWrapperTest {

    @Test
    void shouldBuildConditionsAndParameters() {
        SqlQuery query = SqlWrapper.create("t_order")
                .eq("status", "PAID")
                .like("order_no", "DS")
                .in("id", List.of(1L, 2L))
                .isNotNull("create_time")
                .orderByDesc("create_time")
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.* from t_order o "
                        + "where o.status = :p1 "
                        + "and o.order_no like :p2 "
                        + "and o.id in (:p3) "
                        + "and o.create_time is not null"
        );
        assertThat(query.getSql())
                .endsWith(" order by o.create_time desc");
        assertThat(query.getCountSql()).isEqualTo(
                "select count(*) from ("
                        + query.getBaseSql()
                        + ") jpa_plus_count"
        );
        assertThat(query.getParameters())
                .containsEntry("p1", "PAID")
                .containsEntry("p2", "%DS%")
                .containsEntry("p3", List.of(1L, 2L));
    }

    @Test
    void shouldBuildNestedOrGroup() {
        SqlQuery query = SqlWrapper.create("t_order")
                .eq("status", "PAID")
                .or(wrapper -> wrapper
                        .eq("status", "SHIPPED")
                        .like("receiver_name", "张")
                )
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.* from t_order o "
                        + "where o.status = :p1 "
                        + "or (o.status = :p2 "
                        + "and o.receiver_name like :p3)"
        );
        assertThat(query.getParameters())
                .containsEntry("p1", "PAID")
                .containsEntry("p2", "SHIPPED")
                .containsEntry("p3", "%张%");
    }

    @Test
    void shouldHandleNullAndEmptyCollections() {
        SqlQuery query = SqlWrapper.create("t_order")
                .eq("remark", null)
                .ne("tracking_number", null)
                .in("id", List.of())
                .notIn("user_id", List.of())
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.* from t_order o "
                        + "where o.remark is null "
                        + "and o.tracking_number is not null "
                        + "and 1 = 0"
        );
        assertThat(query.getParameters()).isEmpty();
    }

    @Test
    void shouldSupportSelectGroupByAndCustomAlias() {
        SqlQuery query = SqlWrapper.create("t_product", "p")
                .select("p.stock_status", "count(*) as total")
                .eq("is_active", true)
                .groupBy("stock_status")
                .orderByAsc("stock_status")
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select p.stock_status, count(*) as total "
                        + "from t_product p "
                        + "where p.is_active = :p1 "
                        + "group by p.stock_status"
        );
        assertThat(query.getSql())
                .endsWith(" order by p.stock_status asc");
        assertThat(query.getParameters())
                .containsEntry("p1", true);
    }

    @Test
    void shouldSupportCustomOperator() {
        SqlOperator endsWith = new SqlOperator() {
            @Override
            public int parameterCount() {
                return 1;
            }

            @Override
            public String render(String path, List<String> names) {
                return path + " like :" + names.get(0);
            }
        };

        SqlQuery query = SqlWrapper.create("t_order")
                .add("order_no", endsWith, "DS")
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.* from t_order o "
                        + "where o.order_no like :p1"
        );
        assertThat(query.getParameters())
                .containsEntry("p1", "DS");
    }

    @Test
    void shouldAppendRawAndExistsFragments() {
        SqlQuery query = SqlWrapper.create("t_order")
                .raw("1 = 1")
                .exists("select 1 from t_order_item i"
                        + " where i.order_id = o.id")
                .notExists("select 1 from t_payment p"
                        + " where p.order_id = o.id")
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.* from t_order o where 1 = 1"
                        + " and exists (select 1 from t_order_item i"
                        + " where i.order_id = o.id)"
                        + " and not exists (select 1 from t_payment p"
                        + " where p.order_id = o.id)"
        );

        assertThatThrownBy(() -> SqlWrapper.create("t_order").raw(" "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> SqlWrapper.create("t_order")
                .notExists(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMapQueryParameterFieldToColumn() {
        SqlQuery query = SqlWrapper.create("t_order")
                .alias("keyword", "order_no")
                .apply(new SampleQuery("DS"))
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.* from t_order o where o.order_no = :p1"
        );
        assertThat(query.getParameters()).containsEntry("p1", "DS");
    }

    /** 测试查询参数。 */
    private static final class SampleQuery {

        /** 关键字。 */
        @QueryCondition
        private final String keyword;

        /**
         * 创建查询参数。
         *
         * @param keyword 关键字
         */
        private SampleQuery(String keyword) {
            this.keyword = keyword;
        }
    }

    /**
     * 验证 group by、having 与参数化 exists 组合。
     */
    @Test
    void shouldBuildGroupByHavingAndParameterizedExists() {
        SqlQuery query = SqlWrapper.create("t_order")
                .select("status", "count(*) as total")
                .groupBy("status")
                .having("count(*) > 1")
                .exists(
                        "select 1 from t_order_item i"
                                + " where i.order_id = o.id"
                                + " and i.sku = :sku",
                        Map.of("sku", "A-1")
                )
                .build();

        String base = "select status, count(*) as total"
                + " from t_order o"
                + " where exists (select 1 from t_order_item i"
                + " where i.order_id = o.id"
                + " and i.sku = :p1)"
                + " group by o.status"
                + " having count(*) > 1";

        assertThat(query.getBaseSql()).isEqualTo(base);
        assertThat(query.getCountSql()).isEqualTo(
                "select count(*) from (" + base + ") jpa_plus_count"
        );
        assertThat(query.getParameters()).containsEntry("p1", "A-1");

        assertThatThrownBy(() -> SqlWrapper.create("t_order").having(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证 union 合并语句、参数与统计语句。
     */
    @Test
    void shouldBuildUnionWithMergedParameters() {
        SqlWrapper archived = SqlWrapper.create("t_order_archive", "a")
                .select("a.id", "a.order_no")
                .eq("a.status", "CLOSED");

        SqlWrapper wrapper = SqlWrapper.create("t_order", "o")
                .select("o.id", "o.order_no")
                .eq("status", "PAID")
                .union(archived)
                .orderByDesc("id");

        SqlQuery query = wrapper.build();

        String base = "select o.id, o.order_no from t_order o"
                + " where o.status = :p1"
                + " union "
                + "select a.id, a.order_no from t_order_archive a"
                + " where a.status = :p2";

        assertThat(wrapper.hasUnion()).isTrue();
        assertThat(wrapper.getSortAlias()).isNull();
        assertThat(query.getBaseSql()).isEqualTo(base);
        assertThat(query.getSql()).isEqualTo(base + " order by id desc");
        assertThat(query.getCountSql()).isEqualTo(
                "select count(*) from (" + base + ") jpa_plus_count"
        );
        assertThat(query.getParameters())
                .containsEntry("p1", "PAID")
                .containsEntry("p2", "CLOSED");
    }

    /**
     * 验证 union all 保留重复行并支持多段合并。
     */
    @Test
    void shouldBuildUnionAllForMultipleParts() {
        SqlWrapper first = SqlWrapper.create("t_order_2024", "y1")
                .select("y1.id");
        SqlWrapper second = SqlWrapper.create("t_order_2025", "y2")
                .select("y2.id");

        SqlQuery query = SqlWrapper.create("t_order", "o")
                .select("o.id")
                .unionAll(first, second)
                .build();

        assertThat(query.getBaseSql()).isEqualTo(
                "select o.id from t_order o"
                        + " union all select y1.id from t_order_2024 y1"
                        + " union all select y2.id from t_order_2025 y2"
        );
    }

    /**
     * 验证 union 子查询的非法用法被拒绝。
     */
    @Test
    void shouldRejectInvalidUnionParts() {
        assertThatThrownBy(() -> SqlWrapper.create("t_order")
                .union((SqlWrapper) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Union part must not be null");

        assertThatThrownBy(() -> SqlWrapper.create("t_order")
                .union(SqlWrapper.create("t_order_archive")
                        .orderByAsc("id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("order by");

        SqlWrapper nested = SqlWrapper.create("t_order_2024")
                .union(SqlWrapper.create("t_order_2025"));

        assertThatThrownBy(() -> SqlWrapper.create("t_order")
                .union(nested))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nested union");
    }

    @Test
    void shouldRejectInvalidInput() {
        assertThatThrownBy(() -> SqlWrapper.create(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Table");

        assertThatThrownBy(() -> SqlWrapper.create("t_order")
                .add("status", StandardSqlOperator.BETWEEN, "PAID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expects 2");
    }
}
