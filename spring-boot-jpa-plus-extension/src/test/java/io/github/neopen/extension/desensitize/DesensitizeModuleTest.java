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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.neopen.extension.annotation.Desensitize;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DesensitizeModule} 单元测试。
 */
class DesensitizeModuleTest {

    /**
     * 验证 Jackson 序列化时对标注字段脱敏。
     *
     * @throws Exception JSON 序列化失败
     */
    @Test
    void shouldMaskAnnotatedFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new DesensitizeModule());

        UserView view = new UserView();
        view.name = "张三丰";
        view.phone = "13800001234";
        view.email = "alice@example.com";
        view.remark = "普通备注";

        String json = mapper.writeValueAsString(view);

        assertThat(json).contains("\"name\":\"张**\"");
        assertThat(json).contains("\"phone\":\"138****1234\"");
        assertThat(json).contains("\"email\":\"a***@example.com\"");
        assertThat(json).contains("\"remark\":\"普通备注\"");
    }

    /**
     * 验证未注册模块时输出原始值。
     *
     * @throws Exception JSON 序列化失败
     */
    @Test
    void shouldKeepOriginalWithoutModule() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        UserView view = new UserView();
        view.name = "张三丰";
        view.phone = "13800001234";

        String json = mapper.writeValueAsString(view);

        assertThat(json).contains("\"name\":\"张三丰\"");
        assertThat(json).contains("\"phone\":\"13800001234\"");
    }

    /**
     * 验证空值字段不会报错。
     *
     * @throws Exception JSON 序列化失败
     */
    @Test
    void shouldHandleNullFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new DesensitizeModule());

        String json = mapper.writeValueAsString(new UserView());

        assertThat(json).contains("\"name\":null");
        assertThat(json).contains("\"phone\":null");
    }

    /**
     * 测试用视图对象。
     */
    static class UserView {

        /** 姓名。 */
        @Desensitize(type = DesensitizeType.NAME)
        public String name;

        /** 手机号。 */
        @Desensitize(type = DesensitizeType.PHONE)
        public String phone;

        /** 邮箱。 */
        @Desensitize(type = DesensitizeType.EMAIL)
        public String email;

        /** 备注。 */
        public String remark;
    }
}
