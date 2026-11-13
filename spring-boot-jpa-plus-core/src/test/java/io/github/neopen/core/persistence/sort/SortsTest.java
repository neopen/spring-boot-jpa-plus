package io.github.neopen.core.persistence.sort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Map;
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

    /**
     * 验证嵌套属性路径可用于排序。
     */
    @Test
    void shouldAcceptNestedPath() {
        Sort sort = Sorts.parse("customer.name,asc", Sample.class);

        assertThat(sort).containsExactly(Sort.Order.asc("customer.name"));
    }

    /**
     * 验证嵌套路径中的未知字段被拒绝。
     */
    @Test
    void shouldRejectUnknownNestedField() {
        assertThatThrownBy(
                () -> Sorts.parse("customer.password,asc", Sample.class)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort field");
    }

    /**
     * 验证简单类型之后无法继续展开路径。
     */
    @Test
    void shouldRejectPathThroughSimpleType() {
        assertThatThrownBy(
                () -> Sorts.parse("customer.name.length,asc", Sample.class)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not sortable");
    }

    /**
     * 验证路径层级超过上限时被拒绝。
     */
    @Test
    void shouldRejectTooDeepPath() {
        assertThatThrownBy(
                () -> Sorts.parse(
                        "customer.address.city.code,asc",
                        Sample.class
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too deep");
    }

    /**
     * 验证别名映射转换为实体属性路径。
     */
    @Test
    void shouldResolveAliasToEntityPath() {
        Sort sort = Sorts.parse(
                "name,desc",
                Sample.class,
                Map.of("name", "customer.name")
        );

        assertThat(sort).containsExactly(
                Sort.Order.desc("customer.name")
        );
    }

    /**
     * 验证未登记别名的字段按原字段名校验。
     */
    @Test
    void shouldFallbackToEntityFieldWhenAliasMissing() {
        Sort sort = Sorts.parse(
                "id,asc",
                Sample.class,
                Map.of("name", "customer.name")
        );

        assertThat(sort).containsExactly(Sort.Order.asc("id"));
    }

    /**
     * 验证别名指向非法路径时被拒绝。
     */
    @Test
    void shouldRejectInvalidAliasTarget() {
        assertThatThrownBy(
                () -> Sorts.parse(
                        "name,asc",
                        Sample.class,
                        Map.of("name", "customer.password")
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported sort field");
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

        /** 客户。 */
        private Customer customer;
    }

    /** 客户。 */
    private static class Customer {

        /** 客户名称。 */
        private String name;

        /** 地址。 */
        private Address address;
    }

    /** 地址。 */
    private static class Address {

        /** 城市。 */
        private String city;
    }
}
