package io.github.neopen.extension.desensitize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 基于 Jackson 的脱敏序列化器。
 *
 * <p>把标注 {@link io.github.neopen.extension.annotation.Desensitize}
 * 的字符串字段按标注类型脱敏后输出。通常不直接使用，交给
 * {@link DesensitizeModule} 自动挂载即可。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class DesensitizeJsonSerializer extends JsonSerializer<Object> {

    /** 脱敏类型。 */
    private final DesensitizeType type;

    /**
     * 创建脱敏序列化器。
     *
     * @param type 脱敏类型
     */
    public DesensitizeJsonSerializer(DesensitizeType type) {
        this.type = type;
    }

    /**
     * 输出脱敏后的字符串。
     *
     * @param value       原始值
     * @param generator   JSON 输出器
     * @param serializers 序列化上下文
     * @throws IOException 写出失败时抛出
     */
    @Override
    public void serialize(
            Object value,
            JsonGenerator generator,
            SerializerProvider serializers
    ) throws IOException {
        generator.writeString(
                value == null
                        ? null
                        : Desensitizes.mask(value.toString(), type)
        );
    }
}
