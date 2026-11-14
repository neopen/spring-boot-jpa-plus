package io.github.neopen.core.persistence.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 审计字段填充单元测试。
 */
class AuditsTest {

    /** 固定的第一时间。 */
    private static final Instant FIRST =
            Instant.parse("2026-01-01T00:00:00Z");

    /** 固定的第二时间。 */
    private static final Instant SECOND =
            Instant.parse("2026-02-01T00:00:00Z");

    /**
     * 恢复默认配置，避免测试间互相影响。
     */
    @AfterEach
    void reset() {
        Audits.setClock(null);
        Audits.setAuditorSupplier(null);
    }

    /**
     * 验证插入时填充全部审计字段。
     */
    @Test
    void shouldFillAllFieldsOnCreate() {
        Audits.setClock(Clock.fixed(FIRST, ZoneOffset.UTC));
        Audits.setAuditorSupplier(() -> "creator");

        AuditedEntity entity = new AuditedEntity();

        new AuditListener().prePersist(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(FIRST);
        assertThat(entity.getUpdatedAt()).isEqualTo(FIRST);
        assertThat(entity.getCreatedBy()).isEqualTo("creator");
        assertThat(entity.getUpdatedBy()).isEqualTo("creator");
    }

    /**
     * 验证更新时只覆盖更新字段。
     */
    @Test
    void shouldOnlyTouchUpdateFieldsOnUpdate() {
        Audits.setClock(Clock.fixed(FIRST, ZoneOffset.UTC));
        Audits.setAuditorSupplier(() -> "creator");

        AuditedEntity entity = new AuditedEntity();
        AuditListener listener = new AuditListener();

        listener.prePersist(entity);

        Audits.setClock(Clock.fixed(SECOND, ZoneOffset.UTC));
        Audits.setAuditorSupplier(() -> "updater");

        listener.preUpdate(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(FIRST);
        assertThat(entity.getCreatedBy()).isEqualTo("creator");
        assertThat(entity.getUpdatedAt()).isEqualTo(SECOND);
        assertThat(entity.getUpdatedBy()).isEqualTo("updater");
    }

    /**
     * 验证未配置操作人时保留原值。
     */
    @Test
    void shouldSkipAuditorWhenSupplierReturnsNull() {
        Audits.setClock(Clock.fixed(FIRST, ZoneOffset.UTC));

        AuditedEntity entity = new AuditedEntity();
        entity.setCreatedBy("existing");

        new AuditListener().prePersist(entity);

        assertThat(entity.getCreatedBy()).isEqualTo("existing");
        assertThat(entity.getUpdatedBy()).isNull();
    }

    /**
     * 验证毫秒时间戳与 LocalDateTime 字段。
     */
    @Test
    void shouldFillTimestampAndLocalDateTimeFields() {
        Audits.setClock(Clock.fixed(FIRST, ZoneOffset.UTC));

        TimeEntity entity = new TimeEntity();

        new AuditListener().prePersist(entity);

        assertThat(entity.getCreatedTime()).isEqualTo(FIRST.toEpochMilli());
        assertThat(entity.getUpdatedTime())
                .isEqualTo(LocalDateTime.ofInstant(FIRST, ZoneOffset.UTC));
    }

    /**
     * 验证未声明审计字段的实体不参与填充。
     */
    @Test
    void shouldIgnoreEntityWithoutAuditFields() {
        assertThat(Audits.isAudited(String.class)).isFalse();
        assertThat(Audits.isAudited(AuditedEntity.class)).isTrue();
    }

    /**
     * 验证非法时间字段类型快速失败。
     */
    @Test
    void shouldRejectUnsupportedTimeType() {
        assertThatThrownBy(() -> Audits.isAudited(InvalidTimeEntity.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported audit time type");
    }

    /**
     * 验证操作人字段类型必须是字符串。
     */
    @Test
    void shouldRejectNonStringAuditorField() {
        assertThatThrownBy(() -> Audits.isAudited(InvalidAuditorEntity.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be String");
    }

    /** 审计实体。 */
    public static class AuditedEntity {

        /** 创建时间。 */
        @CreatedAt
        private Instant createdAt;

        /** 更新时间。 */
        @UpdatedAt
        private Instant updatedAt;

        /** 创建人。 */
        @CreatedBy
        private String createdBy;

        /** 更新人。 */
        @UpdatedBy
        private String updatedBy;

        public Instant getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Instant createdAt) {
            this.createdAt = createdAt;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getUpdatedBy() {
            return updatedBy;
        }

        public void setUpdatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
        }
    }

    /** 时间类型实体。 */
    public static class TimeEntity {

        /** 创建时间戳。 */
        @CreatedAt
        private long createdTime;

        /** 更新时间。 */
        @UpdatedAt
        private LocalDateTime updatedTime;

        public long getCreatedTime() {
            return createdTime;
        }

        public LocalDateTime getUpdatedTime() {
            return updatedTime;
        }
    }

    /** 时间字段类型非法的实体。 */
    public static class InvalidTimeEntity {

        /** 非法创建时间。 */
        @CreatedAt
        private String createdAt;
    }

    /** 操作人字段类型非法的实体。 */
    public static class InvalidAuditorEntity {

        /** 非法创建人。 */
        @CreatedBy
        private Integer createdBy;
    }
}
