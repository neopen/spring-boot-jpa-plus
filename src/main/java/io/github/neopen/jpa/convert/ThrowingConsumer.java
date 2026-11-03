package io.github.neopen.jpa.convert;

/**
 * ThrowingConsumer
 * 
 * @author He.hp
 * @date 2019年4月2日 下午2:14:48
 * @version 1.0
 * @description 》》 抛出异常的方法
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Exception> {
	/**
	 * @author He.hp
	 * @Description >>> 接收处理异常
	 * @date 2019年4月2日 下午2:28:19
	 * @param t
	 * @throws E
	 */
	void acceptException(T t) throws E;
}