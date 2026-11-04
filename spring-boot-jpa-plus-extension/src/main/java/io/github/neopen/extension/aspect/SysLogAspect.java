package io.github.neopen.extension.aspect;

import com.alibaba.fastjson2.JSONObject;
import io.github.neopen.extension.annotation.SysControllerLog;
import io.github.neopen.extension.annotation.SysServiceLog;
import io.github.neopen.extension.utils.NetworkUtil;
import io.github.neopen.extension.utils.RateLimiterUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
@SuppressWarnings("all")
public class SysLogAspect {

    @Pointcut("@annotation(io.github.neopen.jpa.annotation.SysServiceLog)")
    public void serviceAspect() {
    }

    @Pointcut("@annotation(io.github.neopen.jpa.annotation.SysControllerLog)")
    public void controllerAspect() {
    }

    @Value("${limiter.enable:false}")
    private boolean enableLimiter;

    /**
     * 前置通知 用于拦截Controller层记录用户的操作
     *
     * @throws IOException
     */
    @Before("controllerAspect()")
    public void doBefore(JoinPoint joinPoint) throws IOException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        NetworkUtil.getIpAddress(request);

        try {
            log.info("请求方法:" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName()));
            log.info("方法描述：" + getControllerMethodDescription(joinPoint));

        } catch (Exception e) {
            log.error("异常信息：{}", e.getLocalizedMessage());
        }

        if (enableLimiter) {
            Object user = request.getSession().getAttribute("_curent_user");
            if (user != null && !RateLimiterUtil.tryAcquire("test", request.getRequestURI()))
                throw new InvalidObjectException("请求频繁");
        }
    }

    /**
     * 异常通知 用于拦截service层记录异常日志
     *
     * @throws IOException
     */
    @AfterThrowing(pointcut = "serviceAspect()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e) throws IOException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();

        NetworkUtil.getIpAddress(request);
        // 获取用户请求方法的参数并序列化为JSON格式字符串
        String params = "";
        if (joinPoint.getArgs() != null && joinPoint.getArgs().length > 0) {
            for (int i = 0; i < joinPoint.getArgs().length; i++) {
                params += JSONObject.toJSONString(joinPoint.getArgs()[i]) + ";";
            }
        }
        try {
            log.info("异常代码:" + e.getClass().getName());
            log.info("异常信息:" + e.getMessage());
            log.info("异常方法:" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName() + "()"));
            log.info("方法描述:" + getServiceMethodDescription(joinPoint));
            log.info("请求参数:" + params);
        } catch (Exception ex) {
            log.error("异常信息:{}", ex.getMessage());
        }
    }

    /**
     * 获取注解中对方法的描述信息 用于service层注解
     */
    public static String getServiceMethodDescription(JoinPoint joinPoint) throws Exception {
        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] arguments = joinPoint.getArgs();
        Class targetClass = Class.forName(targetName);
        Method[] methods = targetClass.getMethods();
        String description = "";
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class[] clazzs = method.getParameterTypes();
                if (clazzs.length == arguments.length) {
                    description = method.getAnnotation(SysServiceLog.class).value();
                    break;
                }
            }
        }
        return description;
    }

    /**
     * 获取注解中对方法的描述信息 用于Controller层注解
     */
    public static String getControllerMethodDescription(JoinPoint joinPoint) throws Exception {
        String targetName = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();// 目标方法名
        Object[] arguments = joinPoint.getArgs();
        Class targetClass = Class.forName(targetName);
        Method[] methods = targetClass.getMethods();
        SysControllerLog controllerLog = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class[] clazzs = method.getParameterTypes();
                if (clazzs.length == arguments.length) {
                    controllerLog = method.getAnnotation(SysControllerLog.class);
                    break;
                }
            }
        }
        return controllerLog == null ? "" : controllerLog.value();
    }

}
