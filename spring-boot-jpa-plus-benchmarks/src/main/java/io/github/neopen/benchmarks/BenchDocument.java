package io.github.neopen.benchmarks;

import io.github.neopen.core.persistence.logicdelete.LogicDelete;
import io.github.neopen.core.persistence.tenant.TenantId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 基准测试实体。
 *
 * <p>同时声明逻辑删除字段、租户字段与数据权限字段，用于覆盖完整
 * 过滤链。</p>
 *
 * @author neopen
 */
@Entity
@Table(name = "bench_document")
public class BenchDocument {

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

    /**
     * JPA 需要的无参构造器。
     */
    public BenchDocument() {
    }

    /**
     * 创建完整实体。
     *
     * @param docNo    业务键
     * @param tenantId 租户
     * @param deptId   部门
     * @param status   状态
     * @param payload  业务负载
     * @param deleted  是否已删除
     */
    public BenchDocument(
            String docNo,
            String tenantId,
            Long deptId,
            String status,
            String payload,
            Boolean deleted
    ) {
        this.docNo = docNo;
        this.tenantId = tenantId;
        this.deptId = deptId;
        this.status = status;
        this.payload = payload;
        this.deleted = deleted;
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
     * 设置主键。
     *
     * @param id 主键
     */
    public void setId(Long id) {
        this.id = id;
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
     * 设置业务键。
     *
     * @param docNo 业务键
     */
    public void setDocNo(String docNo) {
        this.docNo = docNo;
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
     * 设置租户。
     *
     * @param tenantId 租户
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
     * 设置部门。
     *
     * @param deptId 部门
     */
    public void setDeptId(Long deptId) {
        this.deptId = deptId;
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
     * 设置状态。
     *
     * @param status 状态
     */
    public void setStatus(String status) {
        this.status = status;
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
     * 设置业务负载。
     *
     * @param payload 业务负载
     */
    public void setPayload(String payload) {
        this.payload = payload;
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
     * 设置逻辑删除标记。
     *
     * @param deleted 逻辑删除标记
     */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
