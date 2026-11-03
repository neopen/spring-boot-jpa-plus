package io.github.neopen.jpa.annotation;

import java.lang.annotation.*;

/**
 * <b> 日期格式字符串
 *
 * @author He.hp
 * @date 2020年9月11日 下午2:10:07
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Documented
public @interface DateStrValid {

}
