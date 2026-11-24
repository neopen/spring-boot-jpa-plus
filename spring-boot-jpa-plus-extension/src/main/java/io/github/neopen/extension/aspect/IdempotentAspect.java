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

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.neopen.extension.annotation.Idempotent;
import io.github.neopen.extension.utils.IdempotentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 幂等性校验切面。
 *
 * <p>在标注 {@link Idempotent} 的方法执行前完成校验：</p>
 *
 * <ul>
 *     <li>配置了 {@code key} 时按业务键去重，支持 SpEL；</li>
 *     <li>未配置 {@code key} 时沿用一次性令牌校验。</li>
 * </ul>
 *
 * <p>切面由自动配置注册，业务项目无需再声明为组件。</p>
 *
 * @author neopen
 * @see <a href="https://github.com/neopen/spring-boot-jpa-plus">GitHub 源码</a>
 * @see <a href="https://jpa.helpenx.com">官网</a>
 */
@Aspect
@Slf4j
public class IdempotentAspect {

    /**
     * 创建默认实例。
     */
    public IdempotentAspect() {
    }

    /** SpEL 表达式解析器。 */
    private static final ExpressionParser PARSER =
            new SpelExpressionParser();

    /** 方法参数名发现器。 */
    private static final ParameterNameDiscoverer NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    /** 定义幂等性注解切点。 */
    @Pointcut("@annotation(io.github.neopen.extension.annotation.Idempotent)")
    public void idempotentPointcut() {
    }

    /**
     * 在目标方法执行前执行幂等校验。
     *
     * @param joinPoint 当前连接点
     * @throws ClassNotFoundException 解析目标方法失败
     */
    @Before("idempotentPointcut()")
    public void before(JoinPoint joinPoint) throws ClassNotFoundException {
        Idempotent idempotent = getUserGroupAuthParam(joinPoint);

        if (idempotent == null || !idempotent.required()) {
            return;
        }

        log.info("-----Idempotent meathod  >> "
                + joinPoint.getTarget().getClass().getName()
                + "." + joinPoint.getSignature().getName());

        if (StringUtils.hasText(idempotent.key())) {
            checkBusinessKey(joinPoint, idempotent);
            return;
        }

        checkToken(joinPoint);
    }

    /**
     * 按业务键校验重复提交。
     *
     * @param joinPoint 当前连接点
     * @param idempotent 幂等注解
     * @throws IllegalArgumentException 幂等键为空
     * @throws IllegalStateException    重复提交
     */
    private void checkBusinessKey(
            JoinPoint joinPoint,
            Idempotent idempotent
    ) {
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Object[] args = joinPoint.getArgs();
        if (args == null) {
            args = new Object[0];
        }

        Object root = joinPoint.getTarget();
        if (root == null) {
            root = method.getDeclaringClass();
        }

        EvaluationContext context = new MethodBasedEvaluationContext(
                root,
                method,
                args,
                NAME_DISCOVERER
        );

        Object value = PARSER
                .parseExpression(idempotent.key())
                .getValue(context);

        String businessKey = value == null ? null : String.valueOf(value);

        if (!StringUtils.hasText(businessKey)) {
            throw new IllegalArgumentException("幂等键为空，请检查注解配置");
        }

        String key = method.getDeclaringClass().getName()
                + "#" + method.getName()
                + ":" + businessKey;

        if (!IdempotentUtil.acquire(key, idempotent.ttl())) {
            throw new IllegalStateException("重复提交，请勿多次操作");
        }
    }

    /**
     * 校验一次性令牌。
     *
     * @param joinPoint 当前连接点
     * @throws IllegalStateException 令牌无效或已过期
     */
    private void checkToken(JoinPoint joinPoint) {
        HttpServletRequest request = currentRequest();
        String token = findToken(joinPoint.getArgs());

        if (!IdempotentUtil.checkToken(request, token)) {
            throw new IllegalStateException("已过期，请刷新重试");
        }
    }

    /**
     * 获取当前 HTTP 请求。
     *
     * @return 当前请求
     * @throws IllegalStateException 当前上下文不是 Web 请求
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException("无法获取当前请求");
        }

        return attributes.getRequest();
    }

    /**
     * 从方法参数中查找一次性令牌。
     *
     * @param args 方法参数
     * @return 令牌值；未找到时返回空字符串
     */
    private String findToken(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }

        String argStr = JSONObject.toJSONString(args);
        JSONArray objects = JSONArray.parseArray(argStr);

        for (Object object : objects) {
            JSONObject jsonObject = JSONObject.parseObject(
                    object.toString()
            );

            if (jsonObject == null || jsonObject.isEmpty()) {
                continue;
            }

            if (jsonObject.containsKey("checkToken")) {
                return jsonObject.getString("checkToken");
            }
        }

        return "";
    }

    /**
     * 获取连接点方法上的幂等性注解。
     *
     * @param joinPoint 当前连接点
     * @return 幂等性注解；方法未标注时返回 {@code null}
     * @throws ClassNotFoundException 解析目标方法失败
     */
    public static Idempotent getUserGroupAuthParam(JoinPoint joinPoint)
            throws ClassNotFoundException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        return method.getDeclaredAnnotation(Idempotent.class);
    }
}
