package io.github.neopen.extension.desensitize;

import com.alibaba.fastjson2.filter.ValueFilter;
import io.github.neopen.extension.annotation.Desensitize;

/**
 * fastjson2 脱敏值过滤器。
 *
 * <p>序列化时自动对标注 {@link Desensitize} 的字符串字段脱敏：</p>
 *
 * <pre>{@code
 * String json = JSON.toJSONString(
 *         userView,
 *         new DesensitizeValueFilter()
 * );
 * }</pre>
 *
 * <p>过滤器只处理字符串字段，未标注注解的字段原样输出。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class DesensitizeValueFilter implements ValueFilter {

    /**
     * 创建默认实例。
     */
    public DesensitizeValueFilter() {
    }

    /**
     * 处理单个字段值。
     *
     * @param object 当前对象，可能为 {@code null}
     * @param name   字段名
     * @param value  字段值
     * @return 脱敏后的值；无匹配规则时返回原值
     */
    @Override
    public Object apply(Object object, String name, Object value) {
        if (object == null
                || name == null
                || !(value instanceof String text)
                || text.isEmpty()) {
            return value;
        }

        DesensitizeType type = Desensitizes.typeOf(
                object.getClass(),
                name
        );

        return type == null ? value : type.mask(text);
    }
}
