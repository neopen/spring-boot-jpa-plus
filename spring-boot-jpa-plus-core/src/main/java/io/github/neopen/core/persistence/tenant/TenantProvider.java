package io.github.neopen.core.persistence.tenant;

/**
 * 当前租户提供者。
 *
 * <p>Spring Boot 环境注册该接口的 Bean 即可，自动配置会把
 * 它接入 {@link Tenants}。</p>
 *
 * @author neopen
 * @see Tenants
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface TenantProvider {

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户；没有租户上下文时返回 {@code null}
     */
    String currentTenant();
}
