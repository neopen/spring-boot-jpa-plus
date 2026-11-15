package io.github.neopen.demo.config;

import io.github.neopen.core.persistence.audit.AuditorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 审计操作人配置。
 *
 * <p>演示通过 {@link AuditorProvider} 接入请求上下文：优先读取
 * {@code X-Demo-User} 请求头，非 Web 场景回退为 {@value #DEMO_USER}。
 * 该 Bean 的优先级高于 {@code neopen.jpa.audit.auditor} 配置。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Configuration(proxyBeanMethods = false)
public class AuditConfiguration {

    /** 演示用默认操作人。 */
    public static final String DEMO_USER = "demo-user";

    /** 演示用操作人请求头。 */
    private static final String USER_HEADER = "X-Demo-User";

    /**
     * 注册操作人提供者。
     *
     * @return 操作人提供者
     */
    @Bean
    public AuditorProvider auditorProvider() {
        return () -> {
            RequestAttributes attributes =
                    RequestContextHolder.getRequestAttributes();

            if (attributes instanceof ServletRequestAttributes servlet) {
                String user = servlet.getRequest()
                        .getHeader(USER_HEADER);

                if (StringUtils.hasText(user)) {
                    return user;
                }
            }

            return DEMO_USER;
        };
    }
}
