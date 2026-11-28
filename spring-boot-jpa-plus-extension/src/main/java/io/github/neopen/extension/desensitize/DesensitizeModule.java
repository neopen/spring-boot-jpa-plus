/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.extension.desensitize;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import io.github.neopen.extension.annotation.Desensitize;

import java.util.List;

/**
 * Jackson 脱敏模块。
 *
 * <p>注册到 {@code ObjectMapper} 后，标注
 * {@link Desensitize} 的字符串字段在序列化时自动脱敏。Spring Boot
 * 应用引入扩展模块后会自动注册，也可以手动注册：</p>
 *
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * mapper.registerModule(new DesensitizeModule());
 * }</pre>
 *
 * <p>字段需要使用 {@link Desensitize} 标注，未标注字段保持原值。
 * 脱敏只影响 JSON 输出，不修改对象字段本身。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class DesensitizeModule extends SimpleModule {

    /** 模块名称。 */
    private static final String MODULE_NAME = "jpa-plus-desensitize";

    /**
     * 创建脱敏模块。
     */
    public DesensitizeModule() {
        super(MODULE_NAME);
        setSerializerModifier(new DesensitizeSerializerModifier());
    }

    /**
     * 把脱敏序列化器挂载到标注字段。
     */
    private static final class DesensitizeSerializerModifier
            extends BeanSerializerModifier {

        /**
         * 修改属性序列化器。
         *
         * @param config    序列化配置
         * @param beanDesc  描述信息
         * @param writers   属性写出器
         * @return 处理后的属性写出器
         */
        @Override
        public List<BeanPropertyWriter> changeProperties(
                SerializationConfig config,
                BeanDescription beanDesc,
                List<BeanPropertyWriter> writers
        ) {
            for (BeanPropertyWriter writer : writers) {
                if (!String.class.equals(
                        writer.getType().getRawClass())) {
                    continue;
                }

                Desensitize annotation = annotationOf(writer);

                if (annotation != null) {
                    writer.assignSerializer(
                            new DesensitizeJsonSerializer(
                                    annotation.type()
                            )
                    );
                }
            }

            return writers;
        }

        /**
         * 读取字段上的脱敏注解。
         *
         * @param writer 属性写出器
         * @return 脱敏注解；未标注时返回 {@code null}
         */
        private Desensitize annotationOf(BeanPropertyWriter writer) {
            if (writer.getMember() == null) {
                return null;
            }

            return writer.getMember().getAnnotation(Desensitize.class);
        }
    }
}
