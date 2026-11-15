package io.github.neopen.convert.converter;

/**
 * 允许抛出指定受检异常的消费者。
 *
 * @param <T> 输入参数类型
 * @param <E> 可抛出的异常类型
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
    /**
     * 消费输入参数。
     *
     * @param t 输入参数
     * @throws E 处理过程中发生的异常
     */
	void acceptException(T t) throws E;
}