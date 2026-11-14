package io.github.neopen.extension.exception;

/**
 * 触发限流时抛出的异常。
 *
 * <p>业务代码可以统一捕获该异常并转换为接口响应。</p>
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
