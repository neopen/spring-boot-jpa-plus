package io.github.neopen.extension.annotation;

import io.github.neopen.extension.desensitize.DesensitizeType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要脱敏输出或处理的字符串字段。
 *
 * <p>可通过 {@link io.github.neopen.extension.desensitize.Desensitizes#apply(Object)}
 * 就地脱敏对象，也可以把 {@link io.github.neopen.extension.desensitize.DesensitizeValueFilter}
 * 交给 fastjson2，在序列化 JSON 时自动脱敏。</p>
 *
 * <pre>{@code
 * public class UserView {
 *
 *     @Desensitize(DesensitizeType.NAME)
 *     private String name;
 *
 *     @Desensitize(DesensitizeType.PHONE)
 *     private String phone;
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Desensitize {

    /**
     * 脱敏类型。
     *
     * @return 脱敏类型
     */
    DesensitizeType type();
}
