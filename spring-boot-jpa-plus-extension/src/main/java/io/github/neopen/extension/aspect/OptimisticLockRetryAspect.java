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
package io.github.neopen.extension.aspect;

import io.github.neopen.extension.annotation.RetryOnOptimisticLock;
import jakarta.persistence.OptimisticLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;

import java.lang.reflect.Method;

/**
 * {@link RetryOnOptimisticLock} 切面。
 *
 * <p>以最高优先级执行，包裹事务切面，使每次重试都开启独立事务。
 * 只有乐观锁异常或其嵌套原因会触发重试。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OptimisticLockRetryAspect {

    /**
     * 创建默认实例。
     */
    public OptimisticLockRetryAspect() {
    }

    /** 日志。 */
    private static final Logger log =
            LoggerFactory.getLogger(OptimisticLockRetryAspect.class);

    /** 定义乐观锁重试注解切点。 */
    @Pointcut("@annotation(io.github.neopen.extension.annotation"
            + ".RetryOnOptimisticLock)")
    public void retryPointcut() {
    }

    /**
     * 拦截标注注解的方法并在乐观锁冲突时重试。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 方法抛出的原始异常
     */
    @Around("retryPointcut()")
    public Object retry(ProceedingJoinPoint joinPoint) throws Throwable {
        RetryOnOptimisticLock retry = annotationOf(joinPoint);

        if (retry == null) {
            return joinPoint.proceed();
        }

        int maxAttempts = Math.max(1, retry.maxAttempts());
        long delay = Math.max(0L, retry.delay());

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return joinPoint.proceed();
            } catch (Throwable cause) {
                boolean retryable = isOptimisticLockFailure(cause);

                if (!retryable || attempt >= maxAttempts) {
                    throw cause;
                }

                log.debug(
                        "Optimistic lock conflict on {}, attempt {}/{}",
                        joinPoint.getSignature().toShortString(),
                        attempt,
                        maxAttempts
                );

                if (delay > 0L) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw cause;
                    }
                }
            }
        }

        throw new IllegalStateException(
                "Retry attempts exhausted unexpectedly"
        );
    }

    /**
     * 获取目标方法上的注解。
     *
     * @param joinPoint 连接点
     * @return 注解；未找到时返回 {@code null}
     */
    private static RetryOnOptimisticLock annotationOf(
            ProceedingJoinPoint joinPoint
    ) {
        Method method = ((MethodSignature) joinPoint.getSignature())
                .getMethod();
        RetryOnOptimisticLock annotation = AnnotatedElementUtils
                .findMergedAnnotation(method, RetryOnOptimisticLock.class);

        if (annotation != null || joinPoint.getTarget() == null) {
            return annotation;
        }

        Method specific = AopUtils.getMostSpecificMethod(
                method,
                joinPoint.getTarget().getClass()
        );

        return AnnotatedElementUtils.findMergedAnnotation(
                specific,
                RetryOnOptimisticLock.class
        );
    }

    /**
     * 判断异常链中是否包含乐观锁冲突。
     *
     * @param cause 异常
     * @return 包含时返回 {@code true}
     */
    private static boolean isOptimisticLockFailure(Throwable cause) {
        Throwable current = cause;

        while (current != null) {
            if (current instanceof OptimisticLockingFailureException
                    || current instanceof OptimisticLockException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
