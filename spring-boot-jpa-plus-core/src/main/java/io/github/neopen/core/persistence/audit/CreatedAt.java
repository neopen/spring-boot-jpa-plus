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
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreatedAt {
}
