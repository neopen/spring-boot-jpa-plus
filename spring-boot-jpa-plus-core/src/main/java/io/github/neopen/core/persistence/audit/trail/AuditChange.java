package io.github.neopen.core.persistence.audit.trail;

/**
 * 单个字段的变更。
 *
 * <p>新增与删除时只有一侧有值：新增时 {@code before} 为
 * {@code null}，删除时 {@code after} 为 {@code null}。</p>
 *
 * @param field  字段名
 * @param before 变更前值
 * @param after  变更后值
 * @author neopen
 * @see AuditEntry
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
public record AuditChange(String field, Object before, Object after) {
}
