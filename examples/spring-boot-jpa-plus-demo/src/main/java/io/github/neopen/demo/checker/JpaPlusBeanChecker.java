package io.github.neopen.demo.checker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPlusBeanChecker implements ApplicationRunner {

    private final ApplicationContext applicationContext;

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