package io.github.neopen.core.persistence.audit.trail;

/**
 * 变更类型。
 *
 * @author neopen
 * @see AuditEntry
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public enum AuditAction {

    /** 新增。 */
    CREATE,

    /** 修改。 */
    UPDATE,

    /** 删除。 */
    DELETE
}
