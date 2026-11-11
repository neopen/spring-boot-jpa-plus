package io.github.neopen.core.persistence.hql;

import java.util.List;

/**
 * HQL 条件运算符。
 *
 * <p>实现类负责把字段路径和参数名渲染为 HQL 片段。内置运算符见
 * {@link StandardHqlOperator}，业务也可以通过实现该接口扩展
 * 自定义运算符。</p>
 */
public interface HqlOperator {

    /**
     * 获取运算符需要的参数数量。
     *
     * @return 参数数量
     */
    int parameterCount();

    /**
     * 渲染条件片段。
     *
     * @param path           字段路径，例如 {@code o.status}
     * @param parameterNames 按顺序分配的命名参数，数量等于
     *                       {@link #parameterCount()}
     * @return HQL 条件片段
     */
    String render(String path, List<String> parameterNames);
}
