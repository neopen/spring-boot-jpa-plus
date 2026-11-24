/*
 * Copyright 2026 neopen
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

/**
 * 脱敏类型。
 *
 * <p>每种类型内置对应的掩码策略，可通过 {@link #mask(String)} 直接使用。
 * 长度不足或格式不符合预期时返回固定掩码，避免暴露原始内容。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public enum DesensitizeType {

    /** 姓名：保留首字符。 */
    NAME("姓名") {
        @Override
        protected String doMask(String value) {
            if (value.length() == 1) {
                return value;
            }

            return value.charAt(0) + "*".repeat(value.length() - 1);
        }
    },

    /** 手机号：保留前 3 位与后 4 位。 */
    PHONE("手机号") {
        @Override
        protected String doMask(String value) {
            if (value.length() < 7) {
                return "****";
            }

            return value.substring(0, 3)
                    + "****"
                    + value.substring(value.length() - 4);
        }
    },

    /** 身份证号：保留前 6 位与后 4 位。 */
    ID_CARD("身份证号") {
        @Override
        protected String doMask(String value) {
            if (value.length() < 10) {
                return "****";
            }

            return value.substring(0, 6)
                    + "********"
                    + value.substring(value.length() - 4);
        }
    },

    /** 银行卡号：保留前 4 位与后 4 位。 */
    BANK_CARD("银行卡号") {
        @Override
        protected String doMask(String value) {
            if (value.length() < 8) {
                return "****";
            }

            return value.substring(0, 4)
                    + "****"
                    + value.substring(value.length() - 4);
        }
    },

    /** 邮箱：保留首字符与域名。 */
    EMAIL("邮箱") {
        @Override
        protected String doMask(String value) {
            int at = value.indexOf('@');

            if (at <= 0) {
                return "****";
            }

            return value.charAt(0) + "***" + value.substring(at);
        }
    },

    /** 地址：保留前 6 个字符。 */
    ADDRESS("地址") {
        @Override
        protected String doMask(String value) {
            if (value.length() <= 6) {
                return "****";
            }

            return value.substring(0, 6) + "****";
        }
    },

    /** 密码：完全隐藏。 */
    PASSWORD("密码") {
        @Override
        protected String doMask(String value) {
            return "******";
        }
    };

    /** 类型说明。 */
    private final String description;

    /**
     * 创建脱敏类型。
     *
     * @param description 类型说明
     */
    DesensitizeType(String description) {
        this.description = description;
    }

    /**
     * 获取类型说明。
     *
     * @return 类型说明
     */
    public String getDescription() {
        return description;
    }

    /**
     * 对字符串脱敏。
     *
     * @param value 原始值，允许为 {@code null}
     * @return 脱敏后的值；入参为 {@code null} 或空串时原样返回
     */
    public String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return doMask(value);
    }

    /**
     * 执行具体脱敏策略。
     *
     * @param value 非空原始值
     * @return 脱敏后的值
     */
    protected abstract String doMask(String value);
}
