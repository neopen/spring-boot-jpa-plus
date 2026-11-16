package io.github.neopen.autoconfigure;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;

import java.util.Map;

/**
 * Hibernate JDBC 批量的属性定制器。
 *
 * <p>把 {@code neopen.jpa.jdbc-batch.*} 配置写入 Hibernate 属性，
 * 启用 JDBC 批量写入并按语句类型排序，减少往返次数。</p>
 *
 * <p>排序插入与更新需要 Hibernate 把同一语句类型的操作聚合后提交，
 * 适合批量导入等场景；如果业务依赖严格的写入顺序，可以关闭对应的
 * 排序开关。</p>
 *
 * @author neopen
 * @see JpaPlusProperties.JdbcBatch
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public class JdbcBatchHibernateCustomizer
        implements HibernatePropertiesCustomizer {

    /** Hibernate 批量大小属性名。 */
    static final String BATCH_SIZE_PROPERTY = "hibernate.jdbc.batch_size";

    /** Hibernate 插入排序属性名。 */
    static final String ORDER_INSERTS_PROPERTY = "hibernate.order_inserts";

    /** Hibernate 更新排序属性名。 */
    static final String ORDER_UPDATES_PROPERTY = "hibernate.order_updates";

    /** JDBC 批量大小。 */
    private final int batchSize;

    /** 是否排序插入语句。 */
    private final boolean orderInserts;

    /** 是否排序更新语句。 */
    private final boolean orderUpdates;

    /**
     * 创建 JDBC 批量属性定制器。
     *
     * @param batchSize    批量大小，必须大于 0
     * @param orderInserts 是否排序插入语句
     * @param orderUpdates 是否排序更新语句
     * @throws IllegalArgumentException 批量大小不大于 0 时抛出
     */
    public JdbcBatchHibernateCustomizer(
            int batchSize,
            boolean orderInserts,
            boolean orderUpdates
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException(
                    "JDBC batch size must be greater than zero"
            );
        }

        this.batchSize = batchSize;
        this.orderInserts = orderInserts;
        this.orderUpdates = orderUpdates;
    }

    /**
     * 写入 Hibernate 批量属性。
     *
     * @param hibernateProperties Hibernate 属性
     */
    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(BATCH_SIZE_PROPERTY, batchSize);
        hibernateProperties.put(ORDER_INSERTS_PROPERTY, orderInserts);
        hibernateProperties.put(ORDER_UPDATES_PROPERTY, orderUpdates);
    }
}
