package io.github.neopen.extension.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * Json
 *
 * @author he.hp
 * @version 1.0
 * @date 2019年5月22日 上午9:41:19
 * @description 返回的json对象（swagger输出属性包含注释）
 */
@Data
//@JsonInclude(JsonInclude.Include.ALWAYS)
//@ApiModel("返回的json数据类")
@NoArgsConstructor
@SuppressWarnings("all")
public class Json<V> {

    //    @ApiModelProperty(value = "总记录数")
    private long count;

    //    @ApiModelProperty(value = "实体对象或对象结果集")
    private V data;

    //    @ApiModelProperty(value = "成功标记")
    private boolean success;

    //    @ApiModelProperty(value = "错误编码")
    private Integer code;
    //    @ApiModelProperty(value = "错误信息")
    private String message;

    public static <T> Json<T> getSucc() {
        return getSucc(null);
    }

    public static <T> Json<T> getSucc(T data) {
        return getSucc(data, "成功");
    }

    /**
     * <b> 列表结果
     * <p>
     *
     * @param <T>
     * @param data
     * @return
     * @author He.hp
     * @date 2020年9月17日 下午3:30:33
     */
    public static <T> Json<T> getSuccs(T data) {
        return getSucc(data == null ? (T) new ArrayList<>(0) : data, "成功");
    }

    public static <T> Json<T> getSucc(T data, String msg) {
        return getSucc(msg, 0, data, null);
    }

    public static <T> Json<T> getSucc(T data, Long count) {
        return getSucc("成功", 0, (data == null ? (T) new ArrayList<>(0) : data), count == null ? 0l : count);
    }

    /**
     * <b> 成功结果
     * <p>
     *
     * @param <T>
     * @return
     * @author He.hp
     * @date 2020年5月18日 下午3:21:00
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

    public static <T> Json<T> getError() {
        return getError("失败");
    }

    public static <T> Json<T> getError(String msg) {
        return getError(msg, -1);
    }

    /**
     * <b> 错误结果
     * <p>
     *
     * @param <T>
     * @return
     * @author He.hp
     * @date 2020年5月18日 下午3:20:37
     */
    public static <T> Json<T> getError(String msg, Integer code) {
        Json<T> resultData = new Json<T>();
        resultData.setCode(code);
        resultData.setMessage(msg);
        resultData.setSuccess(false);
        return resultData;
    }

    /**
     * <b> 返回操作结果
     * <P> 如，增删改
     *
     * @param result
     * @return com.citycloud.sanitation.common.bean.model.Json<T>
     * @Author He.hp
     * @Date 2021/4/2 15:20
     */
    public static <T> Json<T> getJson(boolean result) {
        Json<T> resultData = new Json<T>();
        resultData.setCode(result ? 0 : -1);
        resultData.setMessage(result ? "成功" : "操作失败");
        resultData.setSuccess(result);
        return resultData;
    }

    /**
     * @param v
     * @return
     * @author He.hp
     * @Description data设置默认值（防止序列化该属性）
     * @date 2019年7月5日 下午5:45:05
     */
    private static <V> V setDefaultV(V v) {
        return v == null ? sayClass(v) : v;
    }

    /**
     * <b> 类型判定
     * <p>
     *
     * @param <V>
     * @param v
     * @return
     * @author He.hp
     * @date 2020年7月22日 下午7:36:12
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
