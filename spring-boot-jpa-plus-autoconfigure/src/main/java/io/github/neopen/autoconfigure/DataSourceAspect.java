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

import io.github.neopen.core.persistence.datasource.DataSource;
import io.github.neopen.core.persistence.datasource.DataSourceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * 处理 {@link DataSource} 注解的切面。
 *
 * <p>在方法执行期间把目标数据源绑定到当前线程，执行结束后恢复
 * 原有数据源。切面顺序设置为最高优先级，保证在事务开启前切换
 * 数据源。</p>
 *
 * @author neopen
 * @see JpaPlusRoutingDataSource
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class DataSourceAspect {

    /** 动态数据源配置。 */
    private final JpaPlusProperties.Datasource properties;

    /**
     * 创建切面。
     *
     * @param properties 动态数据源配置，不能为 {@code null}
     */
    public DataSourceAspect(JpaPlusProperties.Datasource properties) {
        if (properties == null) {
            throw new IllegalArgumentException(
                    "JpaPlusProperties.Datasource must not be null"
            );
        }

        this.properties = properties;
    }

    /**
     * 在目标数据源下执行方法。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(io.github.neopen.core.persistence.datasource.DataSource)"
            + " || @within(io.github.neopen.core.persistence.datasource.DataSource)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        DataSource annotation = resolve(joinPoint);
        String target = resolveTarget(annotation);

        if (target == null) {
            return joinPoint.proceed();
        }

        DataSourceContext.push(target);

        try {
            return joinPoint.proceed();
        } finally {
            DataSourceContext.pop();
        }
    }

    /**
     * 读取方法或类上的注解。
     *
     * @param joinPoint 连接点
     * @return 注解；未标注时返回 {@code null}
     */
    private DataSource resolve(ProceedingJoinPoint joinPoint) {
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        DataSource annotation = AnnotatedElementUtils.findMergedAnnotation(
                method,
                DataSource.class
        );

        if (annotation != null) {
            return annotation;
        }

        return AnnotatedElementUtils.findMergedAnnotation(
                joinPoint.getTarget().getClass(),
                DataSource.class
        );
    }

    /**
     * 解析目标数据源逻辑名称。
     *
     * @param annotation 注解，允许为 {@code null}
     * @return 数据源逻辑名称；无需切换时返回 {@code null}
     */
    private String resolveTarget(DataSource annotation) {
        if (annotation == null) {
            return null;
        }

        if (annotation.value() != null && !annotation.value().isBlank()) {
            return annotation.value().trim();
        }

        return annotation.readOnly()
                ? properties.getReadTarget()
                : properties.getWriteTarget();
    }
}
