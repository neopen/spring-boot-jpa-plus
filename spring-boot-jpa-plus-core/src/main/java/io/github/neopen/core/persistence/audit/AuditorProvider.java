package io.github.neopen.core.persistence.audit;

/**
 * 当前操作人提供者 SPI。
 *
 * <p>审计字段填充时通过该接口获取操作人。业务项目可以注册一个
 * Spring Bean 接入登录用户、租户或调用链上下文：</p>
 *
 * <pre>{@code
 * @Bean
 * public AuditorProvider auditorProvider() {
 *     return () -> SecurityContext.currentUsername();
 * }
 * }</pre>
 *
 * <p>未注册 Bean 时，自动配置回退到
 * {@code neopen.jpa.audit.auditor} 配置的固定值；两者都没有时
 * 不写入操作人字段。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@FunctionalInterface
public interface AuditorProvider {

    /**
     * 获取当前操作人。
     *
     * @return 操作人名称；返回 {@code null} 或空白时不写入操作人字段
     */
    String getAuditor();
}
