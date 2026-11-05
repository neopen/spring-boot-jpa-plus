package io.github.neopen.convert.aspect;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.github.neopen.convert.annotation.DateStrFormat;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.InvalidPropertiesFormatException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期字符串校验切面。
 *
 * <p>在带有日期校验注解的方法执行前，检查参数对象字段上的
 * {@link DateStrFormat} 配置。</p>
 */
@Aspect
@Component
public class DateStrAspect {

    /** 匹配带有日期校验注解的方法。 */
    private static final String AOP_POINTCUT_EXPRESSION = "@annotation(io.github.neopen.convert.annotation.DateStrValid)";

    /** 定义日期校验注解切点。 */
    @Pointcut(AOP_POINTCUT_EXPRESSION)
    public void paramAspect() {
    }

    /**
     * 在目标方法执行前校验日期参数。
     *
     * @param joinPoint 当前连接点
     * @throws Exception 参数访问或日期校验失败时抛出
     */
    @Before("paramAspect()")
    public void doBefore(JoinPoint joinPoint) throws Exception {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length <= 0) {
            return;
        }

        String argStr = JSONObject.toJSONString(args);
        JSONArray objects = JSONArray.parseArray(argStr);

        for (Object arg : args) {
            Class<?> aClass = arg.getClass();
            Field[] fields = aClass.getDeclaredFields();

            for (Field field : fields) {
                // 私有字段也需要读取注解并参与校验。
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                for (Object object : objects) {
                    if (ObjectUtils.isEmpty(object))
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
     * 按字段上的日期注解校验参数值。
     *
     * @param field 待检查字段；为 {@code null} 时直接返回
     * @param param 字段对应的参数值
     * @throws InvalidPropertiesFormatException 必填值为空或格式非法
     */
    private void isAnnoType(Field field, Object param) throws InvalidPropertiesFormatException {
        if (field == null)
            return;

        DateStrFormat annotation = field.getAnnotation(DateStrFormat.class);
        if (annotation == null)
            return;

        if (annotation.annotationType().equals(DateStrFormat.class)) {
            if (annotation.required() && param == null)
                throw new InvalidPropertiesFormatException("日期/时间不能为空");

            if (!annotation.ignore() && !isValidDate(param, annotation.value()))
                throw new InvalidPropertiesFormatException("日期/时间格式非法（" + annotation.value() + "）");
        }
    }

    /**
     * 使用正则表达式完整匹配日期字符串。
     *
     * @param str     待校验字符串
     * @param pattern 正则表达式
     * @return 完整匹配时返回 {@code true}
     */
    private static boolean validDateStr(String str, String pattern) {
        Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(str);
        return matcher.matches();
    }

    /**
     * 校验参数是否符合指定日期格式。
     *
     * <p>参数或格式为空时视为合法；非空值还会检查字符串长度。</p>
     *
     * @param param   待校验参数
     * @param pattern 日期格式
     * @return 参数符合格式时返回 {@code true}
     */
    private static boolean isValidDate(Object param, String pattern) {
        boolean convertSuccess = true;
        if (pattern == null || pattern.isEmpty() || param == null)
            return convertSuccess;

        String str = param.toString();

        if (str.length() != pattern.length())
            return false;

        // 格式字母区分大小写，例如 MM 表示月份，mm 表示分钟。
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        try {
            // 禁用宽松解析，避免无效日期被自动进位为其他日期。
            format.setLenient(false);
            format.parse(str);
        } catch (ParseException e) {
            convertSuccess = false;
        }
        return convertSuccess;
    }
}
