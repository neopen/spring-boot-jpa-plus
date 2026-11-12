package io.github.neopen.core.persistence.sql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

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
