package io.github.neopen.core.persistence.sort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Sorts} 单元测试。
 */
class SortsTest {

    /**
     * 验证多字段排序表达式解析。
     */
    @Test
    void shouldParseMultipleSortFields() {
        Sort sort = Sorts.parse("createTime,desc;id,asc", Sample.class);

        assertThat(sort).containsExactly(
                Sort.Order.desc("createTime"),
                Sort.Order.asc("id")
        );
    }

    /**
     * 验证省略方向时默认升序。
     */
    @Test
    void shouldDefaultToAscending() {
        Sort sort = Sorts.parse("id", Sample.class);

        assertThat(sort).containsExactly(Sort.Order.asc("id"));
    }

    /**
     * 验证空白表达式返回未排序。
     */
    @Test
    void shouldReturnUnsortedForBlankExpression() {
        assertThat(Sorts.parse(null, Sample.class)).isEqualTo(Sort.unsorted());
        assertThat(Sorts.parse("  ", Sample.class)).isEqualTo(Sort.unsorted());
    }

    /**
     * 验证父类字段可用于排序。
     */
    @Test
    void shouldAcceptInheritedFields() {
        Sort sort = Sorts.parse("tenantId,desc", Sample.class);

        assertThat(sort).containsExactly(Sort.Order.desc("tenantId"));
    }

    /**
     * 验证未知字段被拒绝。
     */
    @Test
    void shouldRejectUnknownField() {
        assertThatThrownBy(() -> Sorts.parse("password,asc", Sample.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    /**
     * 验证非法方向被拒绝。
     */
    @Test
    void shouldRejectInvalidDirection() {
        assertThatThrownBy(() -> Sorts.parse("id,sideways", Sample.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort direction");
    }

    /**
     * 验证允许字段集合模式。
     */
    @Test
    void shouldParseAgainstAllowedFields() {
        Sort sort = Sorts.parse(
                "totalAmount,desc",
                Set.of("id", "totalAmount")
        );

        assertThat(sort).containsExactly(Sort.Order.desc("totalAmount"));
    }

    /** 父类字段。 */
    private static class BaseEntity {

        /** 租户主键。 */
        private Long tenantId;
    }

    /** 测试实体。 */
    private static class Sample extends BaseEntity {

        /** 主键。 */
        private Long id;

        /** 创建时间。 */
        private String createTime;

        /** 金额。 */
        private Integer totalAmount;
    }
}
