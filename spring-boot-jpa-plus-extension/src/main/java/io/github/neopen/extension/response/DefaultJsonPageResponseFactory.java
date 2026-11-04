package io.github.neopen.extension.response;


import java.util.List;

/**
 * 默认 JSON 分页响应工厂。
 *
 * <p>
 * 使用项目原有 Json 作为统一响应结构。
 */
public class DefaultJsonPageResponseFactory
        implements JsonPageResponseFactory {

    @Override
    public <V> Json<List<V>> create(
            List<V> data,
            long total) {

        if (total < 0) {
            throw new IllegalArgumentException(
                    "分页总记录数不能小于 0"
            );
        }

        return Json.getSucc(data, total);
    }
}