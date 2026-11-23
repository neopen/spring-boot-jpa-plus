package io.github.neopen.core.persistence.dialect;

import io.github.neopen.core.persistence.logicdelete.LogicDelete;
import io.github.neopen.core.persistence.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * 方言集成测试实体。
 *
 * <p>同时声明逻辑删除、租户、数据权限与 JSON 字段，用于验证
 * MySQL / PostgreSQL 下的过滤、批量与类型映射。</p>
 */
@Entity
@Table(name = "dialect_document")
public class DialectDocument {

    /** 主键。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务键。 */
    @Column(name = "doc_no", nullable = false, unique = true)
    private String docNo;

    /** 租户。 */
    @TenantId
    @Column(name = "tenant_id")
    private String tenantId;

    /** 部门，用于数据权限过滤。 */
    @Column(name = "dept_id")
    private Long deptId;

    /** 状态。 */
    @Column(name = "status")
    private String status;

    /** 业务负载。 */
    @Column(name = "payload")
    private String payload;

    /** 逻辑删除标记。 */
    @LogicDelete
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    /** JSON 扩展属性，PostgreSQL 映射为 jsonb。 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    private Map<String, Object> attributes;

    /** JPA 需要无参构造器。 */
    public DialectDocument() {
    }

    /**
     * 创建完整实体。
     *
     * @param docNo      业务键
     * @param tenantId   租户
     * @param deptId     部门
     * @param status     状态
     * @param payload    业务负载
     * @param deleted    是否已删除
     * @param attributes JSON 扩展属性
     */
    public DialectDocument(
            String docNo,
            String tenantId,
            Long deptId,
            String status,
            String payload,
            Boolean deleted,
            Map<String, Object> attributes
    ) {
        this.docNo = docNo;
        this.tenantId = tenantId;
        this.deptId = deptId;
        this.status = status;
        this.payload = payload;
        this.deleted = deleted;
        this.attributes = attributes;
    }

    /**
     * 获取主键。
     *
     * @return 主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 获取业务键。
     *
     * @return 业务键
     */
    public String getDocNo() {
        return docNo;
    }

    /**
     * 获取租户。
     *
     * @return 租户
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 获取部门。
     *
     * @return 部门
     */
    public Long getDeptId() {
        return deptId;
    }

    /**
     * 获取状态。
     *
     * @return 状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 获取业务负载。
     *
     * @return 业务负载
     */
    public String getPayload() {
        return payload;
    }

    /**
     * 获取逻辑删除标记。
     *
     * @return 逻辑删除标记
     */
    public Boolean getDeleted() {
        return deleted;
    }

    /**
     * 获取 JSON 扩展属性。
     *
     * @return JSON 扩展属性
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
