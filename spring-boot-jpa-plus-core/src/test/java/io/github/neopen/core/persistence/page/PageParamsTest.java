package io.github.neopen.core.persistence.page;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PageParams} 单元测试。
 */
class PageParamsTest {

    /**
     * 验证默认值与 1 基页码转换。
     */
    @Test
    void shouldUseDefaultsAndConvertToPageable() {
        PageParams params = PageParams.of(1);

        assertThat(params.getPage()).isEqualTo(1);
        assertThat(params.getSize()).isEqualTo(PageParams.DEFAULT_SIZE);
        assertThat(params.getOffset()).isZero();
        assertThat(params.isFirst()).isTrue();

        Pageable pageable = params.toPageable();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize())
                .isEqualTo(PageParams.DEFAULT_SIZE);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    /**
     * 验证页码、偏移量与每页数量。
     */
    @Test
    void shouldCalculateOffset() {
        PageParams params = PageParams.of(3, 20);

        assertThat(params.getOffset()).isEqualTo(40);
        assertThat(params.isFirst()).isFalse();
        assertThat(params.toPageable().getPageNumber()).isEqualTo(2);
    }

    /**
     * 验证按实体类型解析排序。
     */
    @Test
    void shouldParseSortByEntityType() {
        Pageable pageable = PageParams.builder()
                .page(1)
                .size(10)
                .sort("createTime,desc;id,asc")
                .entityType(Sample.class)
                .build()
                .toPageable();

        assertThat(pageable.getSort().getOrderFor("createTime"))
                .isNotNull()
                .satisfies(order ->
                        assertThat(order.isDescending()).isTrue());
        assertThat(pageable.getSort().getOrderFor("id"))
                .isNotNull()
                .satisfies(order ->
                        assertThat(order.isAscending()).isTrue());
    }

    /**
     * 验证别名映射与允许字段集合。
     */
    @Test
    void shouldParseSortWithAliasesAndAllowedFields() {
        Pageable aliased = PageParams.builder()
                .sort("name,desc")
                .entityType(Sample.class)
                .aliases(Map.of("name", "customerName"))
                .build()
                .toPageable();

        assertThat(aliased.getSort().getOrderFor("customerName"))
                .isNotNull();

        Pageable allowed = PageParams.builder()
                .sort("id,desc")
                .allowedFields("id", "createTime")
                .build()
                .toPageable();

        assertThat(allowed.getSort().getOrderFor("id")).isNotNull();
    }

    /**
     * 验证未配置校验依据时忽略排序，避免未校验字段进入 SQL。
     */
    @Test
    void shouldIgnoreSortWithoutValidationSource() {
        Pageable pageable = PageParams.builder()
                .sort("id,desc")
                .build()
                .toPageable();

        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    /**
     * 验证非法排序字段被拒绝。
     */
    @Test
    void shouldRejectUnknownSortField() {
        assertThatThrownBy(() -> PageParams.builder()
                .sort("notExists,desc")
                .entityType(Sample.class)
                .build()
                .toPageable())
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PageParams.builder()
                .sort("notExists,desc")
                .allowedFields(Set.of("id"))
                .build()
                .toPageable())
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证页码、每页数量与上限校验。
     */
    @Test
    void shouldValidatePageAndSize() {
        assertThatThrownBy(() -> PageParams.of(0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number");

        assertThatThrownBy(() -> PageParams.of(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size");

        assertThatThrownBy(() -> PageParams.of(1, 501))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed");

        assertThatThrownBy(() -> PageParams.builder()
                .maxSize(0)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Max page size");

        assertThat(PageParams.builder()
                .size(1000)
                .maxSize(2000)
                .build()
                .getSize())
                .isEqualTo(1000);
    }

    /** 排序字段校验用实体。 */
    private static final class Sample {

        /** 主键。 */
        private Long id;

        /** 创建时间。 */
        private String createTime;

        /** 客户名称。 */
        private String customerName;
    }
}
