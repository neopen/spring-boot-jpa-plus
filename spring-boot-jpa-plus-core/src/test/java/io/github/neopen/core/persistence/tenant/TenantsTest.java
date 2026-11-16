package io.github.neopen.core.persistence.tenant;

import io.github.neopen.core.scope.JpaPlusScope;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link Tenants} 单元测试。
 */
class TenantsTest {

    /**
     * 恢复默认配置。
     */
    @AfterEach
    void reset() {
        while (Tenants.isIgnored()) {
            Tenants.restore();
        }

        Tenants.setEnabled(true);
        Tenants.setProvider(null);
        Tenants.clearScope("archive");
        JpaPlusScope.clear();
    }

    /**
     * 验证跳过租户过滤后不再返回当前租户。
     */
    @Test
    void shouldIgnoreTenantFilter() {
        Tenants.setProvider(() -> "t1");

        assertThat(Tenants.callIgnored(Tenants::currentTenant)).isNull();
        assertThat(Tenants.isIgnored()).isFalse();
        assertThat(Tenants.currentTenant()).isEqualTo("t1");
    }

    /**
     * 验证嵌套跳过按层恢复。
     */
    @Test
    void shouldRestoreNestedIgnore() {
        Tenants.setProvider(() -> "t1");

        Tenants.callIgnored(() -> {
            Tenants.ignore();
            assertThat(Tenants.isIgnored()).isTrue();

            Tenants.restore();
            assertThat(Tenants.isIgnored()).isTrue();

            return null;
        });

        assertThat(Tenants.isIgnored()).isFalse();
    }

    /**
     * 验证未跳过时调用恢复会失败。
     */
    @Test
    void shouldRejectRestoreWithoutIgnore() {
        assertThatThrownBy(Tenants::restore)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ignored");
    }

    /**
     * 验证租户提供者返回的值会去除首尾空格。
     */
    @Test
    void shouldResolveCurrentTenant() {
        Tenants.setProvider(() -> " t1 ");

        assertThat(Tenants.currentTenant()).isEqualTo("t1");
        assertThat(Tenants.isEnabled()).isTrue();
    }

    /**
     * 验证作用域固定租户优先于提供者。
     */
    @Test
    void shouldPreferScopedFixedTenant() {
        Tenants.setProvider(() -> "t1");
        Tenants.setTenant("archive", "t2");

        String tenant = JpaPlusScope.call(
                "archive",
                Tenants::currentTenant
        );

        assertThat(tenant).isEqualTo("t2");
        assertThat(Tenants.currentTenant()).isEqualTo("t1");
    }

    /**
     * 验证追加租户条件。
     */
    @Test
    void shouldAppendTenantPredicate() {
        Tenants.setProvider(() -> "t1");

        Root<Order> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        Predicate expected = mock(Predicate.class);

        when(root.get("tenantId")).thenReturn(path);
        when(cb.equal(path, "t1")).thenReturn(expected);

        Predicate predicate = Tenants.<Order>filter(Order.class)
                .toPredicate(root, query, cb);

        assertThat(predicate).isSameAs(expected);
    }

    /**
     * 验证未标注租户字段的实体不受影响。
     */
    @Test
    void shouldSkipEntityWithoutTenantField() {
        Tenants.setProvider(() -> "t1");

        Specification<Plain> business = (r, q, c) -> mock(Predicate.class);

        assertThat(Tenants.filter(Plain.class, business))
                .isSameAs(business);
    }

    /**
     * 验证关闭租户过滤后返回原条件。
     */
    @Test
    void shouldSkipWhenDisabled() {
        Tenants.setProvider(() -> "t1");
        Tenants.setEnabled(false);

        Specification<Order> business = (r, q, c) -> mock(Predicate.class);

        assertThat(Tenants.filter(Order.class, business))
                .isSameAs(business);
        assertThat(Tenants.currentTenant()).isNull();
    }

    /**
     * 验证监听器在插入前填充租户字段。
     */
    @Test
    void shouldFillTenantOnPersist() {
        Tenants.setProvider(() -> "t9");

        Order order = new Order();

        new TenantListener().prePersist(order);

        assertThat(order.tenantId).isEqualTo("t9");
    }

    /**
     * 验证字段类型不是字符串时按数字转换。
     */
    @Test
    void shouldConvertNumericTenant() {
        Tenants.setProvider(() -> "42");

        NumberOrder order = new NumberOrder();

        new TenantListener().prePersist(order);

        assertThat(order.tenantId).isEqualTo(42L);
    }

    /**
     * 验证非法租户字段类型被拒绝。
     */
    @Test
    void shouldRejectUnsupportedTenantFieldType() {
        assertThatThrownBy(() -> Tenants.isTenantEntity(Invalid.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@TenantId field must be");
    }

    /**
     * 验证未配置提供者时不写租户字段。
     */
    @Test
    void shouldNotFillWithoutProvider() {
        Order order = new Order();

        new TenantListener().prePersist(order);

        assertThat(order.tenantId).isNull();
    }

    /** 带字符串租户字段的实体。 */
    private static class Order {

        /** 租户标识。 */
        @TenantId
        private String tenantId;
    }

    /** 带数字租户字段的实体。 */
    private static class NumberOrder {

        /** 租户标识。 */
        @TenantId
        private Long tenantId;
    }

    /** 不带租户字段的实体。 */
    private static class Plain {

        /** 主键。 */
        private Long id;

        /**
         * 获取主键。
         *
         * @return 主键
         */
        Long id() {
            return id;
        }
    }

    /** 租户字段类型非法的实体。 */
    private static class Invalid {

        /** 非法类型的租户字段。 */
        @TenantId
        private Double tenantId;
    }
}
