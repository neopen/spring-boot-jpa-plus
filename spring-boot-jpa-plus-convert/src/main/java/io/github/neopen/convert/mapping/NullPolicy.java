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
package io.github.neopen.convert.mapping;

/**
 * 空值映射策略。
 *
 * <p>用于控制参数中为 {@code null} 的字段如何处理。默认
 * {@link #IGNORE} 保留实体原值，适合部分更新；{@link #SET_NULL}
 * 会把实体字段清空，适合整体覆盖场景。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public enum NullPolicy {

    /**
     * 忽略空值，保留实体原值。
     */
    IGNORE,

    /**
     * 将空值写入实体，清空原值。
     */
    SET_NULL
}
