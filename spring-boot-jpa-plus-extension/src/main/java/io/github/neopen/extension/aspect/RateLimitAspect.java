package io.github.neopen.extension.aspect;

import com.google.common.util.concurrent.RateLimiter;
import io.github.neopen.extension.annotation.RateLimit;
import io.github.neopen.extension.exception.RateLimitException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方法级限流切面。
 *
 * <p>拦截标注 {@link RateLimit} 的方法，按解析后的业务键从本地缓存
 * 获取 {@link RateLimiter}，获取许可失败时抛出
 * {@link RateLimitException}。</p>
 *
 * <p>切面由自动配置注册，无需在业务项目中声明为组件。</p>
 */
@Aspect
public class RateLimitAspect {

    /** SpEL 表达式解析器。 */
    private static final ExpressionParser PARSER =
            new SpelExpressionParser();

    /** 方法参数名发现器。 */
    private static final ParameterNameDiscoverer NAME_DISCOVERER =
            new DefaultParameterNameDiscoverer();

    /** 按业务键缓存限流器。 */
    private final Map<String, RateLimiter> limiters =
            new ConcurrentHashMap<>();

    /**
     * 在目标方法执行前获取限流许可。
     *
     * @param joinPoint 当前连接点
     * @param rateLimit 限流注解
     * @return 目标方法返回值
     * @throws Throwable 目标方法异常或限流异常
     */
    @Around("@annotation(rateLimit)")
    public Object around(
            ProceedingJoinPoint joinPoint,
            RateLimit rateLimit
    ) throws Throwable {
        String key = resolveKey(joinPoint, rateLimit.key());
        RateLimiter limiter = limiters.computeIfAbsent(
                key,
                ignored -> createLimiter(rateLimit)
        );

        if (!limiter.tryAcquire()) {
            throw new RateLimitException(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    /**
     * 解析限流业务键。
     *
     * <p>表达式为空或结果为空白时，使用 {@code 类名#方法名}
     * 作为默认键。</p>
     *
     * @param joinPoint  当前连接点
     * @param expression SpEL 表达式
     * @return 限流业务键
     */
    private String resolveKey(
            ProceedingJoinPoint joinPoint,
            String expression
    ) {
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        String fallback = method.getDeclaringClass().getName()
                + "#" + method.getName();

        if (!StringUtils.hasText(expression)) {
            return fallback;
        }

        EvaluationContext context = new MethodBasedEvaluationContext(
                null,
                method,
                joinPoint.getArgs(),
                NAME_DISCOVERER
        );

        Expression parsed = PARSER.parseExpression(expression);
        Object value = parsed.getValue(context);
        String key = value == null ? null : String.valueOf(value);

        return StringUtils.hasText(key) ? key : fallback;
    }

    /**
     * 按注解配置创建限流器。
     *
     * @param rateLimit 限流注解
     * @return 限流器
     * @throws IllegalArgumentException 许可数量或时间窗口非法
     */
    private RateLimiter createLimiter(RateLimit rateLimit) {
        if (rateLimit.permits() <= 0) {
            throw new IllegalArgumentException(
                    "RateLimit permits must be greater than zero"
            );
        }

        long nanos = rateLimit.unit().toNanos(rateLimit.interval());

        if (nanos <= 0) {
            throw new IllegalArgumentException(
                    "RateLimit interval must be greater than zero"
            );
        }

        double permitsPerSecond =
                rateLimit.permits() * 1_000_000_000.0 / nanos;

        return RateLimiter.create(permitsPerSecond);
    }
}
