package io.github.neopen.extension.response;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JSON 分页响应转换器。
 *
 * <p>将视图对象列表或 Spring Data {@link Page} 包装为统一分页响应，
 * 并通过 {@link JsonPageResponseFactory} 扩展响应创建规则。</p>
 *
 * <p>本类不负责实体转换、数据库查询或 HTTP 请求处理。</p>
 */
public class JsonPageConverter {

    /** 实际创建响应对象的工厂。 */
    private final JsonPageResponseFactory responseFactory;

    /**
     * 创建分页响应转换器。
     *
     * @param responseFactory 分页响应工厂，不能为 {@code null}
     * @throws NullPointerException 响应工厂为 {@code null}
     */
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
     * @param <V>   视图对象类型
     * @param data  当前页数据；为 {@code null} 时按空列表处理
     * @param total 总记录数，不能小于 0
     * @return JSON 分页响应
     * @throws IllegalArgumentException 总记录数小于 0
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
     * @param <V>  视图对象类型
     * @param data 当前页数据；为 {@code null} 时按空列表处理
     * @return 总记录数为 0 的 JSON 分页响应
     */
    public <V> Json<List<V>> convert(
            List<V> data) {

        return responseFactory.create(
                data == null ? Collections.emptyList() : data,
                0
        );
    }

    /**
     * 将 Spring Data 分页对象包装为分页响应。
     *
     * @param <V>  视图对象类型
     * @param page 分页对象；为 {@code null} 时返回空分页响应
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
     * @param <V> 视图对象类型
     * @return 空分页响应
     */
    public <V> Json<List<V>> empty() {

        return convert(
                Collections.emptyList(),
                0L
        );
    }
}