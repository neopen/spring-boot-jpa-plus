package io.github.neopen.core.persistence.sql;

import java.util.List;

/**
 * SQL 运算符策略。
 *
 * <p>运算符负责声明参数数量，并将列、参数名渲染为 SQL 条件片段。
 * 业务可以自定义实现以扩展数据库特有语法。</p>
 */
public interface SqlOperator {

    /**
     * 获取运算符需要的参数数量。
     *
     * @return 参数数量
     */
    int parameterCount();

    /**
     * 渲染 SQL 条件片段。
     *
     * @param path           带表别名的列路径
     * @param parameterNames 已分配的命名参数
     * @return SQL 条件片段
     */
    String render(String path, List<String> parameterNames);
}
