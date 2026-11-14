package io.github.neopen.core.persistence.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记创建人字段。
 *
 * <p>实体插入时由 {@link AuditListener} 自动写入当前操作人。字段类型
 * 必须是 {@link String}，当前操作人由
 * {@link Audits#setAuditorSupplier(java.util.function.Supplier)} 提供；
 * 未配置时不会写入。</p>
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreatedBy {
}
