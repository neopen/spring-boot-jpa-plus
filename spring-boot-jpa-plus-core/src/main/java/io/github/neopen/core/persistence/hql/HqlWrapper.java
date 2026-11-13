package io.github.neopen.core.persistence.hql;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import io.github.neopen.core.persistence.condition.QueryConditionMapper;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * HQL 查询构造器。
 *
 * <p>通过链式调用构造 where、order by，条件默认按 AND 组合，
 * 可通过 {@link #and(Consumer)} 和 {@link #or(Consumer)} 构建
 * 嵌套条件分组。</p>
 *
 * <p>字段名是相对根实体的 HQL 属性路径，支持 {@code order.id}
 * 这类嵌套路径，也兼容直接传入带别名的完整路径。</p>
 *
 * @param <T> 实体类型
 */
public final class HqlWrapper<T> {

    /** 默认根实体别名。 */
    private static final String DEFAULT_ALIAS = "o";

    /** 根实体类型。 */
    private final Class<T> entityType;

    /** 根实体别名。 */
    private final String alias;

    /** 条件节点。 */
    private final List<Node> nodes = new ArrayList<>();

    /** 排序项。 */
    private final List<OrderItem> orders = new ArrayList<>();

    /**
     * 创建查询构造器。
     *
     * @param entityType 根实体类型
     * @param alias      根实体别名
     */
    private HqlWrapper(Class<T> entityType, String alias) {
        this.entityType = entityType;
        this.alias = alias;
    }

    /**
     * 使用默认别名创建查询构造器。
     *
     * @param entityType 根实体类型
     * @param <T>        实体类型
     * @return 查询构造器
     */
    public static <T> HqlWrapper<T> create(Class<T> entityType) {
        return create(entityType, DEFAULT_ALIAS);
    }

    /**
     * 使用指定别名创建查询构造器。
     *
     * @param entityType 根实体类型
     * @param alias      根实体别名
     * @param <T>        实体类型
     * @return 查询构造器
     */
    public static <T> HqlWrapper<T> create(
            Class<T> entityType,
            String alias
    ) {
        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException(
                    "Alias must not be blank"
            );
        }

        return new HqlWrapper<>(entityType, alias);
    }

    /**
     * 获取根实体类型。
     *
     * @return 根实体类型
     */
    public Class<T> getEntityType() {
        return entityType;
    }

    /**
     * 添加等于条件；值为 {@code null} 时等价于 {@link #isNull(String)}。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public HqlWrapper<T> eq(String field, Object value) {
        return value == null
                ? isNull(field)
                : add(field, StandardHqlOperator.EQ, value);
    }

    /**
     * 添加不等于条件；值为 {@code null} 时等价于
     * {@link #isNotNull(String)}。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public HqlWrapper<T> ne(String field, Object value) {
        return value == null
                ? isNotNull(field)
                : add(field, StandardHqlOperator.NE, value);
    }

    /**
     * 添加大于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public HqlWrapper<T> gt(String field, Object value) {
        return add(field, StandardHqlOperator.GT, value);
    }

    /**
     * 添加大于或等于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public HqlWrapper<T> ge(String field, Object value) {
        return add(field, StandardHqlOperator.GE, value);
    }

    /**
     * 添加小于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public HqlWrapper<T> lt(String field, Object value) {
        return add(field, StandardHqlOperator.LT, value);
    }

    /**
     * 添加小于或等于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public HqlWrapper<T> le(String field, Object value) {
        return add(field, StandardHqlOperator.LE, value);
    }

    /**
     * 添加模糊匹配条件，两侧自动补通配符。
     *
     * @param field 字段路径
     * @param value 匹配值
     * @return 当前构造器
     */
    public HqlWrapper<T> like(String field, String value) {
        return add(
                field,
                StandardHqlOperator.LIKE,
                "%" + requireText(value) + "%"
        );
    }

    /**
     * 添加模糊匹配条件，仅左侧补通配符。
     *
     * @param field 字段路径
     * @param value 匹配值
     * @return 当前构造器
     */
    public HqlWrapper<T> likeLeft(String field, String value) {
        return add(
                field,
                StandardHqlOperator.LIKE,
                "%" + requireText(value)
        );
    }

    /**
     * 添加模糊匹配条件，仅右侧补通配符。
     *
     * @param field 字段路径
     * @param value 匹配值
     * @return 当前构造器
     */
    public HqlWrapper<T> likeRight(String field, String value) {
        return add(
                field,
                StandardHqlOperator.LIKE,
                requireText(value) + "%"
        );
    }

    /**
     * 添加不匹配条件，两侧自动补通配符。
     *
     * @param field 字段路径
     * @param value 匹配值
     * @return 当前构造器
     */
    public HqlWrapper<T> notLike(String field, String value) {
        return add(
                field,
                StandardHqlOperator.NOT_LIKE,
                "%" + requireText(value) + "%"
        );
    }

    /**
     * 添加集合包含条件。
     *
     * <p>集合为空时生成恒假条件，避免产生非法 HQL。</p>
     *
     * @param field  字段路径
     * @param values 字段值集合
     * @return 当前构造器
     */
    public HqlWrapper<T> in(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return addRaw("1 = 0");
        }

        return add(field, StandardHqlOperator.IN, values);
    }

    /**
     * 添加集合不包含条件。
     *
     * <p>集合为空时条件恒成立，直接忽略。</p>
     *
     * @param field  字段路径
     * @param values 字段值集合
     * @return 当前构造器
     */
    public HqlWrapper<T> notIn(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }

        return add(field, StandardHqlOperator.NOT_IN, values);
    }

    /**
     * 添加闭区间条件。
     *
     * @param field 字段路径
     * @param start 起始值
     * @param end   结束值
     * @return 当前构造器
     */
    public HqlWrapper<T> between(String field, Object start, Object end) {
        return add(field, StandardHqlOperator.BETWEEN, start, end);
    }

    /**
     * 添加为空条件。
     *
     * @param field 字段路径
     * @return 当前构造器
     */
    public HqlWrapper<T> isNull(String field) {
        return add(field, StandardHqlOperator.IS_NULL);
    }

    /**
     * 添加不为空条件。
     *
     * @param field 字段路径
     * @return 当前构造器
     */
    public HqlWrapper<T> isNotNull(String field) {
        return add(field, StandardHqlOperator.IS_NOT_NULL);
    }

    /**
     * 按查询参数上的 {@link QueryCondition} 注解自动追加条件。
     *
     * <p>空值、空白字符串、空集合和空数组会被忽略，具体规则见
     * {@link QueryConditionMapper}。字段名按 HQL 属性路径处理。</p>
     *
     * @param param 查询参数，允许为 {@code null}
     * @return 当前构造器
     */
    public HqlWrapper<T> apply(Object param) {
        QueryConditionMapper.apply(param, this::applyCondition);
        return this;
    }

    /**
     * 应用注解解析出的单个条件。
     *
     * @param field    字段路径
     * @param operator 条件运算符
     * @param value    条件值
     * @param second   区间结束值
     */
    private void applyCondition(
            String field,
            ConditionOperator operator,
            Object value,
            Object second
    ) {
        switch (operator) {
            case IS_NULL -> isNull(field);
            case IS_NOT_NULL -> isNotNull(field);
            case BETWEEN -> between(field, value, second);
            case IN -> in(field, operator.toCollection(value));
            case NOT_IN -> notIn(field, operator.toCollection(value));
            default -> add(
                    field,
                    StandardHqlOperator.of(operator),
                    operator.decorate(value)
            );
        }
    }

    /**
     * 添加自定义运算符条件。
     *
     * @param field    字段路径
     * @param operator 运算符
     * @param values   运算符参数
     * @return 当前构造器
     */
    public HqlWrapper<T> add(
            String field,
            HqlOperator operator,
            Object... values
    ) {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException(
                    "Field must not be blank"
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
                        path(field),
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
    public HqlWrapper<T> and(Consumer<HqlWrapper<T>> nested) {
        return addGroup(Connector.AND, nested);
    }

    /**
     * 添加嵌套 OR 条件分组。
     *
     * @param nested 分组条件
     * @return 当前构造器
     */
    public HqlWrapper<T> or(Consumer<HqlWrapper<T>> nested) {
        return addGroup(Connector.OR, nested);
    }

    /**
     * 追加升序排序。
     *
     * @param fields 字段路径
     * @return 当前构造器
     */
    public HqlWrapper<T> orderByAsc(String... fields) {
        return orderBy(true, fields);
    }

    /**
     * 追加降序排序。
     *
     * @param fields 字段路径
     * @return 当前构造器
     */
    public HqlWrapper<T> orderByDesc(String... fields) {
        return orderBy(false, fields);
    }

    /**
     * 追加 Spring Data 排序规则。
     *
     * @param sort 排序规则；为 {@code null} 或未排序时忽略
     * @return 当前构造器
     */
    public HqlWrapper<T> orderBy(Sort sort) {
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
    public HqlQuery build() {
        BuildContext context = new BuildContext();
        String where = renderNodes(nodes, context);

        String from = "from " + entityType.getSimpleName()
                + " " + alias;

        String baseHql = "select " + alias + " " + from;
        String countHql = "select count(*) " + from;

        if (!where.isEmpty()) {
            baseHql += " where " + where;
            countHql += " where " + where;
        }

        Map<String, Object> parameters =
                Collections.unmodifiableMap(
                        new LinkedHashMap<>(context.parameters)
                );

        return new HqlQuery(
                baseHql,
                countHql,
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
    private HqlWrapper<T> addGroup(
            Connector connector,
            Consumer<HqlWrapper<T>> nested
    ) {
        if (nested == null) {
            throw new IllegalArgumentException(
                    "Nested wrapper must not be null"
            );
        }

        HqlWrapper<T> child = new HqlWrapper<>(entityType, alias);
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
    private HqlWrapper<T> addRaw(String fragment) {
        nodes.add(new RawNode(Connector.AND, fragment));
        return this;
    }

    /**
     * 追加排序项。
     *
     * @param ascending 是否升序
     * @param fields    字段路径
     * @return 当前构造器
     */
    private HqlWrapper<T> orderBy(boolean ascending, String... fields) {
        if (fields != null) {
            for (String field : fields) {
                if (field != null && !field.isBlank()) {
                    orders.add(
                            new OrderItem(path(field), ascending)
                    );
                }
            }
        }

        return this;
    }

    /**
     * 将字段转换为带别名的 HQL 路径。
     *
     * @param field 字段路径
     * @return 完整路径
     */
    private String path(String field) {
        return field.startsWith(alias + ".")
                ? field
                : alias + "." + field;
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

        /** HQL 关键字。 */
        private final String keyword;

        /**
         * 创建连接符。
         *
         * @param keyword HQL 关键字
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
         * @return HQL 片段
         */
        String render(BuildContext context);
    }

    /**
     * 条件节点。
     *
     * @param connector 连接符
     * @param operator  运算符
     * @param path      字段路径
     * @param values    参数值
     */
    private record ConditionNode(
            Connector connector,
            HqlOperator operator,
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
     * @param path      字段路径
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
