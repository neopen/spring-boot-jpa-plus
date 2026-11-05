package io.github.neopen.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot JPA Plus 示例应用入口。
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class SpringBootJpaPlusDemoApplication {

    /**
     * 启动示例应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootJpaPlusDemoApplication.class, args);
    }

}
