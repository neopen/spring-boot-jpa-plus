package io.github.neopen.core.persistence.logicdelete;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 逻辑删除标记注解。
 *
 * <p>标注在实体的 {@link Boolean} 或 {@code boolean} 字段上，字段值为
 * {@code false} 表示数据未删除。通过 {@link LogicDeletes} 或
 * {@code SpecWrapper#filterDeleted()} 可以统一追加过滤条件。</p>
 *
 * <p>该注解只对 JPA Criteria 查询生效，原生 SQL 查询需要自行处理。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogicDelete {
}
