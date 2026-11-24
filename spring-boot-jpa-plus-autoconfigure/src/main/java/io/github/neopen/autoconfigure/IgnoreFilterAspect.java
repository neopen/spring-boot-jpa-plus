/*
 * Copyright 2026 neopen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.neopen.autoconfigure;

import io.github.neopen.core.persistence.datascope.DataScopes;
import io.github.neopen.core.persistence.datascope.IgnoreDataScope;
import io.github.neopen.core.persistence.tenant.IgnoreTenant;
import io.github.neopen.core.persistence.tenant.Tenants;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * 处理 {@link IgnoreTenant} 与 {@link IgnoreDataScope} 的切面。
 *
 * <p>方法执行期间按注解跳过对应的过滤，执行结束后恢复原有状态。
 * 两个注解相互独立，可以同时标注；嵌套调用按层计数，内层结束后
 * 继续保留外层的跳过状态。</p>
 *
 * <p>切面顺序低于数据源切面，保证在数据源已经确定后再调整过滤。</p>
 *
 * @author neopen
 * @see Tenants#ignore()
 * @see DataScopes#ignore()
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 200)
public class IgnoreFilterAspect {

    /**
     * 创建切面。
     */
    public IgnoreFilterAspect() {
    }

    /**
     * 在跳过过滤的上下文中执行方法。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(io.github.neopen.core.persistence.tenant.IgnoreTenant)"
            + " || @within(io.github.neopen.core.persistence.tenant.IgnoreTenant)"
            + " || @annotation(io.github.neopen.core.persistence.datascope"
            + ".IgnoreDataScope)"
            + " || @within(io.github.neopen.core.persistence.datascope"
            + ".IgnoreDataScope)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean ignoreTenant = hasAnnotation(joinPoint, IgnoreTenant.class);
        boolean ignoreDataScope = hasAnnotation(joinPoint, IgnoreDataScope.class);

        if (!ignoreTenant && !ignoreDataScope) {
            return joinPoint.proceed();
        }

        if (ignoreTenant) {
            Tenants.ignore();
        }

        if (ignoreDataScope) {
            DataScopes.ignore();
        }

        try {
            return joinPoint.proceed();
        } finally {
            if (ignoreDataScope) {
                DataScopes.restore();
            }

            if (ignoreTenant) {
                Tenants.restore();
            }
        }
    }

    /**
     * 判断方法或目标类是否标注了指定注解。
     *
     * <p>方法上的注解优先，其次查找目标类；返回是否命中，不使用
     * 注解属性。</p>
     *
     * @param joinPoint      连接点
     * @param annotationType 注解类型
     * @return 标注时返回 {@code true}
     */
    private boolean hasAnnotation(
            ProceedingJoinPoint joinPoint,
            Class<? extends Annotation> annotationType
    ) {
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (AnnotatedElementUtils.hasAnnotation(method, annotationType)) {
            return true;
        }

        return AnnotatedElementUtils.hasAnnotation(
                joinPoint.getTarget().getClass(),
                annotationType
        );
    }
}
