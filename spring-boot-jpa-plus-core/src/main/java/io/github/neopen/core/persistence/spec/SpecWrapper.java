package io.github.neopen.core.persistence.spec;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * JPA Criteria 查询构造器。
 *
 * <p>通过链式调用构造 Spring Data JPA {@link Specification}，条件默认
 * 按 AND 组合，可通过 {@link #and(Consumer)} 和 {@link #or(Consumer)}
 * 构建嵌套条件分组。</p>
 *
 * <p>字段使用实体属性路径。使用关联字段前需要先通过 {@link #join(String)}
 * 注册连表，例如：</p>
 *
 * <pre>{@code
 * SpecWrapper.create(Order.class)
 *         .join("orderProducts")
 *         .like("orderProducts.productName", "机械");
 * }</pre>
 *
 * <p>一对多连表默认开启 distinct，避免分页和 count 出现重复行。
 * fetch join 在 count 查询中会自动跳过；对集合使用 fetch join 分页时，
 * 仍受 JPA Provider 的“内存分页”限制，建议只在详情或列表查询中使用。</p>
 *
 * @param <T> 实体类型
 */
public final class SpecWrapper<T> implements Specification<T> {

    /** 根实体类型。 */
    private final Class<T> entityType;

    /** 条件节点。 */
    private final List<Node<T>> nodes = new ArrayList<>();

    /** 连表描述。 */
    private final List<JoinSpec> joins = new ArrayList<>();

    /** 是否去重；{@code null} 表示根据连表情况自动判断。 */
    private Boolean distinct;

    /**
     * 创建查询构造器。
     *
     * @param entityType 根实体类型
     */
    private SpecWrapper(Class<T> entityType) {
        this.entityType = entityType;
    }

    /**
     * 创建查询构造器。
     *
     * @param entityType 根实体类型
     * @param <T>        实体类型
     * @return 查询构造器
     */
    public static <T> SpecWrapper<T> create(Class<T> entityType) {
        if (entityType == null) {
            throw new IllegalArgumentException(
                    "Entity type must not be null"
            );
        }

        return new SpecWrapper<>(entityType);
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
    public SpecWrapper<T> eq(String field, Object value) {
        return value == null
                ? isNull(field)
                : add(field, StandardSpecOperator.EQ, value);
    }

    /**
     * 添加不等于条件；值为 {@code null} 时等价于
     * {@link #isNotNull(String)}。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public SpecWrapper<T> ne(String field, Object value) {
        return value == null
                ? isNotNull(field)
                : add(field, StandardSpecOperator.NE, value);
    }

    /**
     * 添加大于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public SpecWrapper<T> gt(String field, Object value) {
        return add(field, StandardSpecOperator.GT, value);
    }

    /**
     * 添加大于或等于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public SpecWrapper<T> ge(String field, Object value) {
        return add(field, StandardSpecOperator.GE, value);
    }

    /**
     * 添加小于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public SpecWrapper<T> lt(String field, Object value) {
        return add(field, StandardSpecOperator.LT, value);
    }

    /**
     * 添加小于或等于条件。
     *
     * @param field 字段路径
     * @param value 字段值
     * @return 当前构造器
     */
    public SpecWrapper<T> le(String field, Object value) {
        return add(field, StandardSpecOperator.LE, value);
    }

    /**
     * 添加模糊匹配条件，两侧自动补通配符。
     *
     * @param field 字段路径
     * @param value 匹配值
     * @return 当前构造器
     */
    public SpecWrapper<T> like(String field, String value) {
        return add(
                field,
                StandardSpecOperator.LIKE,
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
    public SpecWrapper<T> likeLeft(String field, String value) {
        return add(
                field,
                StandardSpecOperator.LIKE,
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
    public SpecWrapper<T> likeRight(String field, String value) {
        return add(
                field,
                StandardSpecOperator.LIKE,
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
    public SpecWrapper<T> notLike(String field, String value) {
        return add(
                field,
                StandardSpecOperator.NOT_LIKE,
                "%" + requireText(value) + "%"
        );
    }

    /**
     * 添加集合包含条件。
     *
     * <p>集合为空时生成恒假条件。</p>
     *
     * @param field  字段路径
     * @param values 字段值集合
     * @return 当前构造器
     */
    public SpecWrapper<T> in(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return raw((root, query, cb) -> cb.disjunction());
        }

        return add(field, StandardSpecOperator.IN, values);
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
    public SpecWrapper<T> notIn(String field, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }

        return add(field, StandardSpecOperator.NOT_IN, values);
    }

    /**
     * 添加闭区间条件。
     *
     * @param field 字段路径
     * @param start 起始值
     * @param end   结束值
     * @return 当前构造器
     */
    public SpecWrapper<T> between(String field, Object start, Object end) {
        return add(field, StandardSpecOperator.BETWEEN, start, end);
    }

    /**
     * 添加为空条件。
     *
     * @param field 字段路径
     * @return 当前构造器
     */
    public SpecWrapper<T> isNull(String field) {
        return add(field, StandardSpecOperator.IS_NULL);
    }

    /**
     * 添加不为空条件。
     *
     * @param field 字段路径
     * @return 当前构造器
     */
    public SpecWrapper<T> isNotNull(String field) {
        return add(field, StandardSpecOperator.IS_NOT_NULL);
    }

    /**
     * 添加自定义运算符条件。
     *
     * @param field    字段路径
     * @param operator 运算符
     * @param values   运算符参数
     * @return 当前构造器
     */
    public SpecWrapper<T> add(
            String field,
            SpecOperator operator,
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

        Object[] boundValues = values == null ? new Object[0] : values;

        if (boundValues.length != operator.parameterCount()) {
            throw new IllegalArgumentException(
                    "Operator expects " + operator.parameterCount()
                            + " value(s), but got "
                            + boundValues.length
            );
        }

        nodes.add(
                new ConditionNode<>(
                        Connector.AND,
                        operator,
                        field.trim(),
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
    public SpecWrapper<T> and(Consumer<SpecWrapper<T>> nested) {
        return addGroup(Connector.AND, nested);
    }

    /**
     * 添加嵌套 OR 条件分组。
     *
     * @param nested 分组条件
     * @return 当前构造器
     */
    public SpecWrapper<T> or(Consumer<SpecWrapper<T>> nested) {
        return addGroup(Connector.OR, nested);
    }

    /**
     * 注册内连接。
     *
     * @param attribute 关联属性
     * @return 当前构造器
     */
    public SpecWrapper<T> join(String attribute) {
        return join(attribute, attribute, JoinType.INNER);
    }

    /**
     * 注册指定类型的连表。
     *
     * @param attribute 关联属性
     * @param joinType  连接类型
     * @return 当前构造器
     */
    public SpecWrapper<T> join(String attribute, JoinType joinType) {
        return join(attribute, attribute, joinType);
    }

    /**
     * 注册带别名的内连接。
     *
     * @param attribute 关联属性
     * @param alias     别名
     * @return 当前构造器
     */
    public SpecWrapper<T> join(String attribute, String alias) {
        return join(attribute, alias, JoinType.INNER);
    }

    /**
     * 注册带别名的连表。
     *
     * @param attribute 关联属性
     * @param alias     别名，后续条件使用该别名作为路径前缀
     * @param joinType  连接类型
     * @return 当前构造器
     */
    public SpecWrapper<T> join(
            String attribute,
            String alias,
            JoinType joinType
    ) {
        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException(
                    "Join attribute must not be blank"
            );
        }

        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException(
                    "Join alias must not be blank"
            );
        }

        if (joinType == null) {
            throw new IllegalArgumentException(
                    "Join type must not be null"
            );
        }

        String normalizedAlias = alias.trim();

        for (JoinSpec spec : joins) {
            if (!spec.fetch() && spec.alias().equals(normalizedAlias)) {
                throw new IllegalArgumentException(
                        "Duplicate join alias: " + normalizedAlias
                );
            }
        }

        joins.add(
                new JoinSpec(
                        attribute.trim(),
                        normalizedAlias,
                        joinType,
                        false
                )
        );

        return this;
    }

    /**
     * 注册 fetch join，默认使用左连接。
     *
     * @param attribute 关联属性
     * @return 当前构造器
     */
    public SpecWrapper<T> fetch(String attribute) {
        return fetch(attribute, JoinType.LEFT);
    }

    /**
     * 注册指定类型的 fetch join。
     *
     * @param attribute 关联属性
     * @param joinType  连接类型
     * @return 当前构造器
     */
    public SpecWrapper<T> fetch(String attribute, JoinType joinType) {
        if (attribute == null || attribute.isBlank()) {
            throw new IllegalArgumentException(
                    "Fetch attribute must not be blank"
            );
        }

        if (joinType == null) {
            throw new IllegalArgumentException(
                    "Join type must not be null"
            );
        }

        joins.add(
                new JoinSpec(
                        attribute.trim(),
                        attribute.trim(),
                        joinType,
                        true
                )
        );

        return this;
    }

    /**
     * 强制开启查询去重。
     *
     * @return 当前构造器
     */
    public SpecWrapper<T> distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * 设置查询去重。
     *
     * @param distinct 是否去重
     * @return 当前构造器
     */
    public SpecWrapper<T> distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /**
     * 追加自定义 Specification。
     *
     * <p>适合子查询、字段间比较等链式 API 未覆盖的场景。</p>
     *
     * @param specification 自定义查询规范
     * @return 当前构造器
     */
    public SpecWrapper<T> raw(Specification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException(
                    "Specification must not be null"
            );
        }

        nodes.add(new RawNode<>(Connector.AND, specification));
        return this;
    }

    /**
     * 转换为 Spring Data JPA 查询规范。
     *
     * @return 当前对象
     */
    public Specification<T> toSpecification() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Predicate toPredicate(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        BuildContext<T> context = new BuildContext<>(root, query, cb);
        applyJoins(context);
        applyDistinct(query);

        return nodes.isEmpty()
                ? cb.conjunction()
                : renderNodes(nodes, context);
    }

    /**
     * 应用连表与 fetch join。
     *
     * @param context 构建上下文
     */
    private void applyJoins(BuildContext<T> context) {
        if (joins.isEmpty()) {
            return;
        }

        boolean countQuery = isCountQuery(context.query);

        for (JoinSpec spec : joins) {
            if (spec.fetch()) {
                if (!countQuery) {
                    context.root.fetch(spec.attribute(), spec.joinType());
                }
                continue;
            }

            Join<?, ?> join = context.root.join(
                    spec.attribute(),
                    spec.joinType()
            );
            join.alias(spec.alias());

            if (context.joins.putIfAbsent(spec.alias(), join) != null) {
                throw new IllegalArgumentException(
                        "Duplicate join alias: " + spec.alias()
                );
            }
        }
    }

    /**
     * 应用去重设置。
     *
     * @param query Criteria 查询
     */
    private void applyDistinct(CriteriaQuery<?> query) {
        if (Boolean.TRUE.equals(distinct)) {
            query.distinct(true);
            return;
        }

        if (Boolean.FALSE.equals(distinct)) {
            query.distinct(false);
            return;
        }

        boolean hasJoin = joins.stream().anyMatch(spec -> !spec.fetch());

        if (hasJoin) {
            query.distinct(true);
        }
    }

    /**
     * 添加条件分组。
     *
     * @param connector 与前后条件的连接符
     * @param nested    分组条件
     * @return 当前构造器
     */
    private SpecWrapper<T> addGroup(
            Connector connector,
            Consumer<SpecWrapper<T>> nested
    ) {
        if (nested == null) {
            throw new IllegalArgumentException(
                    "Nested wrapper must not be null"
            );
        }

        SpecWrapper<T> child = new SpecWrapper<>(entityType);
        nested.accept(child);

        if (!child.nodes.isEmpty()) {
            joins.addAll(child.joins);
            nodes.add(new GroupNode<>(connector, child.nodes));
        }

        return this;
    }

    /**
     * 渲染条件节点。
     *
     * @param nodes   条件节点
     * @param context 构建上下文
     * @return 组合后的谓词
     */
    private static <T> Predicate renderNodes(
            List<Node<T>> nodes,
            BuildContext<T> context
    ) {
        Predicate result = null;

        for (Node<T> node : nodes) {
            Predicate predicate = node.toPredicate(context);

            if (predicate == null) {
                continue;
            }

            if (result == null) {
                result = predicate;
                continue;
            }

            result = node.connector() == Connector.AND
                    ? context.cb.and(result, predicate)
                    : context.cb.or(result, predicate);
        }

        return result == null
                ? context.cb.conjunction()
                : result;
    }

    /**
     * 判断是否为 count 查询。
     *
     * @param query Criteria 查询
     * @return count 查询返回 {@code true}
     */
    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();

        return resultType == Long.class || resultType == long.class;
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
        AND,
        /** 或。 */
        OR
    }

    /** 条件树节点。 */
    private interface Node<T> {

        /**
         * 获取与前一节点的连接符。
         *
         * @return 连接符
         */
        Connector connector();

        /**
         * 构建谓词。
         *
         * @param context 构建上下文
         * @return 查询谓词
         */
        Predicate toPredicate(BuildContext<T> context);
    }

    /**
     * 条件节点。
     *
     * @param connector 连接符
     * @param operator  运算符
     * @param field     字段路径
     * @param values    参数值
     */
    private record ConditionNode<T>(
            Connector connector,
            SpecOperator operator,
            String field,
            Object[] values
    ) implements Node<T> {

        @Override
        public Predicate toPredicate(BuildContext<T> context) {
            return operator.toPredicate(
                    context.cb,
                    context.resolve(field),
                    values
            );
        }
    }

    /**
     * 自定义 Specification 节点。
     *
     * @param connector     连接符
     * @param specification 查询规范
     */
    private record RawNode<T>(
            Connector connector,
            Specification<T> specification
    ) implements Node<T> {

        @Override
        public Predicate toPredicate(BuildContext<T> context) {
            return specification.toPredicate(
                    context.root,
                    context.query,
                    context.cb
            );
        }
    }

    /**
     * 条件分组节点。
     *
     * @param connector 连接符
     * @param children  子节点
     */
    private record GroupNode<T>(
            Connector connector,
            List<Node<T>> children
    ) implements Node<T> {

        @Override
        public Predicate toPredicate(BuildContext<T> context) {
            return renderNodes(children, context);
        }
    }

    /**
     * 连表描述。
     *
     * @param attribute 关联属性
     * @param alias     别名
     * @param joinType  连接类型
     * @param fetch     是否 fetch join
     */
    private record JoinSpec(
            String attribute,
            String alias,
            JoinType joinType,
            boolean fetch
    ) {
    }

    /** 构建上下文。 */
    private static final class BuildContext<T> {

        /** 查询根。 */
        private final Root<T> root;

        /** Criteria 查询。 */
        private final CriteriaQuery<?> query;

        /** Criteria 构建器。 */
        private final CriteriaBuilder cb;

        /** 已注册的连表，按别名索引。 */
        private final Map<String, From<?, ?>> joins = new LinkedHashMap<>();

        /**
         * 创建构建上下文。
         *
         * @param root  查询根
         * @param query Criteria 查询
         * @param cb    Criteria 构建器
         */
        private BuildContext(
                Root<T> root,
                CriteriaQuery<?> query,
                CriteriaBuilder cb
        ) {
            this.root = root;
            this.query = query;
            this.cb = cb;
        }

        /**
         * 解析字段路径。
         *
         * <p>路径首段命中连表别名时从连表继续取值，否则从根实体开始。</p>
         *
         * @param field 字段路径
         * @return 字段路径
         */
        private Path<?> resolve(String field) {
            String[] segments = field.split("\\.");

            if (segments.length == 0) {
                throw new IllegalArgumentException(
                        "Field must not be blank"
                );
            }

            From<?, ?> joined = joins.get(segments[0]);

            if (joined != null && segments.length == 1) {
                return joined;
            }

            Path<?> path = joined == null ? root : joined;
            int index = joined == null ? 0 : 1;

            for (; index < segments.length; index++) {
                if (segments[index].isBlank()) {
                    throw new IllegalArgumentException(
                            "Field path must not contain blank segment: "
                                    + field
                    );
                }

                path = path.get(segments[index]);
            }

            return path;
        }
    }
}
