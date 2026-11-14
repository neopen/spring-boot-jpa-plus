package io.github.neopen.convert.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略参数字段，不参与 param 到实体的转换。
 *
 * <p>适用于请求参数中存在、但不应写入实体的字段，
 * 例如客户端回传的标识、扩展信息或审计字段。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ParamIgnore {
}
