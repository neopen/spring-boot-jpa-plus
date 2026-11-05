package io.github.neopen.core.exception;

/**
 * JPA Plus 基础异常。
 *
 * <p>用于统一封装框架运行期间无法继续处理的异常。</p>
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
