package io.github.neopen.extension.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 通用 JSON 响应对象。
 *
 * @param <V> 响应数据类型
 */
@Data
@NoArgsConstructor
@SuppressWarnings("all")
public class Json<V> {

    /** 总记录数，主要用于分页响应。 */
    private Long count;

    /** 响应数据。 */
    private V data;

    /** 请求是否成功。 */
    private boolean success;

    /** 业务响应编码。 */
    private Integer code;

    /** 响应消息。 */
    private String message;

    /**
     * 创建不含数据的成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Json<T> getSucc() {
        return getSucc(null);
    }

    /**
     * 创建包含数据的成功响应。
     *
     * @param <T>  响应数据类型
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> Json<T> getSucc(T data) {
        return getSucc(data, "成功");
    }

    /**
     * 创建列表数据成功响应。
     *
     * @param <T>  响应数据类型
     * @param data 响应数据；为 {@code null} 时使用空列表
     * @return 成功响应
     */
    public static <T> Json<T> getSuccs(T data) {
        return getSucc(data == null ? (T) new ArrayList<>(0) : data, "成功");
    }

    /**
     * 创建包含自定义消息的成功响应。
     *
     * @param <T>  响应数据类型
     * @param data 响应数据
     * @param msg  成功消息
     * @return 成功响应
     */
    public static <T> Json<T> getSucc(T data, String msg) {
        return getSucc(msg, 0, data, null);
    }

    /**
     * 创建包含总记录数的成功响应。
     *
     * @param <T>   响应数据类型
     * @param data  响应数据；为 {@code null} 时使用空列表
     * @param count 总记录数；为 {@code null} 时使用 0
     * @return 成功响应
     */
    public static <T> Json<T> getSucc(T data, Long count) {
        return getSucc("成功", 0, (data == null ? (T) new ArrayList<>(0) : data), count == null ? 0l : count);
    }

    /**
     * 创建完整字段的成功响应。
     *
     * @param <T>   响应数据类型
     * @param msg   成功消息
     * @param code  业务响应编码
     * @param data  响应数据
     * @param count 总记录数
     * @return 成功响应
     */
    public static <T> Json<T> getSucc(String msg, Integer code, T data, Long count) {
        Json<T> resultData = new Json<T>();
        resultData.setCode(code);
        resultData.setMessage(msg);
        resultData.setData(data);
        resultData.setCount(count);
        resultData.setSuccess(true);
        return resultData;
    }

    /**
     * 创建默认失败响应。
     *
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Json<T> getError() {
        return getError("失败");
    }

    /**
     * 创建包含自定义消息的失败响应。
     *
     * @param <T> 响应数据类型
     * @param msg 失败消息
     * @return 失败响应
     */
    public static <T> Json<T> getError(String msg) {
        return getError(msg, -1);
    }

    /**
     * 创建包含消息和编码的失败响应。
     *
     * @param <T>  响应数据类型
     * @param msg  失败消息
     * @param code 业务响应编码
     * @return 失败响应
     */
    public static <T> Json<T> getError(String msg, Integer code) {
        Json<T> resultData = new Json<T>();
        resultData.setCode(code);
        resultData.setMessage(msg);
        resultData.setSuccess(false);
        return resultData;
    }

    /**
     * 根据操作结果创建响应。
     *
     * @param <T>    响应数据类型
     * @param result 操作是否成功
     * @return 与操作结果对应的响应
     */
    public static <T> Json<T> getJson(boolean result) {
        Json<T> resultData = new Json<T>();
        resultData.setCode(result ? 0 : -1);
        resultData.setMessage(result ? "成功" : "操作失败");
        resultData.setSuccess(result);
        return resultData;
    }

    /**
     * 为响应数据提供默认值。
     *
     * @param <V> 数据类型
     * @param v   原始数据
     * @return 原始数据或对应的默认值
     */
    private static <V> V setDefaultV(V v) {
        return v == null ? sayClass(v) : v;
    }

    /**
     * 根据输入类型返回空值占位对象。
     *
     * <p>集合返回空列表，映射返回空映射，其他情况返回空字符串。</p>
     *
     * @param <V> 数据类型
     * @param v   原始数据
     * @return 与输入类型对应的空值占位对象
     */
    @SuppressWarnings("unchecked")
    public static <V> V sayClass(V v) {
        if (v == null)
            v = (V) new String();
        else if (v instanceof Collection<?>) {
            v = (V) new ArrayList<>(0);
        } else if (v instanceof Map<?, ?>) {
            v = (V) new HashMap<>(0);
        } else if (v instanceof Set<?>) {
            v = (V) new HashSet<>(0);
        } else
            v = (V) new String();

        return v;
    }

}
