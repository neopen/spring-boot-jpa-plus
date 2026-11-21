package io.github.neopen.extension.response;


import java.util.List;

/**
 * 默认 JSON 分页响应工厂。
 *
 * <p>使用项目统一的 {@link Json} 作为响应结构。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class DefaultJsonPageResponseFactory
        implements JsonPageResponseFactory {

    /**
     * 创建默认实例。
     */
    public DefaultJsonPageResponseFactory() {
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException 总记录数小于 0
     */
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