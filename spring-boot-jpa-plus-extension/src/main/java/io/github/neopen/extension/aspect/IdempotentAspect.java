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
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.Method;

/**
 * 幂等性校验切面。
 *
 * <p>在标注 {@link Idempotent} 的方法执行前验证一次性令牌。</p>
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    /** 定义幂等性注解切点。 */
    @Pointcut("@annotation(io.github.neopen.extension.annotation.Idempotent)")
    public void Idempotent() {
    }

    /**
     * 在目标方法执行前校验请求令牌。
     *
     * @param joinPoint 当前连接点
     * @throws IOException            令牌无效或已过期
     * @throws ClassNotFoundException 解析目标方法失败
     */
    @Before("Idempotent()")
    public void UserAuthBefore(JoinPoint joinPoint) throws IOException, ClassNotFoundException {
        log.info("-----Idempotent meathod  >> " + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName()));

        Idempotent idempotent = getUserGroupAuthParam(joinPoint);
        if (idempotent == null || !idempotent.required()) {
            return;
        }

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length <= 0) {
            return;
        }

        String argStr = JSONObject.toJSONString(args);
        JSONArray objects = JSONArray.parseArray(argStr);

        String checkToken = "";
        for (Object object : objects) {
            JSONObject jsonObject = JSONObject.parseObject(object.toString());
            if (jsonObject == null || jsonObject.isEmpty())
                continue;

            if (jsonObject.containsKey("checkToken")) {
                checkToken = jsonObject.getString("checkToken");
                break;
            }
        }

        if (!IdempotentUtil.checkToken(request, checkToken)) {

            throw new InvalidObjectException("已过期，请刷新重试");
        }
    }


    /**
     * 获取连接点方法上的幂等性注解。
     *
     * @param joinPoint 当前连接点
     * @return 幂等性注解；方法未标注时返回 {@code null}
     * @throws ClassNotFoundException 解析目标方法失败
     */
    public static Idempotent getUserGroupAuthParam(JoinPoint joinPoint) throws ClassNotFoundException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        return method.getDeclaredAnnotation(Idempotent.class);
    }


}
