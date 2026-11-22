package io.github.neopen.core.persistence.page;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 游标分页集成测试实体。
 */
@Entity
@Table(name = "keyset_order")
public class KeysetOrder {

    /** 主键。 */
    @Id
    private Long id;

    /** 金额，用于构造排序值重复的场景。 */
    private Integer amount;

    /** 创建时间。 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** JPA 需要无参构造器。 */
    public KeysetOrder() {
    }

    /**
     * 创建测试实体。
     *
     * @param id        主键
     * @param amount    金额
     * @param createdAt 创建时间
     */
    public KeysetOrder(Long id, Integer amount, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.createdAt = createdAt;
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
     * 获取金额。
     *
     * @return 金额
     */
    public Integer getAmount() {
        return amount;
    }

    /**
     * 设置金额。
     *
     * @param amount 金额
     */
    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    /**
     * 获取创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     *
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
