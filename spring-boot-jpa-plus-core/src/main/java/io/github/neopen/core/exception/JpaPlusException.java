package io.github.neopen.core.exception;

/**
 * JPA Plus 基础异常
 */
public class JpaPlusException extends RuntimeException {

    public JpaPlusException(String message) {
        super(message);
    }

    public JpaPlusException(String message, Throwable cause) {
        super(message, cause);
    }

    public JpaPlusException(Throwable cause) {
        super(cause);
    }
}
