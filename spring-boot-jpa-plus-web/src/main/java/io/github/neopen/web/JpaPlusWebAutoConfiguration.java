package io.github.neopen.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@AutoConfiguration
@ConditionalOnClass({
//        org.aspectj.lang.ProceedingJoinPoint.class
})
public class JpaPlusWebAutoConfiguration {

    // 注册日期处理、日志处理相关 Bean
}