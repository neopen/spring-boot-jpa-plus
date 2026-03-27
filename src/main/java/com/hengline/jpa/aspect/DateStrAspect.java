package com.hengline.jpa.aspect;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hengline.jpa.annotation.DateStrFormat;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.InvalidPropertiesFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <b>
 *
 * @author He.hp
 * @date 2020年9月11日 下午2:13:06
 */
@Aspect
@Component
public class DateStrAspect {

    private static final String AOP_POINTCUT_EXPRESSION = "@annotation(com.hengline.jpa.annotation.DateStrValid)";

    @Pointcut(AOP_POINTCUT_EXPRESSION)
    public void paramAspect() {
    }

    /**
     * <b> 校验日期格式
     * <p>
     *
     * @param joinPoint
     * @throws Exception
     * @author He.hp
     * @date 2020年9月11日 下午2:24:55
     */
    @Before("paramAspect()")
    public void doBefore(JoinPoint joinPoint) throws Exception {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length <= 0) {
            return;
        }

        String argStr = JSONObject.toJSONString(args);
        JSONArray objects = JSONArray.parseArray(argStr);

        // 获取方法，此处可将signature强转为MethodSignature
//        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        Method method = signature.getMethod();
//        Class<?>[] parameterTypes = method.getParameterTypes();

        for (Object arg : args) {
//            ClassLoader classLoader = parameterTypes[i].getClassLoader();
//            Class<?> aClass = classLoader.loadClass(parameterTypes[i].getName());

            Class<?> aClass = arg.getClass();
            Field[] fields = aClass.getDeclaredFields();

            for (Field field : fields) {
//                    对私有字段的访问取消权限检查。暴力访问
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                for (Object object : objects) {
                    if (ObjectUtil.isNull(object))
                        return;

                    JSONObject jsonObject = JSONObject.parseObject(object.toString());
                    if (jsonObject == null || jsonObject.isEmpty())
                        return;

                    Object param = jsonObject.get(field.getName());
                    if (param != null)
                        isAnnoType(field, param);
                }
            }
        }
    }

    /**
     * <b> 校验
     * <p>
     *
     * @param field
     * @return void
     * @Author He.hp
     * @Email hehp@citycloud.com.cn
     * @Date 2021/4/14 15:59
     */
    private void isAnnoType(Field field, Object param) throws InvalidPropertiesFormatException {
        if (field == null)
            return;

        DateStrFormat annotation = field.getAnnotation(DateStrFormat.class);
        if (annotation == null)
            return;

        // 这里判断当前注解是否为Test.class
        if (annotation.annotationType().equals(DateStrFormat.class)) {
            if (annotation.required() && param == null)
                throw new InvalidPropertiesFormatException("日期/时间不能为空");

            if (!annotation.ignore() && !isValidDate(param, annotation.value()))
                throw new InvalidPropertiesFormatException("日期/时间格式非法（" + annotation.value() + "）");
        }
    }

    /**
     * <b> 正则验证
     * <p>
     *
     * @param str
     * @param pattern
     * @return
     * @author He.hp
     * @date 2020年9月11日 下午4:18:58
     */
    private static boolean validDateStr(String str, String pattern) {
        // 正则表达式校验日期格式
        // String eL = "^\\d{4}\\d{2}\\d{2}$";// 20190505
        Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(str);
        return matcher.matches();
    }

    /**
     * <b> 校验格式
     * <p>
     *
     * @param param
     * @param pattern
     * @return
     * @author He.hp
     * @date 2020年9月11日 下午2:20:25
     */
    private static boolean isValidDate(Object param, String pattern) {
        boolean convertSuccess = true;
        if (pattern == null || pattern.isEmpty() || param == null)
            return convertSuccess;

        String str = param.toString();

        if (str.length() != pattern.length())
            return false;

        // 指定日期格式为四位年/两位月份/两位日期，注意yyyy/MM/dd区分大小写；
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            // 设置lenient为false. 否则SimpleDateFormat会比较宽松地验证日期，比如2007/02/29会被接受，并转换成2007/03/01
            format.setLenient(false);
            format.parse(str);
        } catch (ParseException e) {
            // 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
            convertSuccess = false;
        }
        return convertSuccess;
    }
}
