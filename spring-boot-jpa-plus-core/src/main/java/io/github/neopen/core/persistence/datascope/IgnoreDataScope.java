package io.github.neopen.core.persistence.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过当前方法或类执行期间的行级数据权限过滤。
 *
 * <p>标注后，方法执行期间不追加 {@link DataRule} 条件，适用于后台
 * 统计、数据修复、审批流等需要突破数据范围的场景。</p>
 *
 * <pre>{@code
 * @IgnoreDataScope
 * public long countAllOrders() {
 *     return orderRepository.count();
 * }
 * }</pre>
 *
 * <p>注解可以标注在类上，也可以标注在方法上；嵌套调用按层计数，
 * 内层结束后自动恢复外层的过滤行为。与
 * {@link io.github.neopen.core.persistence.tenant.IgnoreTenant}
 * 相互独立，可以同时使用。</p>
 *
 * @author neopen
 * @see DataScopes#isIgnored()
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreDataScope {
}
