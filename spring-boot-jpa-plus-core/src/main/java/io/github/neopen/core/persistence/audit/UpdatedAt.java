package io.github.neopen.core.persistence.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记更新时间字段。
 *
 * <p>实体插入和更新时由 {@link AuditListener} 自动写入当前时间。
 * 支持的类型与 {@link CreatedAt} 一致。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdatedAt {
}
