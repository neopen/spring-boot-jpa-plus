package io.github.neopen.demo.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的 JPA Plus 组件检查器。
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