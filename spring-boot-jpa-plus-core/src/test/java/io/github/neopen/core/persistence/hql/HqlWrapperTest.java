package io.github.neopen.core.persistence.hql;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    /** 测试实体占位。 */
    private static final class SampleEntity {
    }
}
