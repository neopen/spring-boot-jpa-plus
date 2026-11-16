package io.github.neopen.core.persistence.audit.trail;

import io.github.neopen.core.persistence.audit.Audits;
import io.github.neopen.core.scope.JpaPlusScope;
import jakarta.persistence.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AuditTrails} 单元测试。
 */
class AuditTrailsTest {

    /** 收集到的变更历史。 */
    private final List<AuditEntry> entries = new ArrayList<>();

    /** 监听器。 */
    private final AuditTrailListener listener = new AuditTrailListener();

    /**
     * 注册发布者与操作人。
     */
    @BeforeEach
    void setUp() {
        AuditTrails.setEnabled(true);
        AuditTrails.setPublisher(entries::add);
        Audits.setAuditorProvider(() -> "tester");
    }

    /**
     * 清理全局状态。
     */
    @AfterEach
    void reset() {
        AuditTrails.setEnabled(true);
        AuditTrails.setPublisher(null);
        AuditTrails.clearScope("archive");
        Audits.setAuditorProvider(null);
        JpaPlusScope.clear();
    }

    /**
     * 验证新增记录包含字段快照。
     */
    @Test
    void shouldPublishCreateEntry() {
        Product product = product(1L, "keyboard", "99.00");

        listener.postPersist(product);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).action()).isEqualTo(AuditAction.CREATE);
        assertThat(entries.get(0).entityId()).isEqualTo(1L);
        assertThat(entries.get(0).actor()).isEqualTo("tester");
        assertThat(entries.get(0).changes())
                .extracting(AuditChange::field)
                .contains("name", "price");
    }

    /**
     * 验证修改记录只包含变化字段。
     */
    @Test
    void shouldPublishUpdateEntryWithChangedFields() {
        Product product = product(1L, "keyboard", "99.00");
        listener.postLoad(product);

        product.setName("mouse");

        listener.preUpdate(product);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).action()).isEqualTo(AuditAction.UPDATE);
        assertThat(entries.get(0).changes())
                .containsExactly(new AuditChange("name", "keyboard", "mouse"));
    }

    /**
     * 验证未发生变化时不发布更新记录。
     */
    @Test
    void shouldSkipUpdateWithoutChanges() {
        Product product = product(1L, "keyboard", "99.00");
        listener.postLoad(product);

        listener.preUpdate(product);

        assertThat(entries).isEmpty();
    }

    /**
     * 验证删除记录包含删除前快照。
     */
    @Test
    void shouldPublishDeleteEntry() {
        Product product = product(2L, "screen", "199.00");
        listener.postLoad(product);

        listener.preRemove(product);
        listener.postRemove(product);

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).action()).isEqualTo(AuditAction.DELETE);
        assertThat(entries.get(0).changes())
                .extracting(AuditChange::field)
                .contains("name", "price");
    }

    /**
     * 验证关闭后不产生记录。
     */
    @Test
    void shouldSkipWhenDisabled() {
        AuditTrails.setEnabled(false);

        listener.postPersist(product(3L, "cable", "9.00"));

        assertThat(entries).isEmpty();
    }

    /**
     * 验证作用域关闭后不产生记录。
     */
    @Test
    void shouldSkipWhenScopedDisabled() {
        AuditTrails.setEnabled("archive", false);

        JpaPlusScope.run(
                "archive",
                () -> listener.postPersist(product(4L, "cable", "9.00"))
        );

        assertThat(entries).isEmpty();
    }

    /**
     * 验证未标注 {@link Audited} 的实体被忽略。
     */
    @Test
    void shouldIgnoreEntityWithoutAnnotated() {
        listener.postPersist(new Plain());

        assertThat(entries).isEmpty();
        assertThat(AuditTrails.isAudited(new Plain())).isFalse();
    }

    /**
     * 验证没有发布者时不会抛出异常。
     */
    @Test
    void shouldIgnoreWithoutPublisher() {
        AuditTrails.setPublisher(null);

        listener.postPersist(product(5L, "cable", "9.00"));

        assertThat(entries).isEmpty();
    }

    /**
     * 创建测试商品。
     *
     * @param id    主键
     * @param name  名称
     * @param price 价格
     * @return 商品
     */
    private Product product(Long id, String name, String price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));

        return product;
    }

    /** 需要记录变更历史的商品。 */
    @Audited
    private static class Product {

        /** 主键。 */
        @Id
        private Long id;

        /** 名称。 */
        private String name;

        /** 价格。 */
        private BigDecimal price;

        /**
         * 获取主键。
         *
         * @return 主键
         */
        Long getId() {
            return id;
        }

        /**
         * 设置主键。
         *
         * @param id 主键
         */
        void setId(Long id) {
            this.id = id;
        }

        /**
         * 获取名称。
         *
         * @return 名称
         */
        String getName() {
            return name;
        }

        /**
         * 设置名称。
         *
         * @param name 名称
         */
        void setName(String name) {
            this.name = name;
        }

        /**
         * 设置价格。
         *
         * @param price 价格
         */
        void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

    /** 未标注注解的普通实体。 */
    private static class Plain {
    }
}
