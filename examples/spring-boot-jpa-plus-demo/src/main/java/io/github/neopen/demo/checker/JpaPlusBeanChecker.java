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
package io.github.neopen.demo.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的 JPA Plus 组件检查器。
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Component
@RequiredArgsConstructor
public class JpaPlusBeanChecker implements ApplicationRunner {

    /** Spring 应用上下文。 */
    private final ApplicationContext applicationContext;

    /**
     * 检查分页响应相关 Bean 是否注册。
     *
     * @param args 应用启动参数
     */
    @Override
    public void run(ApplicationArguments args) {

        System.out.println("========== JPA Plus Bean 检查 ==========");

        System.out.println(
                "JsonPageConverter: " +
                applicationContext.containsBean("jsonPageConverter")
        );

        System.out.println(
                "JsonPageResponseFactory: " +
                applicationContext.containsBean("jsonPageResponseFactory")
        );
    }
}