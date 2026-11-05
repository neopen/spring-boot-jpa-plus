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

/**
 * 系统操作与异常日志切面。
 *
 * <p>记录控制器操作、服务异常，并按配置执行请求限流。</p>
 */
@Aspect
@Component
@Slf4j
@SuppressWarnings("all")
public class SysLogAspect {

    /** 定义服务日志注解切点。 */
    @Pointcut("@annotation(io.github.neopen.extension.annotation.SysServiceLog)")
    public void serviceAspect() {
    }

    /** 定义控制器日志注解切点。 */
    @Pointcut("@annotation(io.github.neopen.extension.annotation.SysControllerLog)")
    public void controllerAspect() {
    }

    /** 是否启用请求限流。 */
    @Value("${limiter.enable:false}")
    private boolean enableLimiter;

    /**
     * 在控制器方法执行前记录操作并检查限流规则。
     *
     * @param joinPoint 当前连接点
     * @throws IOException 请求超过限流规则或发生 I/O 错误
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
     * 在服务方法抛出异常后记录异常及请求参数。
     *
     * @param joinPoint 当前连接点
     * @param e         目标方法抛出的异常
     * @throws IOException 获取请求信息时发生 I/O 错误
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
     * 获取服务方法注解中的操作描述。
     *
     * @param joinPoint 当前连接点
     * @return 服务操作描述；未找到时返回空字符串
     * @throws Exception 目标类或方法解析失败时抛出
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
     * 获取控制器方法注解中的操作描述。
     *
     * @param joinPoint 当前连接点
     * @return 控制器操作描述；未标注时返回空字符串
     * @throws Exception 目标类或方法解析失败时抛出
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
