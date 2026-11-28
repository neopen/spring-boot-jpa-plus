/*
 * Copyright 2026 neopen (余一叶知秋尽)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.core.persistence.hql;

import io.github.neopen.core.persistence.condition.ConditionOperator;
import io.github.neopen.core.persistence.condition.QueryCondition;
import io.github.neopen.core.persistence.condition.QueryConditionEntry;
import io.github.neopen.core.persistence.condition.QueryConditionMapper;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

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
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
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

    /** 查询参数字段到 HQL 属性路径的映射。 */
    private final Map<String, String> aliases = new LinkedHashMap<>();

    /** select 表达式。 */
    private final List<String> selectColumns = new ArrayList<>();

    /** 分组表达式。 */
    private final List<String> groupByColumns = new ArrayList<>();

    /** having 条件片段；未设置时为 {@code null}。 */
    private String having;

    /** 可作为属性路径自动补全别名的简单表达式。 */
    private static final String SIMPLE_PROPERTY =
            "[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*";

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
        Map<String, List<QueryConditionEntry>> groups = new LinkedHashMap<>();

        for (QueryConditionEntry entry : QueryConditionMapper.resolve(param)) {
            if (entry.grouped()) {
                groups.computeIfAbsent(
                        entry.group(),
                        key -> new ArrayList<>()
                ).add(entry);
            } else {
                applyEntry(entry);
            }
        }

        groups.values().forEach(entries ->
                anyOf(nested -> entries.forEach(nested::applyEntry)));

        return this;
    }

    /**
     * 映射查询参数的字段名到 HQL 属性路径。
     *
     * <p>仅影响 {@link #apply(Object)} 解析出的条件。适用于查询参数
     * 字段名与实体属性名不一致的场景，例如参数使用 {@code statusCode}，
     * 而实体属性为 {@code order.status}。</p>
     *
     * @param property 查询参数字段名
     * @param target   HQL 属性路径
     * @return 当前构造器
     * @throws IllegalArgumentException 参数为空时抛出
     */
    public HqlWrapper<T> alias(String property, String target) {
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException(
                    "Property must not be blank"
            );
        }

        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException(
                    "Alias target must not be blank"
            );
        }

        aliases.put(property.trim(), target.trim());

        return this;
    }

    /**
     * 追加原生 HQL 条件片段。
     *
     * <p>片段按 AND 组合，可包含命名参数之前就已经确定的常量表达式，
     * 例如 {@code 1 = 0}。片段不做任何转义，只能传入开发者编写的
     * 静态字符串，禁止拼接用户输入。</p>
     *
     * @param fragment 条件片段
     * @return 当前构造器
     * @throws IllegalArgumentException 片段为空时抛出
     */
    public HqlWrapper<T> raw(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            throw new IllegalArgumentException(
                    "Raw fragment must not be blank"
            );
        }

        return addRaw(fragment.trim());
    }

    /**
     * 追加 {@code exists} 子查询条件。
     *
     * <p>子查询内容原样嵌入 {@code exists (...)}，只能传入开发者编写的
     * 静态 HQL，禁止拼接用户输入。</p>
     *
     * @param subQuery 子查询内容，不包含 {@code exists} 关键字
     * @return 当前构造器
     * @throws IllegalArgumentException 子查询为空时抛出
     */
    public HqlWrapper<T> exists(String subQuery) {
        return exists(subQuery, null);
    }

    /**
     * 追加带命名参数的 {@code exists} 子查询条件。
     *
     * <p>子查询中的 {@code :参数名} 会替换为框架生成的参数名并完成
     * 绑定，子查询结构仍应来自开发者编写的静态 HQL。</p>
     *
     * @param subQuery   子查询内容，不包含 {@code exists} 关键字
     * @param parameters 子查询命名参数，允许为 {@code null}
     * @return 当前构造器
     * @throws IllegalArgumentException 子查询为空时抛出
     */
    public HqlWrapper<T> exists(
            String subQuery,
            Map<String, Object> parameters
    ) {
        return addRaw(
                "exists (" + requireFragment(subQuery) + ")",
                parameters
        );
    }

    /**
     * 追加 {@code not exists} 子查询条件。
     *
     * <p>子查询内容原样嵌入 {@code not exists (...)}，只能传入开发者
     * 编写的静态 HQL，禁止拼接用户输入。</p>
     *
     * @param subQuery 子查询内容，不包含 {@code not exists} 关键字
     * @return 当前构造器
     * @throws IllegalArgumentException 子查询为空时抛出
     */
    public HqlWrapper<T> notExists(String subQuery) {
        return notExists(subQuery, null);
    }

    /**
     * 追加带命名参数的 {@code not exists} 子查询条件。
     *
     * <p>子查询中的 {@code :参数名} 会替换为框架生成的参数名并完成
     * 绑定，子查询结构仍应来自开发者编写的静态 HQL。</p>
     *
     * @param subQuery   子查询内容，不包含 {@code not exists} 关键字
     * @param parameters 子查询命名参数，允许为 {@code null}
     * @return 当前构造器
     * @throws IllegalArgumentException 子查询为空时抛出
     */
    public HqlWrapper<T> notExists(
            String subQuery,
            Map<String, Object> parameters
    ) {
        return addRaw(
                "not exists (" + requireFragment(subQuery) + ")",
                parameters
        );
    }

    /**
     * 应用注解解析出的单个条件。
     *
     * <p>字段名会先经过 {@link #alias(String, String)} 映射，再作为
     * HQL 属性路径使用。</p>
     *
     * @param entry 查询条件
     * @return 当前构造器
     */
    private HqlWrapper<T> applyEntry(QueryConditionEntry entry) {
        applyCondition(
                resolveAlias(entry.field()),
                entry.operator(),
                entry.value(),
                entry.second()
        );

        return this;
    }

    /**
     * 应用单个条件。
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
        return addGroup(Connector.AND, Connector.AND, nested);
    }

    /**
     * 添加嵌套 OR 条件分组。
     *
     * @param nested 分组条件
     * @return 当前构造器
     */
    public HqlWrapper<T> or(Consumer<HqlWrapper<T>> nested) {
        return addGroup(Connector.OR, Connector.AND, nested);
    }

    /**
     * 添加 OR 条件分组。
     *
     * <p>与 {@link #or(Consumer)} 不同，分组内部的条件也按 OR 组合，
     * 分组整体与前后的条件按 AND 组合，例如
     * {@code status = :p1 and (name like :p2 or code like :p3)}。</p>
     *
     * @param nested 分组条件
     * @return 当前构造器
     */
    public HqlWrapper<T> anyOf(Consumer<HqlWrapper<T>> nested) {
        return addGroup(Connector.AND, Connector.OR, nested);
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
     * 指定 select 表达式。
     *
     * <p>简单属性名会自动补全根实体别名，函数、聚合等表达式按原样
     * 使用。未调用时默认查询根实体。</p>
     *
     * @param expressions select 表达式
     * @return 当前构造器
     */
    public HqlWrapper<T> select(String... expressions) {
        if (expressions != null) {
            for (String expression : expressions) {
                if (expression != null && !expression.isBlank()) {
                    selectColumns.add(
                            selectExpression(expression)
                    );
                }
            }
        }

        return this;
    }

    /**
     * 追加分组表达式。
     *
     * <p>简单属性名会自动补全根实体别名。分组查询的统计语句仍然按
     * 行计数，不统计分组数量，适合报表查询而不适合分页总数统计。</p>
     *
     * @param expressions 分组表达式
     * @return 当前构造器
     */
    public HqlWrapper<T> groupBy(String... expressions) {
        if (expressions != null) {
            for (String expression : expressions) {
                if (expression != null && !expression.isBlank()) {
                    groupByColumns.add(
                            selectExpression(expression)
                    );
                }
            }
        }

        return this;
    }

    /**
     * 设置 {@code having} 条件片段。
     *
     * <p>片段原样追加到 {@code group by} 之后，只能传入开发者编写的
     * 静态 HQL，禁止拼接用户输入。重复调用会覆盖之前的值。</p>
     *
     * @param fragment having 条件片段
     * @return 当前构造器
     * @throws IllegalArgumentException 片段为空时抛出
     */
    public HqlWrapper<T> having(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            throw new IllegalArgumentException(
                    "Having fragment must not be blank"
            );
        }

        this.having = fragment.trim();

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

        String select = selectColumns.isEmpty()
                ? alias
                : String.join(", ", selectColumns);

        String baseHql = "select " + select + " " + from;
        String countHql = "select count(*) " + from;

        if (!where.isEmpty()) {
            baseHql += " where " + where;
            countHql += " where " + where;
        }

        if (!groupByColumns.isEmpty()) {
            baseHql += " group by "
                    + String.join(", ", groupByColumns);
        }

        if (having != null) {
            baseHql += " having " + having;
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
     * @param innerConnector 分组内部连接符
     * @param nested    分组条件
     * @return 当前构造器
     */
    private HqlWrapper<T> addGroup(
            Connector connector,
            Connector innerConnector,
            Consumer<HqlWrapper<T>> nested
    ) {
        if (nested == null) {
            throw new IllegalArgumentException(
                    "Nested wrapper must not be null"
            );
        }

        HqlWrapper<T> child = new HqlWrapper<>(entityType, alias);
        child.aliases.putAll(aliases);
        nested.accept(child);

        if (!child.nodes.isEmpty()) {
            nodes.add(
                    new GroupNode(connector, child.nodes, innerConnector)
            );
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
        return addRaw(fragment, null);
    }

    /**
     * 添加带命名参数的静态条件片段。
     *
     * @param fragment   条件片段
     * @param parameters 命名参数，允许为 {@code null}
     * @return 当前构造器
     */
    private HqlWrapper<T> addRaw(
            String fragment,
            Map<String, Object> parameters
    ) {
        nodes.add(
                new RawNode(
                        Connector.AND,
                        fragment,
                        parameters == null
                                ? Collections.emptyMap()
                                : new LinkedHashMap<>(parameters)
                )
        );

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
     * 将 select 或分组表达式转换为带别名的 HQL 片段。
     *
     * <p>只有简单属性路径才补全别名，函数和聚合表达式保持原样。</p>
     *
     * @param expression 表达式
     * @return HQL 片段
     */
    private String selectExpression(String expression) {
        String value = expression.trim();

        return value.matches(SIMPLE_PROPERTY)
                ? path(value)
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
        return renderNodes(nodes, context, null);
    }

    /**
     * 按指定连接符渲染条件节点。
     *
     * @param nodes     条件节点
     * @param context   构建上下文
     * @param connector 强制连接符；为 {@code null} 时使用节点自身连接符
     * @return 条件片段
     */
    private static String renderNodes(
            List<Node> nodes,
            BuildContext context,
            Connector connector
    ) {
        StringBuilder fragment = new StringBuilder();

        for (Node node : nodes) {
            String rendered = node.render(context);

            if (rendered.isEmpty()) {
                continue;
            }

            if (fragment.length() > 0) {
                Connector actual = connector == null
                        ? node.connector()
                        : connector;

                fragment.append(' ')
                        .append(actual.keyword)
                        .append(' ');
            }

            fragment.append(rendered);
        }

        return fragment.toString();
    }

    /**
     * 解析查询参数字段对应的属性路径。
     *
     * @param field 查询参数字段名
     * @return 映射后的属性路径；未配置映射时返回原值
     */
    private String resolveAlias(String field) {
        return aliases.getOrDefault(field, field);
    }

    /**
     * 校验原生子查询片段。
     *
     * @param fragment 子查询片段
     * @return 去除首尾空白后的片段
     * @throws IllegalArgumentException 片段为空时抛出
     */
    private static String requireFragment(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            throw new IllegalArgumentException(
                    "Sub query must not be blank"
            );
        }

        return fragment.trim();
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
     * @param connector  连接符
     * @param fragment   条件片段
     * @param parameters 命名参数
     */
    private record RawNode(
            Connector connector,
            String fragment,
            Map<String, Object> parameters
    ) implements Node {

        @Override
        public String render(BuildContext context) {
            String rendered = fragment;

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String name = context.bind(entry.getValue());

                rendered = rendered.replaceAll(
                        ":" + Pattern.quote(entry.getKey())
                                + "(?![A-Za-z0-9_])",
                        ":" + name
                );
            }

            return rendered;
        }
    }

    /**
     * 条件分组节点。
     *
     * @param connector 连接符
     * @param children  子节点
     * @param innerConnector 分组内部连接符
     */
    private record GroupNode(
            Connector connector,
            List<Node> children,
            Connector innerConnector
    ) implements Node {

        @Override
        public String render(BuildContext context) {
            String fragment = renderNodes(
                    children,
                    context,
                    innerConnector
            );

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
                names.add(bind(value));
            }

            return names;
        }

        /**
         * 绑定单个参数并返回参数名。
         *
         * @param value 参数值
         * @return 参数名
         */
        private String bind(Object value) {
            String name = "p" + (++index);
            parameters.put(name, value);

            return name;
        }
    }
}
