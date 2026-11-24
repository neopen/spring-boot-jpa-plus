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
package io.github.neopen.extension.exception;

/**
 * 触发限流时抛出的异常。
 *
 * <p>业务代码可以统一捕获该异常并转换为接口响应。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class RateLimitException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 创建限流异常。
     *
     * @param message 提示信息
     */
    public RateLimitException(String message) {
        super(message);
    }
}
