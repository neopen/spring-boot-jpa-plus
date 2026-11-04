package io.github.neopen.extension.response;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JSON 分页响应转换器。
 *
 * <p>
 * 职责：
 * 1. 将 VO 列表包装为 Json 分页响应；
 * 2. 将 Page<VO> 包装为 Json 分页响应；
 * 3. 提供空分页响应。
 *
 * <p>
 * 不负责：
 * 1. Entity 转 VO；
 * 2. 数据库查询；
 * 3. Specification 构建；
 * 4. HTTP 请求处理。
 *
 * <p>
 * 通过 JsonPageResponseFactory 扩展响应创建规则。
 */
public class JsonPageConverter {

    private final JsonPageResponseFactory responseFactory;

    public JsonPageConverter(
            JsonPageResponseFactory responseFactory) {

        this.responseFactory = Objects.requireNonNull(
                responseFactory,
                "responseFactory 不能为空"
        );
    }

    /**
     * VO 列表包装为分页响应。
     *
     * @param data  VO 列表
     * @param total 总记录数
     * @param <V>   VO 类型
     * @return JSON 分页响应
     */
    public <V> Json<List<V>> convert(
            List<V> data,
            long total) {

        if (total < 0) {
            throw new IllegalArgumentException(
                    "分页总记录数不能小于 0"
            );
        }

        return responseFactory.create(
                data == null ? Collections.emptyList() : data,
                total
        );
    }

    /**
     * VO 列表包装为分页响应。
     *
     * @param data VO 列表
     * @param <V>  VO 类型
     * @return JSON 分页响应
     */
    public <V> Json<List<V>> convert(
            List<V> data) {

        return responseFactory.create(
                data == null ? Collections.emptyList() : data,
                0
        );
    }

    /**
     * Page<VO> 包装为分页响应。
     *
     * @param page VO 分页对象
     * @param <V>  VO 类型
     * @return JSON 分页响应
     */
    public <V> Json<List<V>> convert(Page<V> page) {

        if (page == null) {
            return empty();
        }

        return convert(
                page.getContent(),
                page.getTotalElements()
        );
    }

    /**
     * 创建空分页响应。
     *
     * @param <V> VO 类型
     * @return 空分页响应
     */
    public <V> Json<List<V>> empty() {

        return convert(
                Collections.emptyList(),
                0L
        );
    }
}