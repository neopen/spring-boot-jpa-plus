package io.github.neopen.core.persistence.sql;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 原生 SQL 查询构造器。
 *
 * <p>面向单表查询，通过链式调用构造 select、where、group by、order by，
 * 条件默认按 AND 组合，可通过 {@link #and(Consumer)} 和
 * {@link #or(Consumer)} 构建嵌套条件分组。</p>
 *
 * <p>等值、比较、模糊、集合和区间等条件的列名默认自动补全表别名；
 * {@code count(*)}、函数表达式等非简单列名保持原样。select 列始终
 * 按传入内容原样使用。</p>
 */
public final class SqlWrapper {

    /** 默认表别名。 */
    private static final String DEFAULT_ALIAS = "o";

    /** 可用于自动补全别名的简单列名。 */
    private static final String SIMPLE_COLUMN = "[A-Za-z_][A-Za-z0-9_$]*";

    /** count 子查询别名。 */
    private static final String COUNT_ALIAS = "jpa_plus_count";

    /** 查询表名。 */
    private final String table;

    /** 表别名。 */
    private final String alias;

    /** select 列。 */
    private final List<String> selectColumns = new ArrayList<>();

    /** 条件节点。 */
    private final List<Node> nodes = new ArrayList<>();

    /** 分组列。 */
    private final List<String> groupByColumns = new ArrayList<>();

    /** 排序项。 */
    private final List<OrderItem> orders = new ArrayList<>();

    /**
     * 创建查询构造器。
     *
     * @param table 表名
     * @param alias 表别名
     */
    private SqlWrapper(String table, String alias) {
        this.table = table;
        this.alias = alias;
    }

    /**
     * 使用默认别名创建查询构造器。
     *
     * @param table 表名，可包含 schema
     * @return 查询构造器
     */
    public static SqlWrapper create(String table) {
        return create(table, DEFAULT_ALIAS);
    }

    /**
     * 使用指定别名创建查询构造器。
     *
     * @param table 表名，可包含 schema
     * @param alias 表别名
     * @return 查询构造器
     */
    public static SqlWrapper create(String table, String alias) {
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException(
                    "Table must not be blank"
            );
        }

        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException(
                    "Alias must not be blank"
            );
        }

        return new SqlWrapper(table.trim(), alias.trim());
    }

    /**
     * 获取查询表名。
     *
     * @return 表名
     */
    public String getTable() {
        return table;
    }

    /**
     * 获取表别名。
     *
     * @return 表别名
     */
    public String getAlias() {
        return alias;
    }

    /**
     * 指定 select 列或表达式。
     *
     * <p>传入内容原样使用，可包含聚合函数和列别名；未调用时默认
     * 查询 {@code 别名.*}。</p>
     *
     * @param columns 列或表达式
     * @return 当前构造器
     */
    public SqlWrapper select(String... columns) {
        if (columns != null) {
            for (String column : columns) {
                if (column != null && !column.isBlank()) {
                    selectColumns.add(column.trim());
                }
            }
        }

        return this;
    }

    /**
     * 添加等于条件；值为 {@code null} 时等价于 {@link #isNull(String)}。
     *
     * @param column 列名
     * @param value  列值
     * @return 当前构造器
     */
    public SqlWrapper eq(String column, Object value) {
        return value == null
                ? isNull(column)
                : add(column, StandardSqlOperator.EQ, value);
    }

    /**
     * 添加不等于条件；值为 {@code null} 时等价于
     * {@link #isNotNull(String)}。
     *
     * @param column 列名
     * @param value  列值
     * @return 当前构造器
     */
    public SqlWrapper ne(String column, Object value) {
        return value == null
                ? isNotNull(column)
                : add(column, StandardSqlOperator.NE, value);
    }

    /**
     * 添加大于条件。
     *
     * @param column 列名
     * @param value  列值
     * @return 当前构造器
     */
    public SqlWrapper gt(String column, Object value) {
        return add(column, StandardSqlOperator.GT, value);
    }

    /**
     * 添加大于或等于条件。
     *
     * @param column 列名
     * @param value  列值
     * @return 当前构造器
     */
    public SqlWrapper ge(String column, Object value) {
        return add(column, StandardSqlOperator.GE, value);
    }

    /**
     * 添加小于条件。
     *
     * @param column 列名
     * @param value  列值
     * @return 当前构造器
     */
    public SqlWrapper lt(String column, Object value) {
        return add(column, StandardSqlOperator.LT, value);
    }

    /**
     * 添加小于或等于条件。
     *
     * @param column 列名
     * @param value  列值
     * @return 当前构造器
     */
    public SqlWrapper le(String column, Object value) {
        return add(column, StandardSqlOperator.LE, value);
    }

    /**
     * 添加模糊匹配条件，两侧自动补通配符。
     *
     * @param column 列名
     * @param value  匹配值
     * @return 当前构造器
     */
    public SqlWrapper like(String column, String value) {
        return add(
                column,
                StandardSqlOperator.LIKE,
                "%" + requireText(value) + "%"
        );
    }

    /**
     * 添加模糊匹配条件，仅左侧补通配符。
     *
     * @param column 列名
     * @param value  匹配值
     * @return 当前构造器
     */
    public SqlWrapper likeLeft(String column, String value) {
        return add(
                column,
                StandardSqlOperator.LIKE,
                "%" + requireText(value)
        );
    }

    /**
     * 添加模糊匹配条件，仅右侧补通配符。
     *
     * @param column 列名
     * @param value  匹配值
     * @return 当前构造器
     */
    public SqlWrapper likeRight(String column, String value) {
        return add(
                column,
                StandardSqlOperator.LIKE,
                requireText(value) + "%"
        );
    }

    /**
     * 添加不匹配条件，两侧自动补通配符。
     *
     * @param column 列名
     * @param value  匹配值
     * @return 当前构造器
     */
    public SqlWrapper notLike(String column, String value) {
        return add(
                column,
                StandardSqlOperator.NOT_LIKE,
                "%" + requireText(value) + "%"
        );
    }

    /**
     * 添加集合包含条件。
     *
     * <p>集合为空时生成恒假条件，避免产生非法 SQL。</p>
     *
     * @param column 列名
     * @param values 列值集合
     * @return 当前构造器
     */
    public SqlWrapper in(String column, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return addRaw("1 = 0");
        }

        return add(column, StandardSqlOperator.IN, values);
    }

    /**
     * 添加集合不包含条件。
     *
     * <p>集合为空时条件恒成立，直接忽略。</p>
     *
     * @param column 列名
     * @param values 列值集合
     * @return 当前构造器
     */
    public SqlWrapper notIn(String column, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }

        return add(column, StandardSqlOperator.NOT_IN, values);
    }

    /**
     * 添加闭区间条件。
     *
     * @param column 列名
     * @param start  起始值
     * @param end    结束值
     * @return 当前构造器
     */
    public SqlWrapper between(String column, Object start, Object end) {
        return add(column, StandardSqlOperator.BETWEEN, start, end);
    }

    /**
     * 添加为空条件。
     *
     * @param column 列名
     * @return 当前构造器
     */
    public SqlWrapper isNull(String column) {
        return add(column, StandardSqlOperator.IS_NULL);
    }

    /**
     * 添加不为空条件。
     *
     * @param column 列名
     * @return 当前构造器
     */
    public SqlWrapper isNotNull(String column) {
        return add(column, StandardSqlOperator.IS_NOT_NULL);
    }

    /**
     * 添加自定义运算符条件。
     *
     * @param column   列名
     * @param operator 运算符
     * @param values   运算符参数
     * @return 当前构造器
     */
    public SqlWrapper add(
            String column,
            SqlOperator operator,
            Object... values
    ) {
        if (column == null || column.isBlank()) {
            throw new IllegalArgumentException(
                    "Column must not be blank"
            );
        }

        if (operator == null) {
            throw new IllegalArgumentException(
                    "Operator must not be null"
            );
        }

        Object[] boundValues = values == null
                ? new Object[0]
                : values;

        if (boundValues.length != operator.parameterCount()) {
            throw new IllegalArgumentException(
                    "Operator expects " + operator.parameterCount()
                            + " value(s), but got "
                            + boundValues.length
            );
        }

        nodes.add(
                new ConditionNode(
                        Connector.AND,
                        operator,
                        path(column),
                        boundValues
                )
        );

        return this;
    }

    /**
     * 添加嵌套 AND 条件分组。
     *
     * @param nested 分组条件
     * @return 当前构造器
     */
    public SqlWrapper and(Consumer<SqlWrapper> nested) {
        return addGroup(Connector.AND, nested);
    }

    /**
     * 添加嵌套 OR 条件分组。
     *
     * @param nested 分组条件
     * @return 当前构造器
     */
    public SqlWrapper or(Consumer<SqlWrapper> nested) {
        return addGroup(Connector.OR, nested);
    }

    /**
     * 追加分组列。
     *
     * @param columns 列名
     * @return 当前构造器
     */
    public SqlWrapper groupBy(String... columns) {
        if (columns != null) {
            for (String column : columns) {
                if (column != null && !column.isBlank()) {
                    groupByColumns.add(column.trim());
                }
            }
        }

        return this;
    }

    /**
     * 追加升序排序。
     *
     * @param columns 列名
     * @return 当前构造器
     */
    public SqlWrapper orderByAsc(String... columns) {
        return orderBy(true, columns);
    }

    /**
     * 追加降序排序。
     *
     * @param columns 列名
     * @return 当前构造器
     */
    public SqlWrapper orderByDesc(String... columns) {
        return orderBy(false, columns);
    }

    /**
     * 追加 Spring Data 排序规则。
     *
     * @param sort 排序规则；为 {@code null} 或未排序时忽略
     * @return 当前构造器
     */
    public SqlWrapper orderBy(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return this;
        }

        sort.forEach(order -> orders.add(
                new OrderItem(
                        path(order.getProperty()),
                        order.isAscending()
                )
        ));

        return this;
    }

    /**
     * 构建不可变查询描述。
     *
     * <p>每次调用都会重新分配参数名，可重复构建。</p>
     *
     * @return 查询描述
     */
    public SqlQuery build() {
        BuildContext context = new BuildContext();
        String where = renderNodes(nodes, context);

        String select = selectColumns.isEmpty()
                ? alias + ".*"
                : String.join(", ", selectColumns);

        String baseSql = "select " + select
                + " from " + table + " " + alias;

        if (!where.isEmpty()) {
            baseSql += " where " + where;
        }

        if (!groupByColumns.isEmpty()) {
            List<String> groups = groupByColumns.stream()
                    .map(this::path)
                    .toList();

            baseSql += " group by " + String.join(", ", groups);
        }

        String countSql = "select count(*) from ("
                + baseSql + ") " + COUNT_ALIAS;

        Map<String, Object> parameters =
                Collections.unmodifiableMap(
                        new LinkedHashMap<>(context.parameters)
                );

        return new SqlQuery(
                baseSql,
                countSql,
                parameters,
                renderOrders()
        );
    }

    /**
     * 添加条件分组。
     *
     * @param connector 与前后条件的连接符
     * @param nested    分组条件
     * @return 当前构造器
     */
    private SqlWrapper addGroup(
            Connector connector,
            Consumer<SqlWrapper> nested
    ) {
        if (nested == null) {
            throw new IllegalArgumentException(
                    "Nested wrapper must not be null"
            );
        }

        SqlWrapper child = new SqlWrapper(table, alias);
        nested.accept(child);

        if (!child.nodes.isEmpty()) {
            nodes.add(new GroupNode(connector, child.nodes));
        }

        return this;
    }

    /**
     * 添加恒真或恒假条件。
     *
     * @param fragment 条件片段
     * @return 当前构造器
     */
    private SqlWrapper addRaw(String fragment) {
        nodes.add(new RawNode(Connector.AND, fragment));
        return this;
    }

    /**
     * 追加排序项。
     *
     * @param ascending 是否升序
     * @param columns   列名
     * @return 当前构造器
     */
    private SqlWrapper orderBy(boolean ascending, String... columns) {
        if (columns != null) {
            for (String column : columns) {
                if (column != null && !column.isBlank()) {
                    orders.add(new OrderItem(path(column), ascending));
                }
            }
        }

        return this;
    }

    /**
     * 将简单列名转换为带表别名的列路径。
     *
     * @param column 列名或表达式
     * @return 完整列路径
     */
    private String path(String column) {
        if (column == null || column.isBlank()) {
            throw new IllegalArgumentException(
                    "Column must not be blank"
            );
        }

        String value = column.trim();

        return value.matches(SIMPLE_COLUMN)
                ? alias + "." + value
                : value;
    }

    /**
     * 渲染排序片段。
     *
     * @return 排序片段；没有排序时返回 {@code null}
     */
    private String renderOrders() {
        if (orders.isEmpty()) {
            return null;
        }

        List<String> parts = new ArrayList<>(orders.size());

        for (OrderItem order : orders) {
            parts.add(
                    order.path()
                            + (order.ascending() ? " asc" : " desc")
            );
        }

        return String.join(", ", parts);
    }

    /**
     * 渲染条件节点列表。
     *
     * @param nodes   条件节点
     * @param context 构建上下文
     * @return 条件片段
     */
    private static String renderNodes(
            List<Node> nodes,
            BuildContext context
    ) {
        StringBuilder fragment = new StringBuilder();

        for (Node node : nodes) {
            String rendered = node.render(context);

            if (rendered.isEmpty()) {
                continue;
            }

            if (fragment.length() > 0) {
                fragment.append(' ')
                        .append(node.connector().keyword)
                        .append(' ');
            }

            fragment.append(rendered);
        }

        return fragment.toString();
    }

    /**
     * 校验模糊匹配值。
     *
     * @param value 匹配值
     * @return 原始值
     */
    private static String requireText(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Like value must not be null"
            );
        }

        return value;
    }

    /** 条件连接符。 */
    private enum Connector {
        /** 与。 */
        AND("and"),
        /** 或。 */
        OR("or");

        /** SQL 关键字。 */
        private final String keyword;

        /**
         * 创建连接符。
         *
         * @param keyword SQL 关键字
         */
        Connector(String keyword) {
            this.keyword = keyword;
        }
    }

    /** 条件树节点。 */
    private interface Node {

        /**
         * 获取与前一节点的连接符。
         *
         * @return 连接符
         */
        Connector connector();

        /**
         * 渲染节点片段。
         *
         * @param context 构建上下文
         * @return SQL 片段
         */
        String render(BuildContext context);
    }

    /**
     * 条件节点。
     *
     * @param connector 连接符
     * @param operator  运算符
     * @param path      列路径
     * @param values    参数值
     */
    private record ConditionNode(
            Connector connector,
            SqlOperator operator,
            String path,
            Object[] values
    ) implements Node {

        @Override
        public String render(BuildContext context) {
            return operator.render(path, context.bind(values));
        }
    }

    /**
     * 恒真或恒假节点。
     *
     * @param connector 连接符
     * @param fragment  条件片段
     */
    private record RawNode(
            Connector connector,
            String fragment
    ) implements Node {

        @Override
        public String render(BuildContext context) {
            return fragment;
        }
    }

    /**
     * 条件分组节点。
     *
     * @param connector 连接符
     * @param children  子节点
     */
    private record GroupNode(
            Connector connector,
            List<Node> children
    ) implements Node {

        @Override
        public String render(BuildContext context) {
            String fragment = renderNodes(children, context);

            return fragment.isEmpty()
                    ? ""
                    : "(" + fragment + ")";
        }
    }

    /**
     * 排序项。
     *
     * @param path      列路径
     * @param ascending 是否升序
     */
    private record OrderItem(String path, boolean ascending) {
    }

    /** 构建上下文，负责参数命名与收集。 */
    private static final class BuildContext {

        /** 命名参数。 */
        private final Map<String, Object> parameters =
                new LinkedHashMap<>();

        /** 参数序号。 */
        private int index;

        /**
         * 绑定参数并返回参数名。
         *
         * @param values 参数值
         * @return 参数名列表
         */
        private List<String> bind(Object[] values) {
            List<String> names = new ArrayList<>(values.length);

            for (Object value : values) {
                String name = "p" + (++index);
                parameters.put(name, value);
                names.add(name);
            }

            return names;
        }
    }
}
