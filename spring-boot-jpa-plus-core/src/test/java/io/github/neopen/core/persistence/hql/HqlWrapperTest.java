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
package io.github.neopen.core.persistence.hql;

import io.github.neopen.core.persistence.condition.QueryCondition;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link HqlWrapper} 单元测试。
 */
class HqlWrapperTest {

    @Test
    void shouldBuildWhereConditionsAndParameters() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .eq("status", "PAID")
                .like("orderNo", "DS")
                .in("id", List.of(1L, 2L))
                .isNotNull("createTime")
                .orderByDesc("createTime")
                .build();

        String where = " where o.status = :p1"
                + " and o.orderNo like :p2"
                + " and o.id in (:p3)"
                + " and o.createTime is not null";

        assertThat(query.getBaseHql())
                .isEqualTo("select o from SampleEntity o" + where);

        assertThat(query.getCountHql())
                .isEqualTo("select count(*) from SampleEntity o" + where);

        assertThat(query.getParameters())
                .containsEntry("p1", "PAID")
                .containsEntry("p2", "%DS%")
                .containsEntry("p3", List.of(1L, 2L));

        assertThat(query.getHql())
                .endsWith("order by o.createTime desc");
    }

    @Test
    void shouldBuildNestedOrGroup() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .eq("status", "PAID")
                .or(w -> w.like("orderNo", "A").eq("userId", 1L))
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select o from SampleEntity o"
                        + " where o.status = :p1"
                        + " or (o.orderNo like :p2 and o.userId = :p3)"
        );

        assertThat(query.getParameters())
                .containsEntry("p2", "%A%")
                .containsEntry("p3", 1L);
    }

    @Test
    void shouldHandleNullAndEmptyCollectionConditions() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .eq("paymentTime", null)
                .ne("createTime", null)
                .in("id", List.of())
                .notIn("productId", List.of())
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select o from SampleEntity o"
                        + " where o.paymentTime is null"
                        + " and o.createTime is not null"
                        + " and 1 = 0"
        );
    }

    @Test
    void shouldSupportCustomOperator() {
        HqlOperator lowerEq = new HqlOperator() {
            @Override
            public int parameterCount() {
                return 1;
            }

            @Override
            public String render(String path, List<String> names) {
                return "lower(" + path + ") = :" + names.get(0);
            }
        };

        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .add("orderNo", lowerEq, "DS")
                .build();

        assertThat(query.getBaseHql())
                .contains("lower(o.orderNo) = :p1");
        assertThat(query.getParameters())
                .containsEntry("p1", "DS");
    }

    @Test
    void shouldUseCustomAliasAndSpringSort() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class, "t")
                .eq("status", "PAID")
                .orderBy(Sort.by(
                        Sort.Order.asc("status"),
                        Sort.Order.desc("createTime")
                ))
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select t from SampleEntity t where t.status = :p1"
        );

        assertThat(query.getOrderBy())
                .isEqualTo("t.status asc, t.createTime desc");
    }

    @Test
    void shouldAppendRawAndExistsFragments() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .raw("1 = 1")
                .exists("select 1 from OrderItem i where i.orderId = o.id")
                .notExists("select 1 from Payment p where p.orderId = o.id")
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select o from SampleEntity o where 1 = 1"
                        + " and exists (select 1 from OrderItem i"
                        + " where i.orderId = o.id)"
                        + " and not exists (select 1 from Payment p"
                        + " where p.orderId = o.id)"
        );

        assertThatThrownBy(() -> HqlWrapper.create(SampleEntity.class)
                .raw(" "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> HqlWrapper.create(SampleEntity.class)
                .exists(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMapQueryParameterFieldToHqlPath() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .alias("keyword", "order.status")
                .apply(new SampleQuery("PAID"))
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select o from SampleEntity o where o.order.status = :p1"
        );
        assertThat(query.getParameters()).containsEntry("p1", "PAID");
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
     * 验证 select、group by 与 having 组合。
     */
    @Test
    void shouldBuildSelectGroupByAndHaving() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .select("status", "count(id)")
                .groupBy("status")
                .having("count(o.id) > 1")
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select o.status, count(id) from SampleEntity o"
                        + " group by o.status"
                        + " having count(o.id) > 1"
        );

        assertThat(query.getCountHql())
                .isEqualTo("select count(*) from SampleEntity o");

        assertThatThrownBy(() -> HqlWrapper.create(SampleEntity.class)
                .having(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证 exists 子查询的命名参数绑定。
     */
    @Test
    void shouldBindParametersInExistsSubQuery() {
        HqlQuery query = HqlWrapper.create(SampleEntity.class)
                .exists(
                        "select 1 from OrderItem i"
                                + " where i.orderId = o.id"
                                + " and i.sku = :sku",
                        Map.of("sku", "A-1")
                )
                .notExists(
                        "select 1 from Payment p"
                                + " where p.orderId = o.id"
                                + " and p.channel = :channel",
                        Map.of("channel", "WECHAT")
                )
                .build();

        assertThat(query.getBaseHql()).isEqualTo(
                "select o from SampleEntity o"
                        + " where exists (select 1 from OrderItem i"
                        + " where i.orderId = o.id"
                        + " and i.sku = :p1)"
                        + " and not exists (select 1 from Payment p"
                        + " where p.orderId = o.id"
                        + " and p.channel = :p2)"
        );

        assertThat(query.getParameters())
                .containsEntry("p1", "A-1")
                .containsEntry("p2", "WECHAT");
    }

    /** 测试实体占位。 */
    private static final class SampleEntity {
    }
}
