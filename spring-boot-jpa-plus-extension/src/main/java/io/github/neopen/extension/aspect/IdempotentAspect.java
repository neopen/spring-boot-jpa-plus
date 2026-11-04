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

@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    @Pointcut("@annotation(io.github.neopen.extension.annotation.Idempotent)")
    public void Idempotent() {
    }

    /**
     * <b>
     * <p>
     *
     * @param joinPoint
     * @return void
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Date 2021/4/22 11:22
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
     * <b> 获得对象
     * <p>
     *
     * @param joinPoint
     * @return com.citycloud.sanitation.common.annotation.UserAuth
     * @Author neopen
     * @GitHub https://github.com/neopen/spring-boot-jpa-plus
     * @Date 2021/4/22 11:24
     */
    public static Idempotent getUserGroupAuthParam(JoinPoint joinPoint) throws ClassNotFoundException {
        /*获取signature 该注解作用在方法上，强转为 MethodSignature*/
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        /*获取执行的方法对应Method对象*/
        Method method = signature.getMethod();

        return method.getDeclaredAnnotation(Idempotent.class);
    }


}
