package io.github.neopen.demo.controller;

import io.github.neopen.core.exception.JpaPlusException;
import io.github.neopen.extension.response.Json;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 参数映射示例异常处理。
 *
 * <p>把日期校验等切面抛出的 {@link JpaPlusException} 转换为统一的
 * JSON 错误响应，避免示例接口直接返回 500。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@RestControllerAdvice(assignableTypes = ConvertDemoController.class)
public class ConvertDemoExceptionHandler {

    /**
     * 处理参数映射相关异常。
     *
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(JpaPlusException.class)
    public Json<Void> handle(JpaPlusException ex) {
        return Json.getError(ex.getMessage());
    }
}
