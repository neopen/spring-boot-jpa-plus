package io.github.neopen.extension.response;

import java.util.List;

/**
 * JSON 分页响应工厂。
 *
 * <p>负责创建统一的分页响应对象。业务项目可实现此接口，自定义
 * 成功消息、响应编码、空数据响应及统一响应对象的扩展属性。</p>
 */
public interface JsonPageResponseFactory {

    /**
     * 创建分页成功响应。
     *
     * @param <V>   视图对象类型
     * @param data  当前页数据
     * @param total 总记录数，不能小于 0
     * @return JSON 分页成功响应
     */
    <V> Json<List<V>> create(
            List<V> data,
            long total
    );
}