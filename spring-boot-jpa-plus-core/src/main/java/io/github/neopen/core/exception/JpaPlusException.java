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
package io.github.neopen.core.exception;

/**
 * JPA Plus 基础异常。
 *
 * <p>表示框架无法按预期继续执行的领域异常，典型场景为实体元数据非法、
 * 类型转换失败或反射调用异常。</p>
 *
 * <p>约定：参数与配置错误使用 {@link IllegalArgumentException}，
 * 运行状态不满足使用 {@link IllegalStateException}，不要用本异常替代二者。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class JpaPlusException extends RuntimeException {

    /**
     * 使用异常信息创建异常。
     *
     * @param message 异常信息
     */
    public JpaPlusException(String message) {
        super(message);
    }

    /**
     * 使用异常信息和根本原因创建异常。
     *
     * @param message 异常信息
     * @param cause   根本原因
     */
    public JpaPlusException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 使用根本原因创建异常。
     *
     * @param cause 根本原因
     */
    public JpaPlusException(Throwable cause) {
        super(cause);
    }
}
