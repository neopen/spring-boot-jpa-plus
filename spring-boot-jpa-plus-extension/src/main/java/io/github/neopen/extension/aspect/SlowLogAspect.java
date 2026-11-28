/*
 * Copyright 2026 neopen (余一叶知秋尽)
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

import io.github.neopen.extension.annotation.SlowLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * {@link SlowLog} 切面。
 *
 * <p>记录方法耗时，达到阈值时输出 WARN 日志。方法异常退出时同样记录，
 * 并原样向外抛出异常。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Aspect
public class SlowLogAspect {

    /** 日志。 */
    private static final Logger log =
            LoggerFactory.getLogger(SlowLogAspect.class);

    /** 定义慢方法日志注解切点。 */
    @Pointcut("@annotation(io.github.neopen.extension.annotation.SlowLog)")
    public void slowLogPointcut() {
    }

    /**
     * 拦截标注注解的方法并记录耗时。
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 方法抛出的原始异常
     */
    @Around("slowLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        SlowLog slowLog = annotationOf(joinPoint);

        if (slowLog == null) {
            return joinPoint.proceed();
        }

        long start = System.nanoTime();

        try {
            return joinPoint.proceed();
        } finally {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - start
            );

            if (elapsed >= slowLog.threshold()) {
                log.warn(
                        "Slow method {} took {} ms (threshold {} ms)",
                        joinPoint.getSignature().toShortString(),
                        elapsed,
                        slowLog.threshold()
                );
            }
        }
    }

    /**
     * 获取目标方法上的注解。
     *
     * @param joinPoint 连接点
     * @return 注解；未找到时返回 {@code null}
     */
    private static SlowLog annotationOf(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature())
                .getMethod();
        SlowLog annotation = AnnotatedElementUtils
                .findMergedAnnotation(method, SlowLog.class);

        if (annotation != null || joinPoint.getTarget() == null) {
            return annotation;
        }

        Method specific = AopUtils.getMostSpecificMethod(
                method,
                joinPoint.getTarget().getClass()
        );

        return AnnotatedElementUtils.findMergedAnnotation(
                specific,
                SlowLog.class
        );
    }
}
