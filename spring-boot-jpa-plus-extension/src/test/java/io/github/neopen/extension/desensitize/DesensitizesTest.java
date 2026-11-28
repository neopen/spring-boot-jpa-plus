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

import com.alibaba.fastjson2.JSON;
import io.github.neopen.extension.annotation.Desensitize;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Desensitizes}、{@link DesensitizeType} 与
 * {@link DesensitizeValueFilter} 单元测试。
 */
class DesensitizesTest {

    /**
     * 验证各脱敏类型的掩码规则。
     */
    @Test
    void shouldMaskByType() {
        assertThat(DesensitizeType.NAME.mask("张三丰")).isEqualTo("张**");
        assertThat(DesensitizeType.NAME.mask("张")).isEqualTo("张");
        assertThat(DesensitizeType.PHONE.mask("13800001234"))
                .isEqualTo("138****1234");
        assertThat(DesensitizeType.ID_CARD.mask("110101199001011234"))
                .isEqualTo("110101********1234");
        assertThat(DesensitizeType.BANK_CARD.mask("6222021234567890"))
                .isEqualTo("6222****7890");
        assertThat(DesensitizeType.EMAIL.mask("alice@example.com"))
                .isEqualTo("a***@example.com");
        assertThat(DesensitizeType.ADDRESS.mask("北京市朝阳区建国路88号"))
                .isEqualTo("北京市朝阳区****");
        assertThat(DesensitizeType.PASSWORD.mask("p@ssw0rd"))
                .isEqualTo("******");
    }

    /**
     * 验证空值与异常长度输入。
     */
    @Test
    void shouldHandleEdgeValues() {
        assertThat(DesensitizeType.PHONE.mask(null)).isNull();
        assertThat(DesensitizeType.PHONE.mask("")).isEmpty();
        assertThat(DesensitizeType.PHONE.mask("123")).isEqualTo("****");
        assertThat(DesensitizeType.EMAIL.mask("invalid"))
                .isEqualTo("****");
        assertThat(Desensitizes.mask("13800001234", null))
                .isEqualTo("13800001234");
    }

    /**
     * 验证对象脱敏覆盖父类字段并保留未标注字段。
     */
    @Test
    void shouldApplyToObjectIncludingInheritedFields() {
        UserView view = new UserView();
        view.setName("张三丰");
        view.setPhone("13800001234");
        view.setEmail("alice@example.com");
        view.setRemark("普通备注");

        UserView result = Desensitizes.apply(view);

        assertThat(result).isSameAs(view);
        assertThat(view.getName()).isEqualTo("张**");
        assertThat(view.getPhone()).isEqualTo("138****1234");
        assertThat(view.getEmail()).isEqualTo("a***@example.com");
        assertThat(view.getRemark()).isEqualTo("普通备注");
        assertThat(Desensitizes.<UserView>apply(null)).isNull();
    }

    /**
     * 验证字段类型查询。
     */
    @Test
    void shouldResolveFieldType() {
        assertThat(Desensitizes.typeOf(UserView.class, "phone"))
                .isEqualTo(DesensitizeType.PHONE);
        assertThat(Desensitizes.typeOf(UserView.class, "remark")).isNull();
        assertThat(Desensitizes.typeOf(null, "phone")).isNull();
    }

    /**
     * 验证 fastjson2 序列化时自动脱敏。
     */
    @Test
    void shouldMaskWhenSerializingJson() {
        UserView view = new UserView();
        view.setName("张三丰");
        view.setPhone("13800001234");
        view.setEmail("alice@example.com");
        view.setRemark("普通备注");

        String json = JSON.toJSONString(
                view,
                new DesensitizeValueFilter()
        );

        assertThat(json)
                .contains("\"name\":\"张**\"")
                .contains("\"phone\":\"138****1234\"")
                .contains("\"email\":\"a***@example.com\"")
                .contains("\"remark\":\"普通备注\"");
    }

    /** 联系人基类。 */
    static class Contact {

        /** 手机号。 */
        @Desensitize(type = DesensitizeType.PHONE)
        private String phone;

        /**
         * 获取手机号。
         *
         * @return 手机号
         */
        public String getPhone() {
            return phone;
        }

        /**
         * 设置手机号。
         *
         * @param phone 手机号
         */
        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    /** 用户视图。 */
    static class UserView extends Contact {

        /** 姓名。 */
        @Desensitize(type = DesensitizeType.NAME)
        private String name;

        /** 邮箱。 */
        @Desensitize(type = DesensitizeType.EMAIL)
        private String email;

        /** 备注，未标注注解。 */
        private String remark;

        /**
         * 获取姓名。
         *
         * @return 姓名
         */
        public String getName() {
            return name;
        }

        /**
         * 设置姓名。
         *
         * @param name 姓名
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取邮箱。
         *
         * @return 邮箱
         */
        public String getEmail() {
            return email;
        }

        /**
         * 设置邮箱。
         *
         * @param email 邮箱
         */
        public void setEmail(String email) {
            this.email = email;
        }

        /**
         * 获取备注。
         *
         * @return 备注
         */
        public String getRemark() {
            return remark;
        }

        /**
         * 设置备注。
         *
         * @param remark 备注
         */
        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
