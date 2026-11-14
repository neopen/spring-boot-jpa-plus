package io.github.neopen.core.persistence.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记创建时间字段。
 *
 * <p>实体插入时由 {@link AuditListener} 自动写入当前时间。支持
 * {@link java.time.Instant}、{@code LocalDateTime}、{@code LocalDate}、
 * {@code OffsetDateTime}、{@code ZonedDateTime}、{@link java.util.Date}
 * 以及 {@link Long} / {@code long}（毫秒时间戳）。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreatedAt {
}
